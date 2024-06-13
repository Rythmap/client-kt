package com.mvnh.rythmap

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mvnh.rythmap.databinding.FragmentAccountBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.retrofit.account.entities.AnyOtherFriendRequest
import com.mvnh.rythmap.retrofit.account.entities.SendFriendRequest
import com.mvnh.rythmap.utils.SecretData.SERVER_URL
import com.mvnh.rythmap.utils.SecretData.TAG
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)

        tokenManager = TokenManager(requireContext())
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        val nicknameSharedPref = context?.getSharedPreferences(
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
            Log.d(TAG, "Nickname: ${nicknameSharedPref?.getString("nickname", "")}")
            retrieveAndShowAccountInfo(nicknameSharedPref?.getString("nickname", "")!!)
        }

        val editProfileSheetVM: EditProfileSheetVM by activityViewModels()
        editProfileSheetVM.accountInfoUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                retrieveAndShowAccountInfo(nicknameSharedPref?.getString("nickname", "")!!)
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
            if (binding.musicPrefsLabel.visibility == View.VISIBLE) {
                bundle.putStringArrayList(
                    "musicPreferences",
                    ArrayList(binding.musicPrefsLabel.text.split(", "))
                )
            }
            if (binding.otherPrefsLabel.visibility == View.VISIBLE) {
                bundle.putStringArrayList(
                    "otherPreferences",
                    ArrayList(binding.otherPrefsLabel.text.split(", "))
                )
            }
            editProfileBottomSheet.arguments = bundle

            editProfileBottomSheet.show(parentFragmentManager, "editProfileBottomSheet")
        }

        binding.addToFriendsButton.setOnClickListener {
            val fromToken = tokenManager.getToken()
            val toNickname = binding.usernameTextView.text.toString()

            when (binding.addToFriendsButton.text) {
                getString(R.string.add) -> {
                    sendFriendRequest(fromToken!!, toNickname)
                }
                getString(R.string.actions) -> {
                    showFriendRequestOptions()
                }
                getString(R.string.pending) -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.cancel_friend_request))
                        .setMessage(getString(R.string.cancel_friend_request_message))
                        .setPositiveButton(getString(R.string.yes)) { _, _ ->
                            cancelFriendRequest(toNickname, fromToken!!)
                        }
                        .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                getString(R.string.you_re_friends) -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.remove_friend))
                        .setMessage(getString(R.string.remove_friend_message))
                        .setPositiveButton(getString(R.string.yes)) { _, _ ->
                            removeFriend(toNickname, fromToken!!)
                        }
                        .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
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

                    if (accountInfo != null && isAdded) {
                        loadProfileMedia(accountInfo)
                        binding.visibleNameTextView.text = buildVisibleName(accountInfo)
                        binding.usernameTextView.text = accountInfo.nickname

                        if (!accountInfo.about.isNullOrEmpty()) {
                            binding.descriptionTextView.text = accountInfo.about
                            binding.descriptionTextView.visibility = View.VISIBLE
                        }

                        retrieveAccountInterests(accountInfo)
                        retrieveAccountLastTracks(accountInfo)

                        val accountIdSharedPref =
                            context?.getSharedPreferences("accountId", 0)
                        val accountIdString = accountIdSharedPref?.getString("accountId", null)
                        if (accountIdString != null) {
                            checkProfileOwnership(accountInfo, accountIdString)
                        }

                        val nicknameSharedPref = context?.getSharedPreferences(
                            "nickname",
                            Context.MODE_PRIVATE
                        )
                        val nicknameString = nicknameSharedPref?.getString("nickname", null)
                        if (nicknameString != null) {
                            if (accountInfo.friends != null && accountInfo.friends.contains(nicknameString)) {
                                binding.addToFriendsButton.text = getString(R.string.you_re_friends)
                                changeAddToFriendsButtonColor()
                            } else {
                                val checkFriendRequestsCall = accountApi.getPrivateAccountInfo(tokenManager.getToken()!!)
                                checkFriendRequestsCall.enqueue(object : Callback<AccountInfoPrivate> {
                                    override fun onResponse(
                                        call: Call<AccountInfoPrivate>,
                                        response: Response<AccountInfoPrivate>
                                    ) {
                                        if (response.isSuccessful) {
                                            val accountInfo2 = response.body()
                                            if (accountInfo2?.friendRequests != null && accountInfo2.friendRequests.contains(binding.usernameTextView.text.toString())) {
                                                binding.addToFriendsButton.text = getString(R.string.actions)
                                                binding.addToFriendsButton.setOnClickListener {
                                                    showFriendRequestOptions()
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "Failed to retrieve account info: ${response.message()}")
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<AccountInfoPrivate>,
                                        t: Throwable
                                    ) {
                                        Log.e(TAG, "Failed to retrieve account info", t)
                                    }
                                })
                            }
                        }

                        if (accountInfo.friendRequests != null && accountInfo.friendRequests.contains(nicknameString)) {
                            binding.addToFriendsButton.text = getString(R.string.pending)
                            changeAddToFriendsButtonColor()
                        }

                        showAccountContent()
                    }
                } else {
                    Log.e(TAG, "Failed to retrieve account info: ${response.message()}")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_retrieve_account_info),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<AccountInfoPublic>, t: Throwable) {
                Log.e(TAG, "Failed to retrieve account info", t)
                Toast.makeText(
                    context,
                    getString(R.string.failed_to_retrieve_account_info),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadProfileMedia(accountInfo: AccountInfoPublic) {
        if (accountInfo.avatar != null) {
            Log.d(TAG, "Avatar: ${accountInfo.avatar}")
            binding.profilePfp.load("https://$SERVER_URL/account/info/media/avatar?id=${accountInfo.avatar}")
        }
        if (accountInfo.banner != null) {
            Log.d(TAG, "Banner: ${accountInfo.banner}")
            binding.profileBanner.load("https://$SERVER_URL/account/info/media/banner?id=${accountInfo.banner}")
        }
    }

    private fun buildVisibleName(accountInfo: AccountInfoPublic): String {
        var visibleNameStr = ""
        if (!accountInfo.visibleName?.name.isNullOrBlank()) {
            visibleNameStr += accountInfo.visibleName?.name

            if (!accountInfo.visibleName?.surname.isNullOrBlank()) {
                visibleNameStr += " ${accountInfo.visibleName?.surname}"
            }
        }
        if (visibleNameStr.isBlank()) {
            visibleNameStr = accountInfo.nickname
        }
        return visibleNameStr
    }

    private fun retrieveAccountInterests(accountInfo: AccountInfoPublic) {
        if (!accountInfo.friends.isNullOrEmpty()) {
            binding.friendsAmountTextView.text = "${accountInfo.friends.size} ${getString(R.string.amount_of_friends)}"
        } else {
            binding.friendsAmountTextView.visibility = View.GONE
        }
        if (accountInfo.musicPreferences != null) {
            binding.musicPrefsLabel.text = accountInfo.musicPreferences.joinToString()
        } else {
            binding.musicPrefsLabel.visibility = View.GONE
        }
        if (accountInfo.otherPreferences != null) {
            binding.otherPrefsLabel.text = accountInfo.otherPreferences.joinToString()
        } else {
            binding.otherPrefsLabel.visibility = View.GONE
        }
        if (binding.friendsAmountTextView.visibility == View.GONE &&
            binding.musicPrefsLabel.visibility == View.GONE &&
            binding.otherPrefsLabel.visibility == View.GONE
        ) {
            binding.accountInterestsLayout.visibility = View.GONE
        }
    }

    private fun retrieveAccountLastTracks(accountInfo: AccountInfoPublic) {
        if (accountInfo.lastTracks?.yandexTrack != null) {
            binding.trackNameTextView.text = accountInfo.lastTracks.yandexTrack.title
            binding.artistNameTextView.text = accountInfo.lastTracks.yandexTrack.artist
            binding.trackDurationLabel.text = "${accountInfo.lastTracks.yandexTrack.duration / 60}:${accountInfo.lastTracks.yandexTrack.duration % 60}"
            binding.trackImageView.load(accountInfo.lastTracks.yandexTrack.img)
        } else {
            binding.trackNameTextView.text = getString(R.string.no_track_listened)
            binding.artistNameTextView.text =
                getString(R.string.maybe_listen_to_some_music)
            binding.listenButton.visibility = View.GONE
        }
    }

    private fun checkProfileOwnership(accountInfo: AccountInfoPublic, accountId: String) {
        if (accountInfo.accountId == accountId) {
            binding.addToFriendsButton.visibility = View.GONE
            binding.sendMessageButton.visibility = View.GONE

            binding.editProfileButton.visibility = View.VISIBLE
        } else {
            binding.addToFriendsButton.visibility = View.VISIBLE
            binding.sendMessageButton.visibility = View.VISIBLE

            binding.editProfileButton.visibility = View.GONE
        }
    }

    private fun changeAddToFriendsButtonColor() {
        val backgroundTint = TypedValue().let { typedValue ->
            context?.theme?.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true)
            typedValue.data
        }
        binding.addToFriendsButton.backgroundTintList = ColorStateList.valueOf(backgroundTint)

        val textColor = TypedValue().let { typedValue ->
            context?.theme?.resolveAttribute(com.google.android.material.R.attr.colorSurfaceInverse, typedValue, true)
            typedValue.data
        }
        binding.addToFriendsButton.setTextColor(textColor)
    }

    private fun showAccountContent() {
        val transition = Fade()
        transition.duration = 200
        transition.addTarget(binding.accountContent)
        TransitionManager.beginDelayedTransition(binding.root, transition)

        binding.progressBar.visibility = View.GONE
        binding.accountContent.visibility = View.VISIBLE
    }

    private fun showFriendRequestOptions() {
        val popupMenu = PopupMenu(context, binding.addToFriendsButton)
        popupMenu.menu.add(getString(R.string.accept))
        popupMenu.menu.add(getString(R.string.decline))

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                getString(R.string.accept) -> {
                    acceptFriendRequest(binding.usernameTextView.text.toString(), tokenManager.getToken()!!)
                }
                getString(R.string.decline) -> {
                    declineFriendRequest(binding.usernameTextView.text.toString(), tokenManager.getToken()!!)
                }
            }
            true
        }

        popupMenu.show()
    }

    private fun sendFriendRequest(fromToken: String, toNickname: String) {
        val call = accountApi.sendFriendRequest(
            SendFriendRequest(
                fromToken,
                toNickname
            )
        )
        Log.d(TAG, "Call request: ${call.request()}")
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        context,
                        getString(R.string.friend_request_sent),
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.addToFriendsButton.text = getString(R.string.pending)
                    changeAddToFriendsButtonColor()
                } else {
                    Log.e(TAG, "Failed to send friend request: ${response.message()}")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_send_friend_request),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to send friend request", t)
                Toast.makeText(
                    context,
                    getString(R.string.failed_to_send_friend_request),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun acceptFriendRequest(toNickname: String, fromToken: String) {
        val call = accountApi.acceptFriendRequest(
            AnyOtherFriendRequest(
                toNickname,
                fromToken
            )
        )
        Log.d(TAG, "Call request: ${call.request()}")
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        context,
                        getString(R.string.friend_request_accepted),
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.addToFriendsButton.text = getString(R.string.you_re_friends)
                    changeAddToFriendsButtonColor()
                } else {
                    Log.e(TAG, "Failed to accept friend request: ${response.message()}")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_accept_friend_request),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to accept friend request", t)
                Toast.makeText(
                    context,
                    getString(R.string.failed_to_accept_friend_request),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun declineFriendRequest(toNickname: String, fromToken: String) {
        val call = accountApi.declineFriendRequest(
            AnyOtherFriendRequest(
                toNickname,
                fromToken
            )
        )
        Log.d(TAG, "Call request: ${call.request()}")
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        context,
                        getString(R.string.friend_request_declined),
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.addToFriendsButton.text = getString(R.string.add)
                    changeAddToFriendsButtonColor()
                } else {
                    Log.e(TAG, "Failed to decline friend request: ${response.message()}")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_decline_friend_request),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to decline friend request", t)
                Toast.makeText(
                    context,
                    getString(R.string.failed_to_decline_friend_request),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun cancelFriendRequest(toNickname: String, fromToken: String) {
        val call = accountApi.cancelFriendRequest(
            AnyOtherFriendRequest(
                toNickname,
                fromToken
            )
        )
        Log.d(TAG, "Call request: ${call.request()}")
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        context,
                        getString(R.string.friend_request_cancelled),
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.addToFriendsButton.text = getString(R.string.add)
                    changeAddToFriendsButtonColor()
                } else {
                    Log.e(TAG, "Failed to cancel friend request: ${response.message()}")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_cancel_friend_request),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to cancel friend request", t)
                Toast.makeText(
                    context,
                    getString(R.string.failed_to_cancel_friend_request),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun removeFriend(toNickname: String, fromToken: String) {
        val call = accountApi.removeFriend(
            AnyOtherFriendRequest(
                toNickname,
                fromToken
            )
        )
        Log.d(TAG, "Call request: ${call.request()}")
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        context,
                        getString(R.string.friend_removed),
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.addToFriendsButton.text = getString(R.string.add)
                    changeAddToFriendsButtonColor()
                } else {
                    Log.e(TAG, "Failed to remove friend: ${response.message()}")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_remove_friend),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to remove friend", t)
                Toast.makeText(
                    context,
                    getString(R.string.failed_to_remove_friend),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}