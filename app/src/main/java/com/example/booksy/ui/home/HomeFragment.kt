package com.example.booksy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.databinding.FragmentHomeBinding
import com.example.booksy.viewmodel.HomeViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var bookAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setupRecyclerView()
        observeViewModel()
        setupToggle()

        homeViewModel.loadBooks()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(emptyList())
        binding.booksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.booksRecyclerView.adapter = bookAdapter
    }

    private fun observeViewModel() {
        homeViewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.updateBooks(books)

            googleMap?.let { map ->
                map.clear()
                books.forEach { book ->
                    val lat = book.lat
                    val lng = book.lng
                    if (lat != null && lng != null) {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title(book.title)
                        )
                    }
                }
            }
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }


    private fun setupToggle() {
        binding.toggleViewButton.setOnClickListener {
            val isMapVisible = binding.mapView.visibility == View.VISIBLE
            binding.mapView.visibility = if (isMapVisible) View.GONE else View.VISIBLE
            binding.booksRecyclerView.visibility = if (isMapVisible) View.VISIBLE else View.GONE
        }
    }

    override fun onMapReady(map: GoogleMap) {
        MapsInitializer.initialize(requireContext())
        googleMap = map
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mapView.onDestroy()
    }
}
