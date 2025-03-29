package com.example.booksy.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booksy.R
import com.example.booksy.databinding.FragmentHomeBinding
import com.example.booksy.viewmodel.HomeViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val defaultNearbyDistanceMeters = 2.0f
    private val currentUserLat = 32.08
    private val currentUserLng = 34.78

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

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setupRecyclerView()
        setupNearbyRecyclerView()
        observeViewModel()
        setupToggle()

        requestCurrentLocation()
        homeViewModel.loadBooks()
    }

    private fun requestCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    homeViewModel.updateCurrentLocation(location)
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }
    }


    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            books = emptyList(),
            onItemClick = { book ->
                val action = HomeFragmentDirections.actionHomeFragmentToBookDetailFragment(book.id)
                findNavController().navigate(action)
            },
            onEditClick = null
        )

        binding.booksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.booksRecyclerView.adapter = bookAdapter
    }

    private fun setupNearbyRecyclerView() {
        val nearbyAdapter = NearbyBooksAdapter(emptyList())
        binding.nearbyBooksRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.nearbyBooksRecyclerView.adapter = nearbyAdapter

        homeViewModel.nearbyBooks.observe(viewLifecycleOwner) { nearby ->
            nearbyAdapter.updateBooks(nearby)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        homeViewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.updateBooks(books)

            val nearbyBooks = books.filter {
                it.lat != null && it.lng != null &&
                        calculateDistance(it.lat, it.lng) <= defaultNearbyDistanceMeters
            }
            binding.nearbyCountTextView.text = "Books nearby: ${nearbyBooks.size}"

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

    private fun calculateDistance(lat: Double?, lng: Double?): Float {
        val currentLat = currentUserLat ?: return Float.MAX_VALUE
        val currentLng = currentUserLng ?: return Float.MAX_VALUE

        val results = FloatArray(1)
        Location.distanceBetween(currentLat, currentLng, lat!!, lng!!, results)
        return results[0]
    }



    private fun setupToggle() {
        binding.toggleViewButton.setOnClickListener {
            val isMapVisible = binding.mapView.visibility == View.VISIBLE
            binding.mapView.visibility = if (isMapVisible) View.GONE else View.VISIBLE
            binding.booksRecyclerView.visibility = if (isMapVisible) View.VISIBLE else View.GONE


            binding.toggleViewButton.setImageResource(
                if (isMapVisible) R.drawable.ic_map else R.drawable.ic_list
            )
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
