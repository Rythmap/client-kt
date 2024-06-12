package com.mvnh.rythmap

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.databinding.ActivityAddMoreInfoBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.retrofit.account.entities.AccountUpdateInfo
import com.mvnh.rythmap.retrofit.account.entities.AccountVisibleName
import com.mvnh.rythmap.utils.SecretData.SERVER_URL
import com.mvnh.rythmap.utils.TokenManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.apache.commons.io.IOUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddMoreInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMoreInfoBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddMoreInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        val musicPreferencesStringArray = resources.getStringArray(R.array.music_preferences)
        val musicPreferencesAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, musicPreferencesStringArray)
        binding.musicPreferencesDropdown.setAdapter(musicPreferencesAdapter)
        binding.musicPreferencesDropdown.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        val otherPreferencesStringArray = resources.getStringArray(R.array.other_preferences)
        val otherPreferencesAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, otherPreferencesStringArray)
        binding.otherPreferencesDropdown.setAdapter(otherPreferencesAdapter)
        binding.otherPreferencesDropdown.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        val pickPfpLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    uploadMedia(tokenManager.getToken(), "avatar", uri)
                } else {
                    Log.e(TAG, "Failed to pick media")
                    Toast.makeText(this, "Failed to pick media", Toast.LENGTH_SHORT).show()
                }
            }
        val pickBannerLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    uploadMedia(tokenManager.getToken(), "banner", uri)
                } else {
                    Log.e(TAG, "Failed to pick media")
                    Toast.makeText(this, "Failed to pick media", Toast.LENGTH_SHORT).show()
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

        binding.doneStageButton.setOnClickListener {
            updateAccountInfo(
                AccountUpdateInfo(
                    token = tokenManager.getToken()!!,
                    visibleName = AccountVisibleName(
                        binding.nameEditText.text.toString(),
                        binding.surnameEditText.text.toString()
                    ),
                    musicPreferences = if (binding.musicPreferencesDropdown.text.toString().isNotBlank()) {
                        binding.musicPreferencesDropdown.text.toString().split(", ")
                    } else {
                        null
                    },
                    otherPreferences = if (binding.otherPreferencesDropdown.text.toString().isNotBlank()) {
                        binding.otherPreferencesDropdown.text.toString().split(", ")
                    } else {
                        null
                    },
                    about = binding.aboutEditText.text.toString()
                )
            )
        }
        binding.skipStageButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }

    private fun updateAccountInfo(accountUpdateInfo: AccountUpdateInfo) {
        val call = accountApi.updateInfo(accountUpdateInfo)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    startActivity(Intent(this@AddMoreInfoActivity, MainActivity::class.java))
                    finishAffinity()
                } else {
                    Log.e(TAG, response.raw().toString())
                    Toast.makeText(
                        this@AddMoreInfoActivity,
                        "Failed to update account info",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, t.toString())
                Toast.makeText(
                    this@AddMoreInfoActivity,
                    "Failed to update account info",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun uploadMedia(token: String?, type: String, uri: Uri) {
        token ?: return

        val uriInputStream = contentResolver.openInputStream(uri)
        val uriBytes = IOUtils.toByteArray(uriInputStream)
        val requestBody = uriBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, uriBytes.size)
        val filePart = MultipartBody.Part.createFormData("file", "file", requestBody)

        val call = accountApi.updateMedia(token, type, filePart)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Media uploaded")
                    retrieveMedia(token, type)
                } else {
                    Log.e(TAG, "Failed to upload media: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@AddMoreInfoActivity,
                        "Failed to upload media",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to upload media", t)
                Toast.makeText(
                    this@AddMoreInfoActivity,
                    "Failed to upload media",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun retrieveMedia(token: String, type: String) {
        val getAccountInfo = accountApi.getPrivateAccountInfo(token)
        getAccountInfo.enqueue(object : Callback<AccountInfoPrivate> {
            override fun onResponse(
                call: Call<AccountInfoPrivate>,
                response: Response<AccountInfoPrivate>
            ) {
                if (response.isSuccessful) {
                    val accountInfo = response.body()
                    if (accountInfo != null) {
                        if (type == "avatar") {
                            binding.profilePfp.load("https://$SERVER_URL/account/info/media/avatar?id=${accountInfo.avatar}")
                        } else if (type == "banner") {
                            binding.profileBanner.load("https://$SERVER_URL/account/info/media/banner?id=${accountInfo.banner}")
                        }
                    } else {
                        Log.e(TAG, "Failed to retrieve account info")
                        Toast.makeText(
                            this@AddMoreInfoActivity,
                            "Failed to retrieve account info",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e(TAG, "Failed to retrieve account info: ${response.message()}")
                    Toast.makeText(
                        this@AddMoreInfoActivity,
                        "Failed to retrieve account info",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<AccountInfoPrivate>, t: Throwable) {
                Log.e(TAG, "Failed to retrieve account info", t)
                Toast.makeText(
                    this@AddMoreInfoActivity,
                    "Failed to retrieve account info",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}