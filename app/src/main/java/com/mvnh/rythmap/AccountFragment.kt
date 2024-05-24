package com.mvnh.rythmap

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import com.google.gson.Gson
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.databinding.FragmentAccountBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.retrofit.yandex.YandexApi
import com.mvnh.rythmap.retrofit.yandex.entities.YandexTrack
import com.mvnh.rythmap.utils.TokenManager
import com.mvnh.rythmap.vm.EditProfileSheetVM
import okhttp3.ResponseBody
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

        if (arguments != null) {
            binding.editProfileButton.visibility = View.GONE
            binding.accountContent.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE

            val nickname = arguments?.getString("nickname")
            val call = accountApi.getPublicAccountInfo(nickname!!)
            call.enqueue(object : Callback<AccountInfoPublic> {
                override fun onResponse(
                    call: Call<AccountInfoPublic>,
                    response: Response<AccountInfoPublic>
                ) {
                    if (response.isSuccessful) {
                        val accountInfo = response.body()
                        Log.d(TAG, "Account info: $accountInfo")

                        if (isAdded && activity != null) {
                            var visibleName = ""
                            if (!accountInfo?.visibleName?.name.isNullOrBlank()) {
                                visibleName += accountInfo?.visibleName?.name

                                if (!accountInfo?.visibleName?.surname.isNullOrBlank()) {
                                    visibleName += " ${accountInfo?.visibleName?.surname}"
                                }
                            }
                            if (visibleName.isBlank()) {
                                binding.visibleNameTextView.text = accountInfo?.nickname
                            } else {
                                binding.visibleNameTextView.text = visibleName
                            }
                            binding.usernameTextView.text = accountInfo?.nickname
                            if (accountInfo?.about != null && accountInfo.about.isNotEmpty()) {
                                binding.descriptionTextView.text = accountInfo.about
                                binding.descriptionTextView.visibility = View.VISIBLE
                            }

                            retrieveMedia(accountInfo?.nickname ?: "", "avatar")
                            retrieveMedia(accountInfo?.nickname ?: "", "banner")
                        }
                    } else {
                        Log.e(TAG, "Failed to retrieve account info: ${response.errorBody()?.string()}")
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.failed_to_retrieve_account_info),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<AccountInfoPublic>, t: Throwable) {
                    Log.e(TAG, "Failed to retrieve account info", t)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_to_retrieve_account_info),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            retrieveAndShowAccountInfo(tokenManager.getToken())
        }

        val editProfileSheetVM: EditProfileSheetVM by activityViewModels()
        editProfileSheetVM.accountInfoUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                retrieveAndShowAccountInfo(tokenManager.getToken())
                editProfileSheetVM.accountInfoUpdated.value = false
            }
        }

        binding.editProfileButton.setOnClickListener {
            val editProfileBottomSheet = EditProfileBottomSheet()

            val bundle = Bundle()
            bundle.putString("visibleName", binding.visibleNameTextView.text.toString())
            bundle.putString("username", binding.usernameTextView.text.toString())
            if (binding.descriptionTextView.visibility == View.VISIBLE) {
                bundle.putString("description", binding.descriptionTextView.text.toString())
            }
            if (binding.profilePfp.drawable != null) {
                val bitmapWidth = if (binding.profilePfp.width > 0) binding.profilePfp.width else 1
                val bitmapHeight =
                    if (binding.profilePfp.height > 0) binding.profilePfp.height else 1
                bundle.putParcelable(
                    "avatar",
                    binding.profilePfp.drawable.toBitmap(bitmapWidth, bitmapHeight)
                )
            }
            if (binding.profileBanner.drawable != null) {
                val bitmapWidth =
                    if (binding.profileBanner.width > 0) binding.profileBanner.width else 1
                val bitmapHeight =
                    if (binding.profileBanner.height > 0) binding.profileBanner.height else 1
                bundle.putParcelable(
                    "banner",
                    binding.profileBanner.drawable.toBitmap(bitmapWidth, bitmapHeight)
                )
            }
            editProfileBottomSheet.arguments = bundle

            editProfileBottomSheet.show(parentFragmentManager, "editProfileBottomSheet")
        }

        return binding.root
    }

    private fun retrieveAndShowAccountInfo(token: String?) {
        if (token == null) {
            Log.e(TAG, "Token is null")
            tokenManager.clearToken()
            Toast.makeText(
                requireContext(),
                getString(R.string.failed_to_retrieve_account_info),
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.accountContent.visibility = View.GONE

        val call = accountApi.getPrivateAccountInfo(token)
        call.enqueue(object : Callback<AccountInfoPrivate> {
            override fun onResponse(
                call: Call<AccountInfoPrivate>,
                response: Response<AccountInfoPrivate>
            ) {
                if (response.isSuccessful) {
                    val accountInfo = response.body()
                    Log.d(TAG, "Account info: $accountInfo")

                    if (isAdded && activity != null) {
                        var visibleName = ""
                        if (!accountInfo?.visibleName?.name.isNullOrBlank()) {
                            visibleName += accountInfo?.visibleName?.name

                            if (!accountInfo?.visibleName?.surname.isNullOrBlank()) {
                                visibleName += " ${accountInfo?.visibleName?.surname}"
                            }
                        }
                        if (visibleName.isBlank()) {
                            binding.visibleNameTextView.text = accountInfo?.nickname
                        } else {
                            binding.visibleNameTextView.text = visibleName
                        }
                        binding.usernameTextView.text = accountInfo?.nickname
                        if (accountInfo?.about != null && accountInfo.about.isNotEmpty()) {
                            binding.descriptionTextView.text = accountInfo.about
                            binding.descriptionTextView.visibility = View.VISIBLE
                        }

                        val accountIdSharedPref =
                            requireContext().getSharedPreferences("accountId", 0)
                        if (accountInfo?.accountId == accountIdSharedPref.getString(
                                "accountId",
                                null
                            )
                        ) {
                            binding.addToFriendsButton.visibility = View.GONE
                            binding.sendMessageButton.visibility = View.GONE

                            binding.editProfileButton.visibility = View.VISIBLE
                        } else {
                            binding.addToFriendsButton.visibility = View.VISIBLE
                            binding.sendMessageButton.visibility = View.VISIBLE

                            binding.editProfileButton.visibility = View.GONE
                        }

                        retrieveMedia(accountInfo?.nickname ?: "", "avatar")
                        retrieveMedia(accountInfo?.nickname ?: "", "banner")

                        val yandexTokenSharedPref = requireContext().getSharedPreferences("yandexToken", Context.MODE_PRIVATE)
                        if (yandexTokenSharedPref.getString("yandexToken", null) != null) {
                            Log.d(TAG, "Retrieving last track")
                            retrieveLastTrack(tokenManager.getToken()!!, yandexTokenSharedPref.getString("yandexToken", null)!!)
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to retrieve account info: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_to_retrieve_account_info),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<AccountInfoPrivate>, t: Throwable) {
                Log.e(TAG, "Failed to retrieve account info", t)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.failed_to_retrieve_account_info),
                    Toast.LENGTH_SHORT
                ).show()
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
                        val bitmap =
                            BitmapFactory.decodeByteArray(mediaBytes, 0, mediaBytes?.size ?: 0)
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
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.failed_to_decode_media),
                            Toast.LENGTH_SHORT
                        ).show()
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

                        retrieveMediaCallsCompleted = 0
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

                    retrieveMediaCallsCompleted = 0
                }
            }
        })
    }

    private fun retrieveLastTrack(rythmapToken: String, yandexToken: String) {
        val yandexApi = ServiceGenerator.createService(YandexApi::class.java)
        val call = yandexApi.getAndSaveCurrent(rythmapToken, yandexToken)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val getTrackInfo = yandexApi.getTrackInfo(response.body()?.string()!!)
                    getTrackInfo.enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (response.isSuccessful) {
                                val trackInfo = Gson().fromJson(
                                    response.body()?.string(),
                                    YandexTrack::class.java
                                )
                                Log.d(TAG, "Current track: $trackInfo")

                                if (isAdded && activity != null) {
                                    binding.trackNameTextView.text = trackInfo.title
                                    binding.artistNameTextView.text = trackInfo.artists.joinToString(", ") { it.name }
                                    Log.d(TAG, "Track image: ${trackInfo.image.replace("%%", "1000x1000")}")
                                    binding.trackImageView.load(trackInfo.image.replace("%%", "1000x1000"))
                                }
                            } else {
                                Log.e(TAG, "Failed to retrieve current track info: ${response.message()}")
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.e(TAG, "Failed to retrieve current track info", t)
                        }
                    })
                } else {
                    Log.e(TAG, "Failed to retrieve and save current track: ${response.raw()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to retrieve and save current track", t)
            }
        })
    }
}