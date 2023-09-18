package com.example.relive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

interface OnItemClickListener {
    fun onItemClick(imagePath: String)
}

class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private var photoUrls: List<String> = emptyList()
    private var itemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val imageUrl = photoUrls[position]
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(ContextCompat.getDrawable(holder.itemView.context, R.drawable.placeholder_image))
            .into(holder.imageView)
        holder.imageView.setOnClickListener {
            itemClickListener?.onItemClick(imageUrl)
        }
    }

    override fun getItemCount(): Int {
        return photoUrls.size
    }

    fun setData(urls: List<String>) {
        photoUrls = urls
        notifyDataSetChanged()
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
    }
}
