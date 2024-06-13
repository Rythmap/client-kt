package com.mvnh.rythmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.databinding.ActivityLoginBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountAuth
import com.mvnh.rythmap.retrofit.account.entities.AccountLogin
import com.mvnh.rythmap.retrofit.account.entities.AccountRegister
import com.mvnh.rythmap.retrofit.account.entities.AccountVisibleName
import com.mvnh.rythmap.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        binding.loginButton.setOnClickListener {
            performAuth(
                binding.usernameField.text.toString(),
                password = binding.passwordField.text.toString(),
                email = binding.emailField.text.toString(),
            )
        }

        binding.passwordFieldLayout.setStartIconOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.password_reqs)
                .setMessage(R.string.password_reqs_message)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        binding.registerButton.setOnClickListener {
            if (binding.registerButton.text.toString() == getString(R.string.don_t_have_an_account)) {
                binding.emailFieldLayout.visibility = android.view.View.VISIBLE
                binding.usernameFieldLayout.hint = getString(R.string.username)
                binding.loginButton.text = getString(R.string.register)
                binding.registerButton.text = getString(R.string.already_have_an_account)
            } else {
                binding.emailField.text = null
                binding.emailFieldLayout.visibility = android.view.View.GONE
                binding.usernameFieldLayout.hint = getString(R.string.username_or_email)
                binding.loginButton.text = getString(R.string.login)
                binding.registerButton.text = getString(R.string.don_t_have_an_account)
            }
        }

        if (tokenManager.getToken() != null) {
            runOnUiThread {
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
        }
    }

    private fun performAuth(
        nickname: String,
        visibleName: AccountVisibleName? = null,
        password: String,
        email: String,
        musicPreferences: List<String>? = null,
        otherPreferences: List<String>? = null,
        about: String? = null,
    ) {
        val accountApi = ServiceGenerator.createService(AccountApi::class.java)

        val call: Call<AccountAuth> = if (email.isNotEmpty()) {
            accountApi.register(AccountRegister(nickname, visibleName, password, email, musicPreferences, otherPreferences, about))
        } else {
            accountApi.login(AccountLogin(nickname, password))
        }

        call.enqueue(object: Callback<AccountAuth> {
            override fun onResponse(call: Call<AccountAuth>, response: Response<AccountAuth>) {
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        tokenManager.saveToken(token)

                        Log.d(TAG, "Token: $token")

                        runOnUiThread {
                            if (email.isNotEmpty()) {
                                startActivity(Intent(this@LoginActivity, AddMoreInfoActivity::class.java))
                                finishAffinity()
                            } else {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finishAffinity()
                            }
                        }
                    } else {
                        val errorResponse = response.errorBody()?.string()
                        Log.e(TAG, "LoginActivity: $errorResponse")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, errorResponse, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorResponse = response.errorBody()?.string()
                    Log.e(TAG, "LoginActivity: $errorResponse")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, errorResponse, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<AccountAuth>, t: Throwable) {
                Log.e(TAG, "LoginActivity: $t")
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, t.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}