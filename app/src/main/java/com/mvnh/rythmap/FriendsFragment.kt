package com.mvnh.rythmap

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.databinding.FragmentFriendsBinding
import com.mvnh.rythmap.recycler.MessengerRecyclerAdapter
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoBasic
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.retrofit.account.entities.AccountVisibleName
import com.mvnh.rythmap.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FriendsFragment : Fragment() {

    private lateinit var binding: FragmentFriendsBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi

    private var retrieveFriendsCallsCompleted = 0

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
            searchFriends(it.toString())
        }

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
                        Log.d(TAG, "Friends: ${accountInfo.friends}")
                        for (friend in accountInfo.friends) {
                            friends.addAll(retrieveFriendInfo(friend, accountInfo.friends.size))
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

    private fun retrieveFriendInfo(friend: String, friendsAmount: Int): List<AccountInfoBasic> {
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
                        val friendInfo = AccountInfoBasic(accountInfo.accountId, nickname, visibleName, accountInfo.createdAt)

                        friends.add(friendInfo)
                        activity?.runOnUiThread {
                            (binding.friendsRecyclerView.adapter as MessengerRecyclerAdapter).setFriends(
                                friends
                            )
                            Log.d(TAG, "RecyclerView updated with friends: $friends")
                        }

                        retrieveFriendsCallsCompleted++
                        if (retrieveFriendsCallsCompleted == friendsAmount) {
                            binding.progressBar.visibility = View.GONE
                            binding.friendsContent.visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun onFailure(call: Call<AccountInfoPublic>, t: Throwable) {
                Log.d(TAG, "Failed to retrieve friend: ${t.message}")

                retrieveFriendsCallsCompleted++
                if (retrieveFriendsCallsCompleted == friendsAmount) {
                    binding.progressBar.visibility = View.GONE
                    binding.friendsContent.visibility = View.VISIBLE
                }
            }
        })

        return friends
    }

    private fun searchFriends(query: String) {
        val call = accountApi.searchFriends(query)
        call.enqueue(object : Callback<Map<String, AccountInfoBasic>> {
            override fun onResponse(
                call: Call<Map<String, AccountInfoBasic>>,
                response: Response<Map<String, AccountInfoBasic>>
            ) {
                if (response.isSuccessful) {
                    val friendsResponse = response.body()
                    Log.d(TAG, "Friends found: $friendsResponse")

                    val friends = mutableListOf<AccountInfoBasic>()
                    friendsResponse?.forEach { (nickname, accountInfo) ->
                        val friend = AccountInfoBasic(
                            accountInfo.accountId,
                            nickname,
                            accountInfo.visibleName,
                            accountInfo.createdAt
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
                    Toast.makeText(context, "Failed to search friends", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, AccountInfoBasic>>, t: Throwable) {
                Log.d(TAG, "Failed to search friends: ${t.message}")
                Toast.makeText(context, "Failed to search friends", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        retrieveAndShowFriends(tokenManager.getToken()!!)
        binding.searchEditText.text?.clear()
        binding.friendsContent.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }
}