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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.R
import com.example.booksy.databinding.FragmentHomeBinding
import com.example.booksy.viewmodel.HomeViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var nearbyBooksAdapter: NearbyBooksAdapter

    companion object {
        private const val LOCATION_PERMISSION_CODE = 1001
        private const val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingOverlay = binding.loadingOverlay

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }

        mapView = binding.mapView
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        setupRecyclerView()
        observeViewModel()
        requestLocationPermission()
        setupProfileButton()
        setupFilterButton()

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

    private fun setupProfileButton() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        binding.profileButton.setOnClickListener {
            if (currentUser != null) {
                findNavController().navigate(R.id.userProfileFragment)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            val filterBottomSheet = FilterBottomSheetFragment(
                currentFilters = viewModel.getCurrentFilters(),
                onApply = { filters -> viewModel.applyFilters(filters) }
            )
            filterBottomSheet.show(childFragmentManager, "filterBottomSheet")
        }
    }

    private fun setupRecyclerView() {
        nearbyBooksAdapter = NearbyBooksAdapter(emptyList()) { book ->
            val action = HomeFragmentDirections.actionHomeFragmentToBookDetailFragment(book.id)
            findNavController().navigate(action)
        }

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
            binding.nearbyCountTextView.text = getString(R.string.books_nearby)
                .replace("0", nearby.size.toString())
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
        moveToUserLocation()
    }

    @SuppressLint("PotentialBehaviorOverride")
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
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
                        moveToLocation(location)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun moveToUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    moveToLocation(it)
                }
            }
        }
    }

    private fun moveToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY) ?: Bundle()
        mapView.onSaveInstanceState(mapViewBundle)
        outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        viewModel.loadBooks()
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
