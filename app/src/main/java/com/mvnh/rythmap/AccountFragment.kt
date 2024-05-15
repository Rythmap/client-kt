package com.mvnh.rythmap

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.mvnh.rythmap.SecretData.TAG
import com.mvnh.rythmap.databinding.FragmentAccountBinding
import com.mvnh.rythmap.responses.ServiceGenerator
import com.mvnh.rythmap.responses.account.AccountApi
import com.mvnh.rythmap.responses.account.entities.AccountInfoPrivate
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.apache.commons.io.IOUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi

    private var retrieveMediaCallsCompleted = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)

        tokenManager = TokenManager(requireContext())
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        retrieveAndShowAccountInfo(tokenManager.getToken())

        val pickPfpLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    uploadMedia(tokenManager.getToken(), "avatar", uri)
                } else {
                    Log.e(TAG, "Failed to pick media")
                    Toast.makeText(requireContext(), "Failed to pick media", Toast.LENGTH_SHORT).show()
                }
            }
        val pickBannerLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    uploadMedia(tokenManager.getToken(), "banner", uri)
                } else {
                    Log.e(TAG, "Failed to pick media")
                    Toast.makeText(requireContext(), "Failed to pick media", Toast.LENGTH_SHORT).show()
                }
            }

        binding.profilePfp.setOnClickListener {
            pickPfpLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.SingleMimeType("image/*")
                )
            )
        }
        binding.profileBanner.setOnClickListener {
            pickBannerLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.SingleMimeType("image/*")
                )
            )
        }

        return binding.root
    }

    private fun retrieveAndShowAccountInfo(token: String?) {
        token ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.accountContent.visibility = View.GONE

        val call = accountApi.getPrivateAccountInfo(token)
        call.enqueue(object : Callback<AccountInfoPrivate> {
            override fun onResponse(call: Call<AccountInfoPrivate>, response: Response<AccountInfoPrivate>) {
                if (response.isSuccessful) {
                    val accountInfo = response.body()
                    Log.d(TAG, "Account info: $accountInfo")

                    var visibleName = ""
                    if (accountInfo?.visibleName != null) {
                        if (accountInfo.visibleName.name != null) {
                            visibleName = accountInfo.visibleName.name
                        }
                        if (accountInfo.visibleName.surname != null) {
                            visibleName += " " + accountInfo.visibleName.surname
                        }
                    }
                    if (visibleName.isBlank()) {
                        binding.visibleNameTextView.text = accountInfo?.nickname
                    } else {
                        binding.visibleNameTextView.text = visibleName
                    }
                    binding.usernameTextView.text = accountInfo?.nickname
                    if (accountInfo?.about?.isNotBlank() == true && accountInfo.about != "null") {
                        binding.descriptionTextView.text = accountInfo.about
                        binding.descriptionTextView.visibility = View.VISIBLE
                    }

                    val accountIdSharedPref = requireContext().getSharedPreferences("accountId", 0)
                    if (accountInfo?.accountId == accountIdSharedPref.getString("accountId", null)) {
                        binding.addToFriendsButton.visibility = View.GONE
                        binding.sendMessageButton.visibility = View.GONE
                        binding.editProfileButton.visibility = View.VISIBLE
                    }

                    retrieveMedia(accountInfo?.nickname ?: "", "avatar")
                    retrieveMedia(accountInfo?.nickname ?: "", "banner")
                } else {
                    Log.e(TAG, "Failed to retrieve account info: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to retrieve account info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AccountInfoPrivate>, t: Throwable) {
                Log.e(TAG, "Failed to retrieve account info", t)
                Toast.makeText(requireContext(), "Failed to retrieve account info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun retrieveMedia(nickname: String, type: String) {
        if (nickname.isEmpty() || type.isEmpty()) {
            return
        }

        val call = accountApi.getMedia(nickname, type)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    try {
                        val mediaBytes = response.body()?.bytes()
                        val bitmap = BitmapFactory.decodeByteArray(mediaBytes, 0, mediaBytes?.size ?: 0)
                        if (type == "avatar") {
                            binding.profilePfp.setImageBitmap(bitmap)
                        } else {
                            binding.profileBanner.setImageBitmap(bitmap)
                        }

                        retrieveMediaCallsCompleted++
                        if (retrieveMediaCallsCompleted == 2) {
                            val transition = Fade()
                            transition.duration = 200
                            transition.addTarget(binding.accountContent)
                            TransitionManager.beginDelayedTransition(binding.root, transition)

                            binding.progressBar.visibility = View.GONE
                            binding.accountContent.visibility = View.VISIBLE

                            retrieveMediaCallsCompleted = 0
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to decode media", e)
                        Toast.makeText(requireContext(), "Failed to decode media", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Failed to retrieve media: ${response.errorBody()?.string()}")

                    retrieveMediaCallsCompleted++
                    if (retrieveMediaCallsCompleted == 2) {
                        val transition = Fade()
                        transition.duration = 200
                        transition.addTarget(binding.accountContent)
                        TransitionManager.beginDelayedTransition(binding.root, transition)

                        binding.progressBar.visibility = View.GONE
                        binding.accountContent.visibility = View.VISIBLE
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to retrieve media", t)

                retrieveMediaCallsCompleted++
                if (retrieveMediaCallsCompleted == 2) {
                    val transition = Fade()
                    transition.duration = 200
                    transition.addTarget(binding.accountContent)
                    TransitionManager.beginDelayedTransition(binding.root, transition)

                    binding.progressBar.visibility = View.GONE
                    binding.accountContent.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun uploadMedia(token: String?, type: String, uri: Uri) {
        token ?: return

        val uriInputStream = requireContext().contentResolver.openInputStream(uri)
        val uriBytes = IOUtils.toByteArray(uriInputStream)
        val requestBody = uriBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, uriBytes.size)
        val filePart = MultipartBody.Part.createFormData("file", "file", requestBody)

        val call = accountApi.updateMedia(token, type, filePart)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "${type.uppercase()} uploaded successfully")
                    retrieveAndShowAccountInfo(token)
                } else {
                    Log.e(TAG, "Failed to upload media: ${response.errorBody()?.string()}")
                    retrieveAndShowAccountInfo(token)
                    Toast.makeText(requireContext(), "Failed to upload $type", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to upload ${type.uppercase()}", t)
                retrieveAndShowAccountInfo(token)
                Toast.makeText(requireContext(), "Failed to upload $type", Toast.LENGTH_SHORT).show()
            }
        })
    }
}