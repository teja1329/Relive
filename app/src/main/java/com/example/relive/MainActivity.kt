
package com.example.relive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var photoImageView: ImageView
    private lateinit var shareButton: Button
    private lateinit var gestureDetector: GestureDetector
    private lateinit var currentImagePath: String
    private var initialTouchX = 0f
    private lateinit var heartLogo: ImageView
    private val likedPhotosList = mutableListOf<String>()
    private lateinit var menuIcon: ImageView

    private lateinit var heartIcon: ImageView
    private var initialTouchY = 0f
    private val SWIPE_THRESHOLD = 100 // Adjust this value as needed
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var lastTapTime: Long = 0
    private val DOUBLE_TAP_TIME_DELTA: Long = 300 // Adjust this value as needed
    private lateinit var heartLogoFrame: FrameLayout

    private var isLiked = false
    private var initialTranslateX = 0f
    private var initialTranslateY = 0f
    private var isScrolling = false
    private var offsetX = 0f
    private var offsetY = 0f
    private lateinit var dateTextView: TextView
    private val imageList = mutableListOf<String>()
    private var currentIndex = -1
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private val visitedPhotosStack = Stack<Int>()
    private lateinit var rotateButton: Button
    private var isZoomedIn = false
    private var initialScaleFactor = 1.0f
    private lateinit var initialImageMatrix: Matrix

    private var isScaling = false
    private var initialImageWidth = 0
    private var initialImageHeight = 0
    private lateinit var imageView: ImageView
    private val initialScaleX = 1.0f
    private val initialScaleY = 1.0f
    private lateinit var logoutbutton:Button
    private lateinit var sessionManager: SessionManager

    private lateinit var shareIcon: ImageView
    private lateinit var deleteIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        setContentView(R.layout.activity_main)
        initializeLikedPhotosSet()

        menuIcon = findViewById(R.id.menuIcon)

        menuIcon.setOnClickListener {
            showPopupMenu(it)
        }
        gestureDetector = GestureDetector(this, GestureListener())
        heartIcon = findViewById(R.id.heartIcon)
        gestureDetector = GestureDetector(this, GestureListener())
        imageView = findViewById(R.id.photoImageView)

        photoImageView = findViewById(R.id.photoImageView)
        dateTextView = findViewById(R.id.dateTextView)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        imageView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                initialImageWidth = imageView.width
                initialImageHeight = imageView.height
            }
        })
