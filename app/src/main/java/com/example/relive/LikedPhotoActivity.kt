package com.example.relive

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import com.bumptech.glide.Glide

class LikedPhotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_photo)

        // Retrieve the image path from the intent
        val imagePath = intent.getStringExtra("imagePath")

        // Find the ImageView by its ID
        val imageView = findViewById<ImageView>(R.id.likedPhotoImageView)

        // Load and display the liked photo using Glide (replace "R.drawable.placeholder_image" with imagePath)
        Glide.with(this)
            .load(imagePath)
            .into(imageView)
    }
}
