package com.example.relive

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var cancelDeleteButton:Button
    private lateinit var confirmDeleteButton:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)

        // Initialize buttons
        confirmDeleteButton = findViewById(R.id.confirmDeleteButton)
        cancelDeleteButton = findViewById(R.id.cancelDeleteButton)

        // Handle "Yes! Confirm" button click
        confirmDeleteButton.setOnClickListener {
            Toast.makeText(
                this, "Deleted successfully",
                Toast.LENGTH_SHORT
            ).show()
            // Get the currently signed-in user
            val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // Get the user's UID
                val userId = currentUser.uid

                // Initialize Firebase Storage reference
                val storageRef = FirebaseStorage.getInstance().reference

                // Define the path to the user's photos in Firebase Storage
                val photosPath = "users/$userId/liked_images"

                // Create a reference to the user's photos folder
                val userPhotosRef = storageRef.child(photosPath)

                // Delete all photos in the folder
                userPhotosRef.listAll()
                    .addOnSuccessListener { result ->
                        val deleteTasks = mutableListOf<Task<Void>>()

                        for (photoRef in result.items) {
                            val deleteTask = photoRef.delete()
                            deleteTasks.add(deleteTask)
                        }

                        // Wait for all photo deletions to complete
                        Tasks.whenAllComplete(deleteTasks)
                            .addOnSuccessListener {
                                // All photos deleted successfully

                                // Now, delete the user's account
                                currentUser.delete()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            // User account deleted successfully

                                            // Redirect back to the login screen or any other appropriate screen
                                            val intent = Intent(this, LoginActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            // Handle errors while deleting the user account
                                            // You may want to display an error message to the user
                                        }
                                    }
                            }
                            .addOnFailureListener { exception ->
                                // Handle errors while deleting photos
                                // You may want to display an error message to the user
                            }
                    }
                    .addOnFailureListener { exception ->
                        // Handle errors while listing photos
                        // You may want to display an error message to the user
                    }
            }
        }



        // Handle "No" button click
        cancelDeleteButton.setOnClickListener {
            // Redirect back to the MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}
