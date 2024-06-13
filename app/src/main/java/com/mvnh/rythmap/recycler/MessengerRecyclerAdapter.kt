package com.mvnh.rythmap.recycler

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.mvnh.rythmap.AccountFragment
import com.mvnh.rythmap.MainActivity
import com.mvnh.rythmap.R
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.retrofit.account.entities.AnyOtherFriendRequest
import com.mvnh.rythmap.retrofit.account.entities.SendFriendRequest
import com.mvnh.rythmap.utils.SecretData.SERVER_URL
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.utils.TokenManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessengerRecyclerAdapter(private var friends: List<AccountInfoPublic>, private val context: Context) :
    RecyclerView.Adapter<MessengerRecyclerAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePfp: ShapeableImageView = itemView.findViewById(R.id.profilePfp)
        val visibleNameTextView: TextView = itemView.findViewById(R.id.visibleNameLabel)
        val nicknameTextView: TextView = itemView.findViewById(R.id.usernameLabel)
        // val sendMessageButton: Button = itemView.findViewById(R.id.sendMessageButton)
        val addToFriendsButton: Button = itemView.findViewById(R.id.addToFriendsButton)
    }

    private var tokenManager: TokenManager = TokenManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_item, parent, false)
        return FriendViewHolder(itemView)
    }

    override fun getItemCount() = friends.size

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val currentItem = friends[position]

        var visibleName = ""
        if (currentItem.visibleName != null) {
            if (currentItem.visibleName.name != null) {
                visibleName = currentItem.visibleName.name!!
                if (currentItem.visibleName.surname != null) {
                    visibleName += " ${currentItem.visibleName.surname}"
                }
            }
        }
        if (visibleName == "" || visibleName.isBlank()) {
            visibleName = currentItem.nickname
        }
        holder.visibleNameTextView.text = visibleName

        holder.nicknameTextView.text = currentItem.nickname

        if (currentItem.avatar != null) {
            holder.profilePfp.load("https://$SERVER_URL/account/info/media/avatar?id=${currentItem.avatar}")
        }

        val accountIdSharedPref = context.getSharedPreferences("accountId", Context.MODE_PRIVATE)
        val accountIdString = accountIdSharedPref.getString("accountId", null)
        if (currentItem.accountId == accountIdString) {
            holder.addToFriendsButton.visibility = View.GONE
        }

        val nicknameSharedPref = context.getSharedPreferences("nickname", Context.MODE_PRIVATE)
        val nicknameString = nicknameSharedPref.getString("nickname", null)
        for (friend in friends) {
            if (currentItem.friends?.contains(nicknameString) == true) {
                holder.addToFriendsButton.visibility = View.GONE
            }
        }

        Log.d(TAG, "isFriendRequest for ${currentItem.nickname}: ${currentItem.isFriendRequest}")
        Log.d(TAG, "isFriend for ${currentItem.nickname}: ${currentItem.isFriend}")
        if (currentItem.isFriendRequest) {
            holder.addToFriendsButton.background =
                AppCompatResources.getDrawable(context, R.drawable.menu)

            holder.addToFriendsButton.setOnClickListener {
                val popupMenu = PopupMenu(context, holder.addToFriendsButton)
                popupMenu.menu.add(holder.itemView.context.getString(R.string.accept))
                popupMenu.menu.add(holder.itemView.context.getString(R.string.decline))

                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        holder.itemView.context.getString(R.string.accept) -> {
                            acceptFriendRequest(holder.nicknameTextView.text.toString(), tokenManager.getToken()!!)
                        }
                        holder.itemView.context.getString(R.string.decline) -> {
                            declineFriendRequest(holder.nicknameTextView.text.toString(), tokenManager.getToken()!!)
                        }
                    }
                    true
                }

                popupMenu.show()
            }
        } else if (currentItem.isFriend) {
            holder.addToFriendsButton.visibility = View.GONE
        } else {
            holder.addToFriendsButton.setOnClickListener {
                val accountApi = ServiceGenerator.createService(AccountApi::class.java)
                val call = accountApi.sendFriendRequest(
                    SendFriendRequest(
                        holder.nicknameTextView.text.toString(),
                        tokenManager.getToken()!!
                    )
                )
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "Friend request sent successfully")
                            Toast.makeText(context, context.getString(R.string.friend_request_sent), Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Log.e(TAG, "Failed to send friend request: ${response.raw()}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e(TAG, "Failed to send friend request", t)
                    }
                })
            }
        }

        holder.itemView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("nickname", currentItem.nickname)

            val fragment = AccountFragment()
            fragment.arguments = bundle

            val fragmentManager = (context as MainActivity).supportFragmentManager
            fragmentManager.beginTransaction().apply {
                replace(R.id.fragmentContainerView, fragment)
                addToBackStack(null)
                commit()
            }
        }
    }

    fun acceptFriendRequest(toNickname: String, fromToken: String) {
        val accountApi = ServiceGenerator.createService(AccountApi::class.java)
        val call = accountApi.acceptFriendRequest(
            AnyOtherFriendRequest(
                toNickname,
                fromToken
            )
        )
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Friend request accepted successfully")
                    Toast.makeText(context, context.getString(R.string.friend_request_accepted), Toast.LENGTH_SHORT)
                        .show()

                } else {
                    Log.e(TAG, "Failed to accept friend request: ${response.raw()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to accept friend request", t)
            }
        })
    }

    fun declineFriendRequest(toNickname: String, fromToken: String) {
        val accountApi = ServiceGenerator.createService(AccountApi::class.java)
        val call = accountApi.declineFriendRequest(
            AnyOtherFriendRequest(
                toNickname,
                fromToken
            )
        )
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Friend request declined successfully")
                    Toast.makeText(context, context.getString(R.string.friend_request_declined), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.e(TAG, "Failed to decline friend request: ${response.raw()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to decline friend request", t)
            }
        })
    }

    fun setFriends(newFriends: List<AccountInfoPublic>) {
        friends = newFriends.toMutableList()
        notifyDataSetChanged()
    }
}