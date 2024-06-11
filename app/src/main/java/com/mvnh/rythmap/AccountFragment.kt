package com.mvnh.rythmap

import android.content.Context
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
import com.mvnh.rythmap.databinding.FragmentAccountBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.utils.SecretData.SERVER_URL
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.utils.TokenManager
import com.mvnh.rythmap.vm.EditProfileSheetVM
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

        val nicknameSharedPref = requireContext().getSharedPreferences(
            "nickname",
            Context.MODE_PRIVATE
        )

        if (arguments != null) {
            binding.editProfileButton.visibility = View.GONE
            binding.accountContent.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE

            val nickname = arguments?.getString("nickname")
            retrieveAndShowAccountInfo(nickname!!)
        } else {
            Log.d(TAG, "Nickname: ${nicknameSharedPref.getString("nickname", "")}")
            retrieveAndShowAccountInfo(nicknameSharedPref.getString("nickname", "")!!)
        }

        val editProfileSheetVM: EditProfileSheetVM by activityViewModels()
        editProfileSheetVM.accountInfoUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                retrieveAndShowAccountInfo(nicknameSharedPref.getString("nickname", "")!!)
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

    private fun retrieveAndShowAccountInfo(nickname: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.accountContent.visibility = View.GONE

        val call = accountApi.getPublicAccountInfo(nickname)
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

                        if (accountInfo?.lastTracks != null) {
                            val yandexLastTrack = accountInfo.lastTracks.yandexTrack

                            if (yandexLastTrack != null) {
                                binding.trackNameTextView.text = yandexLastTrack.title
                                binding.artistNameTextView.text = yandexLastTrack.artist
                                binding.trackImageView.load(yandexLastTrack.img)
                            }
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

                        if (accountInfo?.avatar != null) {
                            Log.d(TAG, "Avatar: ${accountInfo.avatar}")
                            binding.profilePfp.load("https://$SERVER_URL/account/info/media/avatar?id=${accountInfo.avatar}")
                        }
                        if (accountInfo?.banner != null) {
                            Log.d(TAG, "Banner: ${accountInfo.banner}")
                            binding.profileBanner.load("https://$SERVER_URL/account/info/media/banner?id=${accountInfo.banner}")
                        }

                        val transition = Fade()
                        transition.duration = 200
                        transition.addTarget(binding.accountContent)
                        TransitionManager.beginDelayedTransition(binding.root, transition)

                        binding.progressBar.visibility = View.GONE
                        binding.accountContent.visibility = View.VISIBLE

                        retrieveMediaCallsCompleted = 0
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
    }
}