//        logoutbutton = findViewById(R.id.logoutbutton)
//        logoutbutton.setOnClickListener{
//            signOut()
//        }
//
//        heartLogo = findViewById(R.id.heartLogo)
//        heartLogo.setOnClickListener {
//            Toast.makeText(this, "showing Likedphotos", Toast.LENGTH_SHORT).show()
//            val intent = Intent(this, LikedPhotosActivity::class.java)
//            startActivity(intent)
//        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )
        } else {
            loadPhotosFromGallery()
        }


        imageView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.x
                    initialTouchY = event.y
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - initialTouchX
                    val deltaY = event.y - initialTouchY

                    if (deltaX < 20 && deltaY < 20) {
                        // The user performed a tap (not a swipe), show the icons
                        shareIcon.visibility = View.VISIBLE
                        deleteIcon.visibility = View.VISIBLE
                    }
                }
            }
            true
        }

        shareIcon = findViewById(R.id.shareIcon)
        deleteIcon = findViewById(R.id.deleteIcon)

        photoImageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                // Hide the logos after touch is released
                hideLogos()
            }
            true
        }

        shareIcon.setOnClickListener {
            sharePhoto()
            // Hide the icons after sharing
            shareIcon.visibility = View.GONE
            deleteIcon.visibility = View.GONE
        }

        deleteIcon.setOnClickListener {
            deleteImage()
            // Hide the icons after deleting
            shareIcon.visibility = View.GONE
            deleteIcon.visibility = View.GONE
        }


        photoImageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTranslateX = imageView.translationX
                    initialTranslateY = imageView.translationY
                    isScrolling = true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isZoomedIn && isScrolling) {
                        val deltaX = event.x - initialTouchX
                        val deltaY = event.y - initialTouchY

                        val newTranslateX = initialTranslateX + deltaX
                        val newTranslateY = initialTranslateY + deltaY

                        imageView.translationX = newTranslateX
                        imageView.translationY = newTranslateY
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isScrolling = false
                }
            }
            true
        }

    }


    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        val inflater: MenuInflater = popupMenu.menuInflater
        inflater.inflate(R.menu.menu_main, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_item_liked_photos -> {
                    Toast.makeText(this, "showing liked photos", Toast.LENGTH_SHORT).show()
                    // Handle Liked Photos click
                    // Start the LikedPhotosActivity
                    startActivity(Intent(this, LikedPhotosActivity::class.java))
                    true
                }
                R.id.menu_item_update_password -> {
                    // Handle Update Password click
                    // Start the UpdatePasswordActivity
                    startActivity(Intent(this, UpdatePasswordActivity::class.java))
                    true
                }
                R.id.menu_item_logout -> {
                    signOut()
                    // Handle Logout click
                    // Implement logout logic here
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun signOut() {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signOut()

        // Update the session manager to mark the user as logged out
        sessionManager.logout()

        // Redirect to the LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun initializeLikedPhotosSet() {
        val sharedPreferences = getSharedPreferences("LikedPhotos", MODE_PRIVATE)
        val likedPhotosSet = sharedPreferences.getStringSet("likedPhotos", HashSet()) ?: HashSet()

        // You can store this set in a global variable or ViewModel for easy access in other parts of your app
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("CacheClearing", "onDestroy called")

        // Run Glide.clearDiskCache() on a background thread
        Thread {
            Glide.get(this).clearDiskCache()
        }.start()

        Glide.get(this).clearMemory()
    }


    private fun hideLogos() {
        shareIcon.visibility = View.GONE
        deleteIcon.visibility = View.GONE
    }


    private fun deleteImage() {
        if (currentIndex != -1) {
            val imagePath = imageList[currentIndex]
            val file = File(imagePath)

            if (file.exists()) {
                if (true) {
                    // Image deleted successfully

                    // Remove the image path from the list
                    imageList.removeAt(currentIndex)

                    // Remove the current index from the stack
                    if (visitedPhotosStack.isNotEmpty()) {
                        visitedPhotosStack.pop()

                    }

                    // Update the current index and display the next image
                    if (imageList.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % imageList.size
                        val nextImagePath = imageList[currentIndex]
                        Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show()
                        showRandomImage()
                    }
                } else {
                    Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }


    private fun goBack() {
        if (visitedPhotosStack.isNotEmpty()) {
            val previousIndex = visitedPhotosStack.pop()
            val imagePath = imageList[previousIndex]
            imageView.scaleX = initialScaleX
            imageView.scaleY = initialScaleY
            isZoomedIn = false
            hideLogos()
            displayImage(imagePath)
            currentIndex = previousIndex
        } else {
            Toast.makeText(this, "No previous photo", Toast.LENGTH_SHORT).show()
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_VELOCITY_THRESHOLD = 1000

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffX > 0) {
                        // Swipe right, perform back button functionality
                        goBack()
                    } else {
                        // Swipe left, show a random image
                        showRandomImage()
                        // Hide the logos after a new image is generated
                        hideLogos()
                    }
                }
            } else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                    Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffY < 0) {
                        // Swipe up, show share icon
                        shareIcon.visibility = View.VISIBLE
                        // Hide the delete icon
                        deleteIcon.visibility = View.GONE
                    } else {
                        // Swipe down, show delete icon
                        deleteIcon.visibility = View.VISIBLE
                        // Hide the share icon
                        shareIcon.visibility = View.GONE
                    }
                }
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }


        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Show the heart icon
            heartIcon.visibility = View.VISIBLE

            // Save the liked photo
            if (currentIndex != -1) {
                val imagePath = imageList[currentIndex]

                // Save the liked photo path to SharedPreferences
                saveLikedPhoto(imagePath)
            }

            // Hide the heart icon after 1 second
            android.os.Handler(mainLooper).postDelayed({
                heartIcon.visibility = View.GONE
            }, 1000)

            return true
        }
    }

    private fun saveLikedPhoto(imagePath: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val databaseRef = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser

        user?.let { currentUser ->
            // Generate a unique filename for the image in Firebase Storage
            val filename = UUID.randomUUID().toString()

            val imageFile = File(imagePath)
            val imageUri: Uri = Uri.fromFile(imageFile)

            // Upload the image to Firebase Storage
            val likedPhotosFolder = "users/${currentUser.uid}/liked_images"
            val imageStorageRef = storageRef.child("$likedPhotosFolder/$filename")
            val uploadTask = imageStorageRef.putFile(imageUri)

            uploadTask.addOnSuccessListener { _ ->
                // Image upload success, get the download URL
                imageStorageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Save the download URL to Firebase Realtime Database
                    val likedPhoto = LikedPhoto(downloadUri.toString())

                    // Push the liked photo to the user's node in the database
                    val userPhotosRef = databaseRef.child("user_photos").child(currentUser.uid).push()
                    userPhotosRef.setValue(likedPhoto)

                    // Show a toast message
                    Toast.makeText(this@MainActivity, "Added to Liked Photos", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                // Handle any errors that occurred during the upload
                Log.e("FirebaseUpload", "Failed to upload image: ${exception.message}", exception)
                Toast.makeText(
                    this@MainActivity,
                    "Failed to upload image: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }




    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.1f, 10.0f)

            // Apply scaling to the ImageView
            imageView.scaleX = scaleFactor
            imageView.scaleY = scaleFactor

            return true
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            scaleGestureDetector.onTouchEvent(event)

            // Handle the double-tap gesture here
            if (event.action == MotionEvent.ACTION_UP) {
                val currentTime = System.currentTimeMillis()
                val timeDifference = currentTime - lastTapTime
                lastTapTime = currentTime

                if (timeDifference < DOUBLE_TAP_TIME_DELTA) {
                    // Double tap detected, show heart icon and perform actions
                    heartIcon.visibility = View.VISIBLE

                    android.os.Handler(mainLooper).postDelayed({
                        heartIcon.visibility = View.GONE
                    }, 1000)


                    // Save the liked photo and perform any other actions you need


                }
            }
        }
        return super.onTouchEvent(event)
    }


    private fun showRandomImage() {
        if (imageList.isNotEmpty()) {
            if (currentIndex != -1) {
                visitedPhotosStack.push(currentIndex)
            }

            val randomIndex = imageList.indices.random()
            val imagePath = imageList[randomIndex]

            // Reset zoom state
            imageView.scaleX = initialScaleX
            imageView.scaleY = initialScaleY
            isZoomedIn = false

            displayImage(imagePath)
            currentIndex = randomIndex
        }
    }

    private fun displayImage(imagePath: String) {
        currentImagePath = imagePath
        photoImageView.rotation = 0f
        Glide.with(this)
            .load(imagePath)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(photoImageView)

        // Get the date and location for the image
        val file = File(imagePath)
        val dateTaken = Date(file.lastModified())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()) // Format for the day

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val formattedDate = "Date: ${dateFormat.format(dateTaken)}"
        val formattedDay = "Day: ${dayFormat.format(dateTaken)}" // Formatted day

        val formattedTime = "Time: ${timeFormat.format(dateTaken)}"

        // Display the formatted date and time in your TextView
        dateTextView.text = "$formattedDate\n$formattedDay\n$formattedTime"
    }

    private fun sharePhoto() {
        if (currentIndex != -1) {
            val imagePath = imageList[currentIndex]
            val imageFile = File(imagePath)

            val imageUri: Uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {

                type = "image/jpg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Toast.makeText(this, "Image sharing", Toast.LENGTH_SHORT).show()
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPhotosFromGallery() {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use { fcursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(columnIndex)
                val file = File(imagePath)
                if (file.exists()) {
                    imageList.add(file.absolutePath)
                }
            }
        }

        cursor?.close()

        if (imageList.isNotEmpty()) {
            val randomIndex = imageList.indices.random()
            val randomImagePath = imageList[randomIndex]
            displayImage(randomImagePath)
            currentIndex = randomIndex
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadPhotosFromGallery()
            }
        }
    }

    companion object {
        private const val REQUEST_PERMISSION = 1
    }

}