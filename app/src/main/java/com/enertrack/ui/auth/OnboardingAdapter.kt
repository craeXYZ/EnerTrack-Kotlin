package com.enertrack.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.enertrack.databinding.ItemOnboardingSlideBinding

// Data class
data class OnboardingSlide(val iconRes: Int, val title: String, val description: String)

class OnboardingAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingSlideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount() = slides.size

    class OnboardingViewHolder(private val binding: ItemOnboardingSlideBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(slide: OnboardingSlide) {
            binding.ivSlideIcon.setImageResource(slide.iconRes)
            binding.tvSlideTitle.text = slide.title
            binding.tvSlideDescription.text = slide.description
        }
    }
}