package com.example.booksy.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.booksy.R
import com.example.booksy.databinding.FragmentAddBookBinding
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*

class AddBookFragment : Fragment() {

    private lateinit var binding: FragmentAddBookBinding
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var imageUri: Uri? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.shareLocationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.addressEditText.isVisible = !isChecked
            if (isChecked) requestLocation()
        }

        binding.saveBookButton.setOnClickListener {
            uploadBook()
        }
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(requireContext())
        fused.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
            } else {
                Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        }
    }

    private fun uploadBook() {
        val title = binding.titleEditText.text.toString().trim()
        val author = binding.authorEditText.text.toString().trim()
        val genre = binding.genreEditText.text.toString().trim()
        val useCurrentLocation = binding.shareLocationCheckbox.isChecked
        val addressText = binding.addressEditText.text.toString().trim()


        if (title.isEmpty() || author.isEmpty() || genre.isEmpty() || (!useCurrentLocation && addressText.isEmpty())) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!useCurrentLocation) {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addressList = geocoder.getFromLocationName(addressText, 1)
            if (!addressList.isNullOrEmpty()) {
                currentLat = addressList[0].latitude
                currentLng = addressList[0].longitude
            }
        }

        val bookId = UUID.randomUUID().toString()

        val saveBookToFirestore: (String) -> Unit = { imageUrl ->
            val book = Book(
                id = bookId,
                title = title,
                author = author,
                genre = genre,
                imageUrl = imageUrl,
                status = BookStatus.AVAILABLE,
                ownerId = FirebaseAuth.getInstance().uid ?: "user1",
                lat = currentLat ?: 0.0,
                lng = currentLng ?: 0.0
            )

            FirebaseFirestore.getInstance()
                .collection("books")
                .document(bookId)
                .set(book)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Book saved successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save book", Toast.LENGTH_SHORT).show()
                }
        }


        if (imageUri != null) {
            val storageRef = FirebaseStorage.getInstance().getReference("book_images/$bookId.jpg")
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveBookToFirestore(downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        } else {

            saveBookToFirestore("")
        }
    }


}
