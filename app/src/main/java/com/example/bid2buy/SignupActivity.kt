package com.example.bid2buy

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bid2buy.databinding.ActivitySignupBinding
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
            binding.displayNameEditText.error = "Display name is required"
            showErrorToast("Please enter a display name")
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Invalid email"
            showErrorToast("Please enter a valid email address")
            return
        }

        if (!isValidPassword(password)) {
            binding.passwordEditText.error = "Invalid password"
            showErrorToast("Password must be at least 8 characters, with 1 uppercase, 1 number, and 1 special character")
            return
        }

        if (password != confirmPassword) {
            binding.confirmPasswordEditText.error = "Passwords do not match"
            showErrorToast("Passwords do not match")
            return
        }

        lifecycleScope.launch {
            val result = authRepository.signUp(email, password, displayName)
            if (result.isSuccess) {
                navigateToMain()
            } else {
                showErrorToast("Signup failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun showErrorToast(message: String) {
        val layout = layoutInflater.inflate(R.layout.custom_error_toast, findViewById(R.id.custom_toast_container))
        layout.findViewById<TextView>(R.id.toast_text).text = message

        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.view = layout
        toast.show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}