package com.example.booksy.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.booksy.R
import com.example.booksy.databinding.FragmentAddBookBinding
import com.example.booksy.model.Book
import com.example.booksy.model.BookStatus
import com.example.booksy.model.Genre
import com.example.booksy.model.Language
import com.example.booksy.viewmodel.OpenLibraryViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddBookFragment : Fragment() {

    private lateinit var binding: FragmentAddBookBinding
    private lateinit var loadingOverlay: FrameLayout
    private val viewModel: OpenLibraryViewModel by viewModels()

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var imageUri: Uri? = null
    private val selectedGenres = mutableListOf<Genre>()
    private val selectedLanguages = mutableListOf<Language>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser === null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        loadingOverlay = view.findViewById(R.id.loadingOverlay)

        viewModel.isLoading.observe(viewLifecycleOwner) {
            loadingOverlay.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.selectedBook.observe(viewLifecycleOwner) { book ->
            book?.let {
                binding.titleEditText.setText(it.title)
                binding.authorEditText.setText(it.getAuthor())
                binding.descriptionEditText.setText(it.getDescription())
                binding.pagesEditText.setText(it.number_of_pages_median?.toString() ?: "")
                binding.bookImageView.load(it.getCoverUrl())
                Toast.makeText(requireContext(), getString(R.string.toast_book_filled), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.noResultFound.observe(viewLifecycleOwner) { noResult ->
            if (noResult) Toast.makeText(requireContext(), getString(R.string.toast_no_book_found), Toast.LENGTH_SHORT).show()
        }

        binding.searchButton.setOnClickListener {
            val query = binding.searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchBook(query)
            }
        }

        binding.shareLocationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.addressEditText.isVisible = !isChecked
            if (isChecked) requestLocation()
        }

        binding.saveBookButton.setOnClickListener {
            uploadBook()
        }

        Genre.values().forEach { genre ->
            val chip = createChip(genre.displayName, selectedGenres.contains(genre))
            chip.setOnClickListener {
                if (selectedGenres.contains(genre)) {
                    selectedGenres.remove(genre)
                } else {
                    selectedGenres.add(genre)
                }
                updateChips(binding.genreChipGroup, selectedGenres)
            }
            binding.genreChipGroup.addView(chip)
        }

        Language.values().forEach { lang ->
            val chip = createChip(lang.displayName, selectedLanguages.contains(lang))
            chip.setOnClickListener {
                if (selectedLanguages.contains(lang)) {
                    selectedLanguages.remove(lang)
                } else {
                    selectedLanguages.add(lang)
                }
                updateChips(binding.languageChipGroup, selectedLanguages)
            }
            binding.languageChipGroup.addView(chip)
        }
    }

    private fun createChip(text: String, isChecked: Boolean): com.google.android.material.chip.Chip {
        return com.google.android.material.chip.Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            this.isChecked = isChecked
        }
    }

    private fun updateChips(group: ViewGroup, selectedList: List<Enum<*>>) {
        group.removeAllViews()
        selectedList.forEach { item ->
            val chip = createChip(
                when (item) {
                    is Genre -> item.displayName
                    is Language -> item.displayName
                    else -> item.name
                },
                true
            )
            group.addView(chip)
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
                Toast.makeText(requireContext(), getString(R.string.toast_location_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadBook() {
        val title = binding.titleEditText.text.toString().trim()
        val author = binding.authorEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val pages = binding.pagesEditText.text.toString().toIntOrNull() ?: 0
        val useCurrentLocation = binding.shareLocationCheckbox.isChecked
        val addressText = binding.addressEditText.text.toString().trim()

        if (title.isEmpty() || author.isEmpty() || description.isEmpty()
            || selectedGenres.isEmpty() || selectedLanguages.isEmpty()
            || (!useCurrentLocation && addressText.isEmpty())
        ) {
            Toast.makeText(requireContext(), getString(R.string.toast_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        loadingOverlay.visibility = View.VISIBLE

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
                genres = selectedGenres,
                description = description,
                languages = selectedLanguages,
                pages = pages,
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
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.toast_book_saved), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.toast_save_failed), Toast.LENGTH_SHORT).show()
                }
        }

        imageUri?.let { uri ->
            val storageRef = FirebaseStorage.getInstance().getReference("book_images/$bookId.jpg")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveBookToFirestore(downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.toast_image_failed), Toast.LENGTH_SHORT).show()
                }
        } ?: saveBookToFirestore("")
    }
}