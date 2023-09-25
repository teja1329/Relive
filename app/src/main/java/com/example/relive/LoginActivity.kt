package com.example.relive

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.common.SignInButton


class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var createAccountButton: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInButton: SignInButton
    private lateinit var googleApiClient: GoogleApiClient
    private val RC_SIGN_IN = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_login)
        sessionManager = SessionManager(this)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signInButton = findViewById(R.id.signInButton)
        createAccountButton = findViewById(R.id.createAccountButton)


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this) { connectionResult ->
                Toast.makeText(
                    this,
                    "Google Play services error. Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()

        googleSignInButton = findViewById(R.id.googleSignInButton)

        googleSignInButton.setOnClickListener {
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        signInButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Sign in with Firebase Authentication
            if (username.isEmpty() || password.isEmpty()) {
                // Check if either email or password is empty
                Toast.makeText(
                    this, "Please enter both email and password.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
            firebaseAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in successful, navigate to the main activity
                        sessionManager.login()

                        val intent = Intent(this, MainActivity::class.java)
                        Toast.makeText(
                            this, "Signing in.",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(intent)
                        finish()
                    } else {
                        // Sign in failed, check the exception for details
                        val exception = task.exception
                        if (exception is FirebaseAuthInvalidCredentialsException) {
                            // Incorrect password
                            Toast.makeText(
                                this, "Incorrect password. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Other authentication errors, display a generic message
                            Toast.makeText(
                                this, "Authentication failed. Please create an account.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                }
        }

        createAccountButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                // Check if either email or password is empty
                Toast.makeText(
                    this, "Please enter both email and password.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Implement your custom password strength checks here
            if (!isStrongPassword(password)) {
                // Password is not strong enough, display an error message
                Toast.makeText(this, "Password should be at least 8 characters long and contain a combination of letters, numbers, and special characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else {
                // Check if the account already exists
                firebaseAuth.fetchSignInMethodsForEmail(username)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val signInMethods = task.result?.signInMethods
                            if (signInMethods != null && signInMethods.isNotEmpty()) {
                                // Account with this email already exists, display a toast message
                                Toast.makeText(
                                    this, "Account with this email already exists.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Account does not exist, create an account with Firebase Authentication
                                firebaseAuth.createUserWithEmailAndPassword(username, password)
                                    .addOnCompleteListener(this) { createUserTask ->
                                        if (createUserTask.isSuccessful) {
                                            // Account creation successful, display a toast message
                                            Toast.makeText(
                                                this,
                                                "Account created successfully. You can now sign in.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // Account creation failed, display a toast message
                                            Toast.makeText(
                                                this,
                                                "Account creation failed. Please try again later.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            }
                        } else {
                            // Error occurred while checking for account existence
                            Toast.makeText(
                                this, "Error checking account existence. Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

    }

    private fun isStrongPassword(password: String): Boolean {
        // Check if the password is at least 8 characters long
        if (password.length < 8) {
            return false
        }

        // Check if the password contains at least one letter, one number, and one special character
        val letterPattern = Regex("[a-zA-Z]")
        val digitPattern = Regex("[0-9]")
        val specialCharPattern = Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?~]")

        return letterPattern.containsMatchIn(password) &&
                digitPattern.containsMatchIn(password) &&
                specialCharPattern.containsMatchIn(password)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                Toast.makeText(this, "Google Sign-In failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in with Google successful, navigate to the main activity
                    sessionManager.login()

                    val intent = Intent(this, MainActivity::class.java)
                    Toast.makeText(
                        this, "Signing in with Google.",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(intent)
                    finish()
                } else {
                    // Sign in with Google failed
                    Toast.makeText(this, "Google Sign-In failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
