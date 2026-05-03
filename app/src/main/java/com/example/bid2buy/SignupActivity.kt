package com.example.bid2buy

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bid2buy.databinding.ActivitySignupBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.signupButton.setOnClickListener {
            performSignup()
        }
    }

    private fun performSignup() {
        val displayName = binding.displayNameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        if (displayName.isEmpty()) {
            binding.displayNameEditText.error = getString(R.string.error_display_name_required)
            showError(getString(R.string.error_enter_display_name))
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = getString(R.string.error_invalid_email)
            showError(getString(R.string.error_enter_valid_email))
            return
        }

        if (!isValidPassword(password)) {
            binding.passwordEditText.error = getString(R.string.error_invalid_password)
            showError(getString(R.string.error_password_criteria))
            return
        }

        if (password != confirmPassword) {
            binding.confirmPasswordEditText.error = getString(R.string.error_passwords_dont_match)
            showError(getString(R.string.error_passwords_dont_match))
            return
        }

        lifecycleScope.launch {
            val result = authRepository.signUp(email, password, displayName)
            if (result.isSuccess) {
                navigateToMain()
            } else {
                showError(getString(R.string.error_signup_failed, result.exceptionOrNull()?.message ?: "Unknown error"))
            }
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun showError(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        snackbar.view.background = ContextCompat.getDrawable(this, R.drawable.toast_background)
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.white))
        snackbar.show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
