package com.example.campreservation

import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide

enum class UserRole { ENTRANT, OWNER, ADMIN}
abstract class Screen : Fragment() {

    abstract val role: UserRole

    abstract fun show()

    abstract fun refresh()

    fun navigate (destinationId: Int, args: Bundle? = null) {
        findNavController().navigate(destinationId, args)
    }

    fun showImage(target: ImageView, url : String?) {
        Glide.with(target)
            .load(url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(target)
    }
}
