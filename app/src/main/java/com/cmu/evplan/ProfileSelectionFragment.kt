package com.cmu.evplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cmu.evplan.R
import com.cmu.evplan.RoutingViewModel
import com.cmu.evplan.databinding.FragmentProfileSelectionBinding

class ProfileSelectionFragment : Fragment(){
    private var _binding: FragmentProfileSelectionBinding? = null

    private val binding get() = _binding!!

    private val viewModel: RoutingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileSelectionBinding.inflate(inflater, container, false)
        val view = binding.root

        setupBrandSpinner();
        return view
    }

    private fun setupBrandSpinner() {
        var list_of_brand = arrayOf("Item 1", "Item 2", "Item 3")
        val spin = binding.editBrandInfoSpinner
        val arrayAdapter =
            view?.let { ArrayAdapter(it.context, android.R.layout.simple_spinner_item, list_of_brand) }
        spin.adapter = arrayAdapter
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                viewModel.setVehicleModel(list_of_brand[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                viewModel.setVehicleModel("Tesla")
            }
        }
    }


}
