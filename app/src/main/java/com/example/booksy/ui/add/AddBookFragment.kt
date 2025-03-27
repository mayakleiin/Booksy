package com.example.booksy.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.booksy.databinding.FragmentAddBookBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale
import java.util.UUID
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus.AVAILABLE


class AddBookFragment : Fragment() {

    private lateinit var binding: FragmentAddBookBinding
    private var selectedImageUri: Uri? = null
    private var currentLatLng: LatLng? = null

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val storage = FirebaseStorage.getInstance().reference

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                binding.bookImageView.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.selectImageButton.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.shareLocationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.addressEditText.isVisible = !isChecked
            if (isChecked) requestLocation()
        }



        binding.saveBookButton.setOnClickListener {
            uploadBook()
        }
    }

    private fun requestLocation() {
        val locationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun uploadBook() {
        val title = binding.titleEditText.text.toString().trim()
        val author = binding.authorEditText.text.toString().trim()
        val genre = binding.genreEditText.text.toString().trim()
        val shareLocation = binding.shareLocationCheckbox.isChecked
        val manualAddress = binding.addressEditText.text.toString().trim()

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val bookId = UUID.randomUUID().toString()
        val imageRef = storage.child("books/$bookId.jpg")

        imageRef.putFile(selectedImageUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                imageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                val latLng = if (shareLocation) {
                    currentLatLng
                } else {
                    geocodeAddress(manualAddress)
                }

                latLng?.let { loc ->
                    val book = Book(
                        id = bookId,
                        title = title,
                        author = author,
                        genre = genre,
                        imageUrl = uri.toString(),
                        status = AVAILABLE,
                        ownerId = firebaseAuth.currentUser?.uid ?: "",
                        lat = loc.latitude,
                        lng = loc.longitude
                    )

                    firestore.collection("books").document(bookId).set(book)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Book added!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to save book", Toast.LENGTH_SHORT).show()
                        }
                } ?: Toast.makeText(context, "Could not get location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun geocodeAddress(address: String): LatLng? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val result = geocoder.getFromLocationName(address, 1)
            if (result?.isNotEmpty() == true) {
                val loc = result[0]
                LatLng(loc.latitude, loc.longitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
