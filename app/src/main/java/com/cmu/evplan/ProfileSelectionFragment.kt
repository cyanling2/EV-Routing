package com.cmu.evplan
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cmu.evplan.databinding.FragmentProfileSelectionBinding

class ProfileSelectionFragment : Fragment(){
    private var _binding: FragmentProfileSelectionBinding? = null

    private val binding get() = _binding!!

    private val viewModel: RoutingViewModel by activityViewModels()

    private var saved: Boolean = false
    private var brandSelected: String = ""
    private var modelSelected: String = ""
    private var connectorSelected: String = ""

    /**
     * Initializes and creates view for the profile selection fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileSelectionBinding.inflate(inflater, container, false)
        val view = binding.root

        // select brand
        val list_of_brand = arrayOf("Select Brand","Tesla", "Toyota","Hyundai","Nissan","KIA")
        val spin_brand = view?.findViewById<Spinner>(R.id.editBrandInfoSpinner)

        var spin_model = view?.findViewById<Spinner>(R.id.editModelInfoSpinner)

        val arrayAdapter =
            view?.let { ArrayAdapter(
                it.context,
                android.R.layout.simple_spinner_dropdown_item,
                list_of_brand
            ) }
        if (spin_brand != null) {
            spin_brand.adapter = arrayAdapter
        }
        if (spin_brand != null) {
            spin_brand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    var list_of_model = arrayOf("")
                    brandSelected = list_of_brand[position]

                    if (brandSelected == null) {
                        list_of_model == arrayOf("")
                    }

                    else if (brandSelected == "Select Brand"){
                        list_of_model == arrayOf("")
                    }

                    else if (brandSelected == "Tesla") {
                        list_of_model = arrayOf("Model3","ModelS","ModelX","ModelY")
                    } else if (brandSelected == "Toyota") {
                        list_of_model = arrayOf("bZ4X","Hybrid")
                    } else if (brandSelected == "Hyundai") {
                        list_of_model = arrayOf("IONIQ","Kona")
                    } else if (brandSelected == "Nissan") {
                        list_of_model = arrayOf("Nissan Leaf")
                    } else if (brandSelected == "KIA") {
                        list_of_model = arrayOf("Soul Electric","Niro Electric")
                    }

                    println("list of model:" + list_of_model[0])
                    var ad: ArrayAdapter<*>? = view?.let {
                        ArrayAdapter<Any?>(
                            it.context,
                            android.R.layout.simple_spinner_dropdown_item,
                            list_of_model
                        )
                    }

                    if (spin_model != null) {
                        spin_model.adapter = ad
                    }
                    if (spin_model != null) {
                        spin_model.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                if (modelSelected == "Select Model"){
                                    modelSelected = ""
                                    return
                                }
                                modelSelected = list_of_model[position]
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                modelSelected = ""
                            }
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    brandSelected = "Tesla"
                }
            }
        }


        var list_of_connector = arrayOf("all", "NEMA1450", "NEMA515", "NEMA520","J1772","J1772COMBO","CHADEMO","TESLA")
        val spin_connctor = view?.findViewById<Spinner>(R.id.editConnectorInfoSpinner)
        val arrayAdapter3 =
            view?.let { ArrayAdapter(
                it.context,
                android.R.layout.simple_spinner_dropdown_item,
                list_of_connector
            ) }
        if (spin_connctor != null) {
            spin_connctor.adapter = arrayAdapter3
        }
        if (spin_connctor != null) {
            spin_connctor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    connectorSelected = list_of_connector[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    connectorSelected = "all"
                }
            }
        }

        _binding!!.btnSave.setOnClickListener {
            saved = true
            if (brandSelected != "Select Brand"){
                viewModel.setVehicleBrand(brandSelected)
                viewModel.setVehicleModel(modelSelected)
            }
            viewModel.setConnectorType(connectorSelected)

            binding.btnSave.setText("Saved")
            binding.btnSave.setBackgroundColor(Color.GRAY)
        }

        _binding!!.txtOneHundredFortyThree.setOnClickListener {
            findNavController().navigate(R.id.action_profileSelectionFragment_to_mapFragment)
        }

        return view
    }

    private fun setupBrandSpinner(view: View) {
        val list_of_brand = arrayOf("Tesla", "Toyota","Hyundai","Nissan","KIA")
        val spin = view?.findViewById<Spinner>(R.id.editBrandInfoSpinner)
        val arrayAdapter =
            view?.let { ArrayAdapter(
                it.context,
                android.R.layout.simple_spinner_dropdown_item,
                list_of_brand
            ) }
        if (spin != null) {
            spin.adapter = arrayAdapter
        }
        if (spin != null) {
            spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    brandSelected = list_of_brand[position]
                    setupModelSpinner(view,brandSelected)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    brandSelected = ""
                }
            }
        }
    }

    private fun setupModelSpinner(view: View, brandSelected: String) {
        var list_of_model = arrayOf("")
        if (brandSelected == "Tesla") {
            list_of_model = arrayOf("Model3","ModelS","ModelX","ModelY")
        } else if (brandSelected == "Toyata") {
            list_of_model = arrayOf("bZ4X","Hybrid")
        } else if (brandSelected == "Hyundai") {
            list_of_model = arrayOf("IONIQ","Kona")
        } else if (brandSelected == "Nissan") {
            list_of_model = arrayOf("Nissan Leaf")
        } else if (brandSelected == "KIA") {
            list_of_model = arrayOf("Soul Electric","Niro Electric")
        }

        val spin = view?.findViewById<Spinner>(R.id.editModelInfoSpinner)
        val arrayAdapter =
            view?.let { ArrayAdapter(
                it.context,
                android.R.layout.simple_spinner_dropdown_item,
                list_of_model
            ) }
        if (spin != null) {
            spin.adapter = arrayAdapter
        }
        if (spin != null) {
            spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    modelSelected = list_of_model[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    modelSelected = ""
                }
            }
        }
    }

    private fun setupConnectorSpinner(view: View) {
        var list_of_connector = arrayOf("NEMA14-50", "NEMA5-15", "NEMA 5-20","J1772","J1772COMBO","CHAdeMO","Tesla")
        val spin = view?.findViewById<Spinner>(R.id.editConnectorInfoSpinner)
        val arrayAdapter =
            view?.let { ArrayAdapter(
                it.context,
                android.R.layout.simple_spinner_dropdown_item,
                list_of_connector
            ) }
        if (spin != null) {
            spin.adapter = arrayAdapter
        }
        if (spin != null) {
            spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    connectorSelected = list_of_connector[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    connectorSelected = ""
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
