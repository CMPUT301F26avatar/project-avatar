package com.example.campreservation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.campreservation.Campsite
import com.example.campreservation.CampsiteRepository
import com.example.campreservation.Entrant
import com.example.campreservation.R
import com.example.campreservation.databinding.FragmentEntrantViewBinding
import com.example.campreservation.databinding.ItemCampsiteBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset

class EntrantView : Screen() {
    override val role = UserRole.ENTRANT

    private var _binding : FragmentEntrantViewBinding? = null
    private var binding get() = _binding!!

    private val entrant: Entrant get() - EntrantSession.current

    private val repository: CampsiteRepository by lazy {
        TODO("Get the real campsite repo")
    }

    private var filter = SearchFilter()

    private val adapter = CampsiteAdapter(
        onClick = ::openCampView,
        loadImage = { view, url -> showImage(view, url) }
    )

    #illl continue here

}