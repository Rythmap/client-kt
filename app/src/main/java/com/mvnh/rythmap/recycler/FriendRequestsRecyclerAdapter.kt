package com.mvnh.rythmap.recycler

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.mvnh.rythmap.R
import com.mvnh.rythmap.utils.TokenManager
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoBasic

class FriendRequestsRecyclerAdapter(private var friendRequests: List<AccountInfoBasic>, private val context: Context) :
    RecyclerView.Adapter<FriendRequestsRecyclerAdapter.FriendRequestViewHolder>() {

    class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePfp: ShapeableImageView = itemView.findViewById(R.id.profilePfp)
        val visibleNameTextView: TextView = itemView.findViewById(R.id.visibleNameTextView)
        val nicknameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptFriendRequestButton)
        val declineButton: Button = itemView.findViewById(R.id.declineFriendRequestButton)
    }

    private var tokenManager: TokenManager = TokenManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_request_item, parent, false)
        return FriendRequestViewHolder(itemView)
    }

    override fun getItemCount() = friendRequests.size

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val currentItem = friendRequests[position]

        TODO()
    }
}