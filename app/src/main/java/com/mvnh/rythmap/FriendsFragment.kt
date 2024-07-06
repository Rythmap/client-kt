package com.mvnh.rythmap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.mvnh.rythmap.databinding.FragmentFriendsBinding
import com.mvnh.rythmap.recycler.MessengerRecyclerAdapter
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.retrofit.account.entities.AccountVisibleName
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.utils.TokenManager
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
            MessengerRecyclerAdapter(mutableListOf(), requireContext())
        retrieveAndShowFriends(tokenManager.getToken()!!)

        binding.searchEditText.addTextChangedListener {
            if (!it.isNullOrBlank()) {
                searchFriends(it.toString())
            }
        }

        return binding.root
    }

    private fun retrieveAndShowFriends(token: String) {
        val friends = mutableListOf<AccountInfoPublic>()
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
                        val friendsList = accountInfo.friends.toMutableList()
                        if (accountInfo.friendRequests != null) {
                            friendsList += accountInfo.friendRequests
                        }
                        for (friend in friendsList) {
                            val friendInfo = retrieveFriendInfo(friend)
                            friends.addAll(friendInfo)
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

        activity?.runOnUiThread {
            (binding.friendsRecyclerView.adapter as MessengerRecyclerAdapter).setFriends(
                friends
            )
            Log.d(TAG, "RecyclerView updated with friends: $friends")
        }
    }

    private fun retrieveFriendInfo(friend: String): List<AccountInfoPublic> {
        val friends = mutableListOf<AccountInfoPublic>()

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
                        val friendInfo = AccountInfoPublic(
                            accountId = accountInfo.accountId,
                            nickname = accountInfo.nickname,
                            visibleName = visibleName,
                            avatar = accountInfo.avatar,
                            createdAt = accountInfo.createdAt
                        )

                        friends.add(friendInfo)
                        activity?.runOnUiThread {
                            (binding.friendsRecyclerView.adapter as MessengerRecyclerAdapter).setFriends(
                                friends
                            )
                            Log.d(TAG, "RecyclerView updated with friends: $friends")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<AccountInfoPublic>, t: Throwable) {
                Log.d(TAG, "Failed to retrieve friend: ${t.message}")
            }
        })

        return friends
    }

    private fun searchFriends(query: String) {
        val call = accountApi.searchFriends(query)
        call.enqueue(object : Callback<Map<String, AccountInfoPublic>> {
            override fun onResponse(
                call: Call<Map<String, AccountInfoPublic>>,
                response: Response<Map<String, AccountInfoPublic>>
            ) {
                if (response.isSuccessful) {
                    val friendsResponse = response.body()
                    Log.d(TAG, "Friends found: $friendsResponse")

                    val friends = mutableListOf<AccountInfoPublic>()
                    friendsResponse?.forEach { (_, accountInfo) ->
                        val friend = AccountInfoPublic(
                            accountId = accountInfo.accountId,
                            nickname = accountInfo.nickname,
                            visibleName = accountInfo.visibleName,
                            avatar = accountInfo.avatar,
                            createdAt = accountInfo.createdAt
                        )
                        friends.add(friend)

                        activity?.runOnUiThread {
                            (binding.friendsRecyclerView.adapter as MessengerRecyclerAdapter).setFriends(
                                friends
                            )
                            Log.d(TAG, "RecyclerView updated with friends: $friends")
                        }
                    }
                } else {
                    Log.d(TAG, "Failed to search friends: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, AccountInfoPublic>>, t: Throwable) {
                Log.d(TAG, "Failed to search friends: ${t.message}")
                Toast.makeText(context, "Failed to search friends", Toast.LENGTH_SHORT).show()
            }
        })
    }
}