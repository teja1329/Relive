package com.example.relive
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LikedPhotosActivity : AppCompatActivity() {

    var imagePath: String? = null
    private lateinit var recyclerViewLikedPhotos: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_photos)

        // Find the RecyclerView to display liked photos
        recyclerViewLikedPhotos = findViewById(R.id.recyclerViewLikedPhotos)

        // Set up a GridLayoutManager with 3 columns
        val spanCount = 3
        recyclerViewLikedPhotos.layoutManager = GridLayoutManager(this, spanCount)

        // Initialize the adapter
        adapter = PhotoAdapter()

        // Set the adapter for the RecyclerView
        recyclerViewLikedPhotos.adapter = adapter


        adapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(imagePath: String) {
                // Create an intent to start the LikedPhotoActivity
                val intent = Intent(this@LikedPhotosActivity, LikedPhotoActivity::class.java)
                intent.putExtra("imagePath", imagePath)

                // Start the LikedPhotoActivity with the selected image path
                startActivity(intent)
            }
        })



        // Fetch and display liked photos from Firebase Storage for the current user
        fetchLikedPhotosForCurrentUser()
    }

    private fun fetchLikedPhotosForCurrentUser() {
        // Check if a user is logged in
        user?.let { currentUser ->
            // Define the path based on the user's UID
            val likedPhotosFolder = "users/${currentUser.uid}/liked_images"

            // List all files (images) in the user's liked photos folder
            storageRef.child(likedPhotosFolder).listAll()
                .addOnSuccessListener { listResult ->
                    val likedPhotoUrls = mutableListOf<String>()

                    // Iterate through the files and add their download URLs to the list
                    listResult.items.forEach { item ->
                        item.downloadUrl
                            .addOnSuccessListener { uri ->
                                val imageUrl = uri.toString()
                                likedPhotoUrls.add(imageUrl)

                                // Update the adapter with the new URLs
                                adapter.setData(likedPhotoUrls)
                            }
                            .addOnFailureListener { exception ->
                                // Handle any errors that may occur while getting the download URL
                                Log.e("FirebaseStorage", "Failed to get download URL: ${exception.message}", exception)
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors that may occur during the fetch
                    // You can display an error message or take appropriate action here
                    Log.e("FirebaseStorage", "Failed to list files: ${exception.message}", exception)
                }
        }
    }

}

