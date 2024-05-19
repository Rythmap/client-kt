package com.mvnh.rythmap

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.mvnh.rythmap.SecretData.TAG
import com.mvnh.rythmap.responses.account.entities.AccountInfoBasic

class FriendsRecyclerAdapter(private var friends: List<AccountInfoBasic>) :
    RecyclerView.Adapter<FriendsRecyclerAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePfp: ShapeableImageView = itemView.findViewById(R.id.profilePfp)
        val visibleNameTextView: TextView = itemView.findViewById(R.id.visibleNameLabel)
        val nicknameTextView: TextView = itemView.findViewById(R.id.usernameLabel)
        val sendMessageButton: Button = itemView.findViewById(R.id.sendMessageButton)
    }

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
                Log.d(TAG, "Visible name: $visibleName")
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
    }

    fun setFriends(newFriends: List<AccountInfoBasic>) {
        friends = newFriends.toMutableList()
        notifyDataSetChanged()
    }
}