package com.mvnh.rythmap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.databinding.ActivityMainBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.retrofit.yandex.entities.YandexToken
import com.mvnh.rythmap.utils.TokenManager
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fragmentManager: FragmentManager

    private lateinit var tokenManager: TokenManager
    private lateinit var accountApi: AccountApi
    private lateinit var yandexLauncher: ActivityResultLauncher<YandexAuthLoginOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        validateToken()

        val sdk = YandexAuthSdk.create(YandexAuthOptions(applicationContext))
        yandexLauncher = registerForActivityResult(sdk.contract) { result ->
            when (result) {
                is YandexAuthResult.Success -> {
                    Log.d(TAG, "Yandex auth result: ${result.token}")

                    val gson = Gson()
                    val yandexAuthToken =
                        gson.fromJson(result.token.toString()
                            .replace("YandexAuthToken", "")
                            .trim(), YandexToken::class.java)
                    Log.d(TAG, "Yandex token: ${yandexAuthToken.token}")
                    val token = yandexAuthToken.token

                    val yandexTokenSharedPref = applicationContext?.getSharedPreferences(
                        "yandexToken",
                        Context.MODE_PRIVATE
                    )
                    yandexTokenSharedPref?.edit()?.putString("yandexToken", token.toString())
                        ?.apply()

                    Toast.makeText(
                        this,
                        getString(R.string.yandex_music_linked),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is YandexAuthResult.Failure -> {
                    Log.e(TAG, "Yandex auth failed: ${result.exception}")
                    Toast.makeText(this, result.exception.toString(), Toast.LENGTH_SHORT).show()
                }

                YandexAuthResult.Cancelled -> {
                    Log.e(TAG, "Yandex auth cancelled")
                    Toast.makeText(
                        this,
                        getString(R.string.yandex_music_link_cancelled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val yandexTokenSharedPref =
            applicationContext?.getSharedPreferences("yandexToken", Context.MODE_PRIVATE)
        if (yandexTokenSharedPref != null) {
            val yandexToken = yandexTokenSharedPref.getString("yandexToken", null)
            if (yandexToken != null) {
                Log.d(TAG, "Yandex token: $yandexToken")
            } else {
                val alertDialogBuilder = MaterialAlertDialogBuilder(this)
                with(alertDialogBuilder) {
                    val view = layoutInflater.inflate(R.layout.link_musical_services_dialog, null)

                    val yandexMusicButton = view.findViewById<View>(R.id.yandexMusicIcon)
                    yandexMusicButton.setOnClickListener {
                        val loginOptions = YandexAuthLoginOptions()
                        yandexLauncher.launch(loginOptions)

                        alertDialogBuilder.create().dismiss()
                    }

                    val spotifyButton = view.findViewById<View>(R.id.spotifyMusicIcon)
                    spotifyButton.setOnClickListener {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.not_implemented_yet),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    setView(view)
                    setPositiveButton("Later") { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
            }
        }

        fragmentManager = supportFragmentManager
        binding.mainBottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.map -> {
                    loadFragment(MapFragment())
                    true
                }

                R.id.friends -> {
                    loadFragment(FriendsFragment())
                    true
                }

                R.id.account -> {
                    loadFragment(AccountFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, fragment).commit()
    }

    private fun validateToken() {
        val token = tokenManager.getToken()
        Log.d(TAG, "Token: $token")
        if (token.isNullOrEmpty()) {
            Log.d(TAG, "Token is null or empty")

            runOnUiThread {
                Toast.makeText(this, "Token is null or empty", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
        } else {
            val call = accountApi.getPrivateAccountInfo(token)
            call.enqueue(object : Callback<AccountInfoPrivate> {
                override fun onResponse(call: Call<AccountInfoPrivate>, response: Response<AccountInfoPrivate>) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Token is valid: ${response.body()?.token}")

                        val accountIdSharedPref = applicationContext?.getSharedPreferences(
                            "accountId",
                            Context.MODE_PRIVATE
                        )
                        Log.d(TAG, "Account ID: ${response.body()?.accountId}")
                        accountIdSharedPref?.edit()?.putString("accountId", response.body()?.accountId)
                            ?.apply()
                    } else {
                        Log.d(
                            TAG,
                            "Token ${tokenManager.getToken()} is invalid: ${
                                response.errorBody()?.string()
                            }"
                        )

                        tokenManager.clearToken()
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Token validation failed",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            finishAffinity()
                        }
                    }
                }

                override fun onFailure(call: Call<AccountInfoPrivate>, t: Throwable) {
                    Log.d(TAG, "Token ${tokenManager.getToken()} is invalid: $t")

                    tokenManager.clearToken()
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Token validation failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finishAffinity()
                    }
                }
            })
        }
    }
}