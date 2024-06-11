package com.mvnh.rythmap.recycler

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.mvnh.rythmap.AccountFragment
import com.mvnh.rythmap.MainActivity
import com.mvnh.rythmap.R
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.utils.TokenManager
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoBasic
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPublic
import com.mvnh.rythmap.retrofit.account.entities.SendFriendRequest
import com.mvnh.rythmap.utils.SecretData.SERVER_URL
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
        val sendMessageButton: Button = itemView.findViewById(R.id.sendMessageButton)
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

        holder.profilePfp.load("https://$SERVER_URL/account/info/media/avatar?id=${currentItem.avatar}")

        val accountId = context.getSharedPreferences("accountId", Context.MODE_PRIVATE).getString("accountId", null)
        if (currentItem.accountId == accountId) {
            holder.addToFriendsButton.visibility = View.GONE
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

        holder.addToFriendsButton.setOnClickListener {
            val accountApi = ServiceGenerator.createService(AccountApi::class.java)
            val call = accountApi.sendFriendRequest(
                SendFriendRequest(tokenManager.getToken()!!, currentItem.nickname)
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

    fun setFriends(newFriends: List<AccountInfoPublic>) {
        friends = newFriends.toMutableList()
        notifyDataSetChanged()
    }
}