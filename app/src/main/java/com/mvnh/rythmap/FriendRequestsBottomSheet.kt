package com.mvnh.rythmap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mvnh.rythmap.databinding.FriendRequestsBottomSheetBinding
import com.mvnh.rythmap.responses.ServiceGenerator
import com.mvnh.rythmap.responses.account.AccountApi

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
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }
}