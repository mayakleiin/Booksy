package com.example.booksy.ui.profile

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import coil.load
import com.example.booksy.databinding.FragmentEditProfileDialogBinding
import com.example.booksy.viewmodel.UserProfileViewModel
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentEditProfileDialogBinding
    private val viewModel: UserProfileViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                binding.profileImage.load(selectedImageUri)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentEditProfileDialogBinding.inflate(LayoutInflater.from(context))

        val user = viewModel.user.value
        binding.nameEditText.setText(user?.name)
        binding.profileImage.load(user?.imageUrl)

        binding.profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.loader.isVisible = loading
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val newName = binding.nameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    if (selectedImageUri != null) {
                        uploadImageToFirebase(newName, selectedImageUri!!)
                    } else {
                        viewModel.updateUserProfile(newName, user?.imageUrl)
                    }
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun uploadImageToFirebase(name: String, imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("profile_images/${UUID.randomUUID()}.jpg")

        imageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                imageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                viewModel.updateUserProfile(name, uri.toString())
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }
}
