package com.example.relive

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
object AccountDeletionHelper {

    fun deleteAccount(context: Context, email: String, password: String) {
        val user = FirebaseAuth.getInstance().currentUser
        // Step 1: Reauthenticate the user
        authenticateUser(email, password)
            .addOnSuccessListener {
                // Reauthentication successful, proceed with account deletion
                user?.let { currentUser ->
                    val storageRef = FirebaseStorage.getInstance().reference
                    val databaseRef = FirebaseDatabase.getInstance().reference

                    // Step 2: Delete user-related photos from Firebase Storage
                    val photosFolder = "users/${currentUser.uid}/photos"
                    val photosStorageRef = storageRef.child(photosFolder)

                    // List all items (photos) in the folder
                    photosStorageRef.listAll()
                        .addOnSuccessListener { items ->
                            val deletePhotoTasks = mutableListOf<Task<Void>>()

                            // Delete each photo
                            for (item in items.items) {
                                val deleteTask = item.delete()
                                deletePhotoTasks.add(deleteTask)
                            }

                            // Wait for all delete tasks to complete
                            Tasks.whenAll(deletePhotoTasks)
                                .addOnSuccessListener {
                                    // Step 3: Delete the user account
                                    currentUser.delete()
                                        .addOnSuccessListener {
                                            // Account deleted successfully
                                            // Log out the user and redirect
                                            logout(context)
                                        }
                                        .addOnFailureListener { exception ->
                                            // Handle account deletion failure
                                            Toast.makeText(
                                                context,
                                                "Failed to delete account: ${exception.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    // Handle photo deletion failure
                                    Toast.makeText(
                                        context,
                                        "Failed to delete photos: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .addOnFailureListener { exception ->
                            // Handle list items (photos) failure
                            Toast.makeText(
                                context,
                                "Failed to list photos: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                // Handle reauthentication failure
                Toast.makeText(
                    context,
                    "Re-authentication failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ...





    private fun authenticateUser(email: String, password: String): Task<Void> {
        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(email, password)
        return user?.reauthenticate(credential) ?: Tasks.forException(
            Exception("User is null")
        )
    }

    private fun logout(context: Context) {
        // Handle user logout logic (e.g., sign out from Firebase Authentication)
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signOut()

        // Clear the app's cache directory
        clearAppCache(context)

        // Redirect to the LoginActivity or any other desired screen
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

    private fun clearAppCache(context: Context) {
        // Clear the app's cache directory logic
        // ...
    }
}

