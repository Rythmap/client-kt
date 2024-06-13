package com.mvnh.rythmap

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.databinding.EditProfileBottomSheetBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountUpdateInfo
import com.mvnh.rythmap.retrofit.account.entities.AccountVisibleName
import com.mvnh.rythmap.utils.TokenManager
import com.mvnh.rythmap.vm.EditProfileSheetVM
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.apache.commons.io.IOUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: EditProfileBottomSheetBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi
    private val editProfileSheetVM: EditProfileSheetVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = EditProfileBottomSheetBinding.inflate(inflater, container, false)

        tokenManager = TokenManager(requireContext())
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        val musicPreferencesStringArray = resources.getStringArray(R.array.music_preferences)
        val musicPreferencesAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, musicPreferencesStringArray)
        binding.musicPreferencesDropdown.setAdapter(musicPreferencesAdapter)
        binding.musicPreferencesDropdown.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        val otherPreferencesStringArray = resources.getStringArray(R.array.other_preferences)
        val otherPreferencesAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, otherPreferencesStringArray)
        binding.otherPreferencesDropdown.setAdapter(otherPreferencesAdapter)
        binding.otherPreferencesDropdown.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        val pickPfpLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    uploadMedia(tokenManager.getToken(), "avatar", uri)
                } else {
                    Log.e(TAG, "Failed to pick media")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_pick_media),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        val pickBannerLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    uploadMedia(tokenManager.getToken(), "banner", uri)
                } else {
                    Log.e(TAG, "Failed to pick media")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_pick_media),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        binding.profilePfp.setOnClickListener {
            pickPfpLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.SingleMimeType("image/*")
                )
            )
        }
        binding.profileBanner.setOnClickListener {
            pickBannerLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.SingleMimeType("image/*")
                )
            )
        }

        binding.saveChangesButton.setOnClickListener {
            dialog?.dismiss()

            val accountUpdateInfo = AccountUpdateInfo(
                tokenManager.getToken()!!,
                AccountVisibleName(
                    binding.nameEditText.text.toString(),
                    binding.surnameEditText.text.toString()
                ),
                if (binding.musicPreferencesDropdown.text.toString().isNotBlank()) {
                    binding.musicPreferencesDropdown.text.toString().split(", ").filter { it.isNotBlank() }
                } else {
                    null
                },
                if (binding.otherPreferencesDropdown.text.toString().isNotBlank()) {
                    binding.otherPreferencesDropdown.text.toString().split(", ").filter { it.isNotBlank() }
                } else {
                    null
                },
                binding.aboutEditText.text.toString()
            )
            editProfileSheetVM.updateAccountInfo(accountUpdateInfo)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profilePfp.setImageBitmap(arguments?.getParcelable("avatar"))
        binding.profileBanner.setImageBitmap(arguments?.getParcelable("banner"))

        val visibleName = arguments?.getString("visibleName")?.split(" ")
        Log.d(TAG, "Visible name: $visibleName")
        if (visibleName != null) {
            if (visibleName[0] != arguments?.getString("username")) {
                binding.nameEditText.setText(visibleName[0])
            }
            if (visibleName.size > 1) {
                binding.surnameEditText.setText(visibleName[1])
            }
        }
        binding.aboutEditText.setText(arguments?.getString("description"))
        binding.musicPreferencesDropdown.setText(arguments?.getStringArrayList("musicPreferences")?.joinToString(", "))
        binding.otherPreferencesDropdown.setText(arguments?.getStringArrayList("otherPreferences")?.joinToString(", "))
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

    private fun uploadMedia(token: String?, type: String, uri: Uri) {
        token ?: return

        val uriInputStream = context?.contentResolver?.openInputStream(uri)
        val uriBytes = IOUtils.toByteArray(uriInputStream)
        val requestBody = uriBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, uriBytes.size)
        val filePart = MultipartBody.Part.createFormData("file", "file", requestBody)

        val call = accountApi.updateMedia(token, type, filePart)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Media uploaded")

                    if (type == "avatar") {
                        binding.profilePfp.setImageURI(uri)
                    } else {
                        binding.profileBanner.setImageURI(uri)
                    }
                } else {
                    Log.e(TAG, "Failed to upload media: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_upload_media),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to upload media", t)
                Toast.makeText(
                    context,
                    getString(R.string.failed_to_upload_media),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}