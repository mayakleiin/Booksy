package com.example.booksy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.booksy.databinding.FragmentFilterBottomSheetBinding
import com.example.booksy.model.BookFilters
import com.example.booksy.model.Genre
import com.example.booksy.model.Language
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class FilterBottomSheetFragment(
    private val currentFilters: BookFilters,
    private val onApply: (BookFilters) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentFilterBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSlider()
        setupChips()
        binding.applyButton.setOnClickListener {
            val selectedGenres = Genre.values().filterIndexed { index, _ ->
                binding.genreChipGroup.checkedChipIds.contains(index)
            }

            val selectedLanguages = Language.values().filterIndexed { index, _ ->
                binding.languageChipGroup.checkedChipIds.contains(index)
            }

            val filters = BookFilters(
                maxDistanceKm = binding.distanceSlider.value,
                selectedGenres = selectedGenres,
                selectedLanguages = selectedLanguages
            )
            onApply(filters)
            dismiss()
        }
    }

    private fun setupSlider() {
        binding.distanceSlider.valueFrom = 1f
        binding.distanceSlider.valueTo = 2000f

        val defaultValue = currentFilters.maxDistanceKm.coerceIn(1f, binding.distanceSlider.valueTo)
        binding.distanceSlider.value = defaultValue
        binding.distanceValueText.text = getString(com.example.booksy.R.string.kilometers_format, defaultValue.toInt())

        binding.distanceSlider.addOnChangeListener { _, value, _ ->
            binding.distanceValueText.text = getString(com.example.booksy.R.string.kilometers_format, value.toInt())
        }
    }

    private fun setupChips() {
        Genre.values().forEachIndexed { index, genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.name
                isCheckable = true
                id = index
                isChecked = currentFilters.selectedGenres.contains(genre)
            }
            binding.genreChipGroup.addView(chip)
        }

        Language.values().forEachIndexed { index, language ->
            val chip = Chip(requireContext()).apply {
                text = language.name
                isCheckable = true
                id = index
                isChecked = currentFilters.selectedLanguages.contains(language)
            }
            binding.languageChipGroup.addView(chip)
        }
    }
}
