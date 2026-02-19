package com.order.storecontroller.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.order.storecontroller.R
import com.order.storecontroller.databinding.FragmentDashboardBinding
import com.order.storecontroller.util.LocaleHelper

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        val current = LocaleHelper.getLocale(ctx)
        binding.languageGroup.check(
            if (current == "ar") R.id.lang_ar else R.id.lang_en
        )
        binding.langAr.setOnClickListener { setLanguage("ar") }
        binding.langEn.setOnClickListener { setLanguage("en") }
    }

    private fun setLanguage(lang: String) {
        if (LocaleHelper.getLocale(requireContext()) == lang) return
        LocaleHelper.setLocale(requireContext(), lang)
        requireActivity().recreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
