package com.mvnh.rythmap

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mvnh.rythmap.SecretData.TAG
import com.mvnh.rythmap.databinding.FragmentFriendsBinding
import com.mvnh.rythmap.responses.ServiceGenerator
import com.mvnh.rythmap.responses.account.AccountApi
import com.mvnh.rythmap.responses.account.entities.AccountInfoBasic
import com.mvnh.rythmap.responses.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.responses.account.entities.AccountInfoPublic
import com.mvnh.rythmap.responses.account.entities.AccountVisibleName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FriendsFragment : Fragment() {

    private lateinit var binding: FragmentFriendsBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFriendsBinding.inflate(inflater, container, false)

        tokenManager = TokenManager(requireContext())
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        val recyclerView = binding.friendsRecyclerView
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        recyclerView.adapter =
            FriendsRecyclerAdapter(mutableListOf())
        retrieveAndShowFriends(tokenManager.getToken()!!)

        return binding.root
    }

    private fun retrieveAndShowFriends(token: String): List<AccountInfoBasic> {
        binding.friendsContent.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        val friends = mutableListOf<AccountInfoBasic>()

        val call = accountApi.getPrivateAccountInfo(token)
        call.enqueue(object : Callback<AccountInfoPrivate> {
            override fun onResponse(
                call: Call<AccountInfoPrivate>,
                response: Response<AccountInfoPrivate>
            ) {
                if (response.isSuccessful) {
                    val accountInfo = response.body()
                    Log.d(TAG, "Account info: $accountInfo")
                    if (accountInfo?.friends != null) {
                        Log.d(TAG, "Friends фыа: ${accountInfo.friends}")
                        for (friend in accountInfo.friends) {
                            friends.addAll(retrieveFriendInfo(friend))
                        }
                    }
                } else {
                    Log.d(TAG, "Failed to retrieve friends: ${response.message()}")
                    Toast.makeText(context, "Failed to retrieve friends", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AccountInfoPrivate>, t: Throwable) {
                Log.d(TAG, "Failed to retrieve friends: ${t.message}")
                Toast.makeText(context, "Failed to retrieve friends", Toast.LENGTH_SHORT).show()
            }
        })

        return friends
    }

    private fun retrieveFriendInfo(friend: String): List<AccountInfoBasic> {
        val friends = mutableListOf<AccountInfoBasic>()

        val call = accountApi.getPublicAccountInfo(friend)
        call.enqueue(object : Callback<AccountInfoPublic> {
            override fun onResponse(
                call: Call<AccountInfoPublic>,
                response: Response<AccountInfoPublic>
            ) {
                if (response.isSuccessful) {
                    val accountInfo = response.body()
                    Log.d(TAG, "Friend info: $accountInfo")
                    if (accountInfo != null) {
                        val visibleName = AccountVisibleName(null, null)
                        if (accountInfo.visibleName != null) {
                            if (accountInfo.visibleName.name != null) {
                                visibleName.name = accountInfo.visibleName.name
                                if (accountInfo.visibleName.surname != null) {
                                    visibleName.surname = accountInfo.visibleName.surname
                                }
                            }
                        }
                        if (visibleName.name == null) {
                            visibleName.name = accountInfo.nickname
                        }
                        val nickname = accountInfo.nickname
                        val friendInfo = AccountInfoBasic(friend, visibleName, nickname)
                        friends.add(friendInfo)
                        activity?.runOnUiThread {
                            (binding.friendsRecyclerView.adapter as FriendsRecyclerAdapter).setFriends(
                                friends
                            )
                            Log.d(TAG, "RecyclerView updated with friends: $friends")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<AccountInfoPublic>, t: Throwable) {
                Log.d(TAG, "Failed to retrieve friend: ${t.message}")
                Toast.makeText(context, "Failed to retrieve friend", Toast.LENGTH_SHORT).show()
            }
        })

        binding.friendsContent.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE

        return friends
    }
}