package com.example.booksy.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.R
import com.example.booksy.databinding.FragmentHomeBinding
import com.example.booksy.viewmodel.HomeViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var nearbyBooksAdapter: NearbyBooksAdapter

    companion object {
        private const val LOCATION_PERMISSION_CODE = 1001
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingOverlay = binding.loadingOverlay

        setupMap()
        setupRecyclerView()
        observeViewModel()
        requestLocationPermission()

        binding.mapView.visibility = View.GONE
        binding.nearbyBooksRecyclerView.visibility = View.VISIBLE
        binding.toggleViewButton.setImageResource(R.drawable.ic_map)

        binding.toggleViewButton.setOnClickListener {
            val isMapVisible = binding.mapView.visibility == View.VISIBLE
            binding.mapView.visibility = if (isMapVisible) View.GONE else View.VISIBLE
            binding.nearbyBooksRecyclerView.visibility = if (isMapVisible) View.VISIBLE else View.GONE
            binding.toggleViewButton.setImageResource(if (isMapVisible) R.drawable.ic_map else R.drawable.ic_list)
        }
    }

    private fun setupMap() {
        mapView = binding.mapView
        mapView.onCreate(null)
        mapView.getMapAsync(this)
    }

    private fun setupRecyclerView() {
        nearbyBooksAdapter = NearbyBooksAdapter(emptyList())
        binding.nearbyBooksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = nearbyBooksAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.books.observe(viewLifecycleOwner) { books ->
            googleMap?.clear()
            books.forEach { book ->
                if (book.lat != null && book.lng != null) {
                    val marker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(book.lat, book.lng))
                            .title(book.title)
                            .snippet(getString(R.string.by_author_format, book.author))
                    )
                    marker?.tag = book.id
                }
            }
        }

        viewModel.nearbyBooks.observe(viewLifecycleOwner) { nearby ->
            nearbyBooksAdapter.updateBooks(nearby)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onMapReady(map: GoogleMap) {
        MapsInitializer.initialize(requireContext())
        googleMap = map
        setupMarkerClickListener()
        viewModel.loadBooks()
    }

    private fun setupMarkerClickListener() {
        googleMap?.setOnInfoWindowClickListener { marker ->
            val bookId = marker.tag as? String
            bookId?.let {
                val action = HomeFragmentDirections.actionHomeFragmentToBookDetailFragment(it)
                findNavController().navigate(action)
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val request = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        viewModel.updateCurrentLocation(location)
                        val latLng = LatLng(location.latitude, location.longitude)
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                    }
                }
            },
            Looper.getMainLooper()
        )
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
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
