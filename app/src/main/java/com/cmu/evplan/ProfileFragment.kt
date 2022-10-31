package com.cmu.evplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cmu.evplan.databinding.FragmentProfileBinding


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: RoutingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        if (viewModel.getBattery() == null) {
            binding.battery.setText("100%")
            viewModel.setBattery(100.00)    // set default value for further calc
        } else {
            binding.battery.setText("${viewModel.getBattery()}%")
        }

        binding.mileage.setText("${viewModel.calRemainRange()} miles")

        val connectorType = binding.chargerType.getText().toString()
        viewModel.setConnectorType(connectorType)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}