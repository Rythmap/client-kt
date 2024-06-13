package com.mvnh.rythmap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mvnh.rythmap.databinding.LastTrackBottomSheetBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.retrofit.yandex.YandexApi
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LastTrackBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: LastTrackBottomSheetBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi
    private lateinit var yandexApi: YandexApi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LastTrackBottomSheetBinding.inflate(inflater, container, false)

        tokenManager = TokenManager(requireContext())
        accountApi = ServiceGenerator.createService(AccountApi::class.java)
        yandexApi = ServiceGenerator.createService(YandexApi::class.java)

        binding.profileButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("nickname", binding.nicknameTextView.text.toString())

            val fragment = AccountFragment()
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, fragment).commit()
            dialog?.dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lastTrackContent.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        binding.nicknameTextView.text = arguments?.getString("nickname")
        val call = accountApi.getPublicAccountInfo(arguments?.getString("nickname")!!)
        call.enqueue(object : Callback<AccountInfoPublic> {
            override fun onResponse(call: Call<AccountInfoPublic>, response: Response<AccountInfoPublic>) {
                if (response.isSuccessful) {
                    val accountInfo = response.body()

                    if (accountInfo?.lastTracks?.yandexTrack != null) {
                        binding.nicknameTextView.text = accountInfo.nickname
                        binding.trackNameTextView.text = accountInfo.lastTracks.yandexTrack.title
                        binding.artistNameTextView.text = accountInfo.lastTracks.yandexTrack.artist
                        binding.trackDurationLabel.text =
                            "${accountInfo.lastTracks.yandexTrack.duration / 60}:${accountInfo.lastTracks.yandexTrack.duration % 60}"
                        binding.trackImageView.load(accountInfo.lastTracks.yandexTrack.img)
                    } else {
                        binding.trackNameTextView.text = resources.getString(R.string.no_track_listened)
                        binding.artistNameTextView.text = resources.getString(R.string.maybe_listen_to_some_music)
                        binding.trackDurationLabel.text = "0:0"
                        binding.listenButton.visibility = View.GONE
                    }

                    binding.lastTrackContent.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                } else {
                    Log.e(TAG, "Failed to get account info: ${response.message()}")
                    Toast.makeText(context,
                        getString(R.string.failed_to_get_account_info), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AccountInfoPublic>, t: Throwable) {
                Log.e(TAG, "Failed to get account info: ${t.message}")
                Toast.makeText(context,
                    getString(R.string.failed_to_get_account_info), Toast.LENGTH_SHORT).show()
            }
        })
    }
}