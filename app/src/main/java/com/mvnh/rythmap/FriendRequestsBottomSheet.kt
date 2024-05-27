package com.mvnh.rythmap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mvnh.rythmap.databinding.FriendRequestsBottomSheetBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.utils.TokenManager

class FriendRequestsBottomSheet: BottomSheetDialogFragment() {

    private lateinit var binding: FriendRequestsBottomSheetBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FriendRequestsBottomSheetBinding.inflate(inflater, container, false)

        tokenManager = TokenManager(requireContext())
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // TODO
        // will be continued soon

        return super.onCreateDialog(savedInstanceState)
    }
}