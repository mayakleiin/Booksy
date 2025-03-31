package com.example.booksy.ui.profile

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.booksy.databinding.FragmentEditProfileDialogBinding
import com.example.booksy.viewmodel.UserProfileViewModel
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*
import com.example.booksy.R

class EditProfileDialogFragment : DialogFragment() {

    private var _binding: FragmentEditProfileDialogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserProfileViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    var onProfileUpdated: (() -> Unit)? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                Picasso.get()
                    .load(selectedImageUri)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(binding.profileImage)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = Dialog(requireContext(), R.style.EditProfileDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = viewModel.user.value
        binding.nameEditText.setText(user?.name)

        binding.profileImage.setImageResource(R.drawable.default_profile)

        if (!user?.imageUrl.isNullOrBlank()) {
            Picasso.get()
                .load(user?.imageUrl)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(binding.profileImage)
        }

        binding.profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.loader.isVisible = loading
        }

        binding.saveButton.setOnClickListener {
            val newName = binding.nameEditText.text.toString().trim()
            if (newName.isNotEmpty()) {
                if (selectedImageUri != null) {
                    uploadImageToFirebase(newName, selectedImageUri!!)
                } else {
                    viewModel.updateUserProfile(newName, user?.imageUrl)
                    onProfileUpdated?.invoke()
                    dismiss()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_name_empty), Toast.LENGTH_SHORT).show()
            }
        }


        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        try {
            (parentFragment?.view?.findViewById<View>(R.id.addBookButton))?.visibility = View.GONE
        } catch (e: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            (parentFragment?.view?.findViewById<View>(R.id.addBookButton))?.visibility = View.VISIBLE
        } catch (e: Exception) {
        }

        _binding = null
    }

    private fun uploadImageToFirebase(name: String, imageUri: Uri) {
        viewModel.setIsLoading(true)
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("profile_images/${UUID.randomUUID()}.jpg")

        imageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                imageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                viewModel.updateUserProfile(name, uri.toString())
                onProfileUpdated?.invoke()
                dismiss()
            }
            .addOnFailureListener {
                viewModel.setIsLoading(false)
                Toast.makeText(requireContext(), getString(R.string.toast_image_upload_failed), Toast.LENGTH_SHORT).show()
            }
    }

}