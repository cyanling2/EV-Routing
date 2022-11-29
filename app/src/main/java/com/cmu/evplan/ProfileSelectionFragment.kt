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
import com.cmu.evplan.R
import com.cmu.evplan.RoutingViewModel
import com.cmu.evplan.databinding.FragmentProfileSelectionBinding

class ProfileSelectionFragment : Fragment(){
    private var _binding: FragmentProfileSelectionBinding? = null

    private val binding get() = _binding!!

    private val viewModel: RoutingViewModel by activityViewModels()

    private var saved: Boolean = false
    private var brandSelected: String = ""
    private var modelSelected: String = ""
    private var connectorSelected: String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileSelectionBinding.inflate(inflater, container, false)
        val view = binding.root

        //setupBrandSpinner(view)
        // setupConnectorSpinner(view)

        // select brand
        val list_of_brand = arrayOf("Tesla", "Toyota","Hyundai","Nissan","KIA")
        val spin_brand = view?.findViewById<Spinner>(R.id.editBrandInfoSpinner)
        var list_of_model = arrayOf("Model3","ModelS","ModelX","ModelY")

        var spin_model = view?.findViewById<Spinner>(R.id.editModelInfoSpinner)
        var arrayAdapter2 =
            view?.let { ArrayAdapter(
                it.context,
                android.R.layout.simple_spinner_dropdown_item,
                list_of_model
            ) }

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
                    brandSelected = list_of_brand[position]
                    println("brand selected" + brandSelected)

                    if (brandSelected == "Tesla") {
                        list_of_model = arrayOf("Model3","ModelS","ModelX","ModelY")
                    } else if (brandSelected == "Toyota") {
                        list_of_model = arrayOf("bZ4X","Hybrid")
                    } else if (brandSelected == "Hyundai") {
                        println("into hyundai")
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
                    println("spin model" +spin_model)

                    if (spin_model != null) {
                        spin_model.adapter = ad
                        println("spin model 22222")
                        println(arrayAdapter2)
                    }
                    if (spin_model != null) {
                        spin_model.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                modelSelected = list_of_model[position]
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                modelSelected = ""
                            }
                        }
                    }
                    println("have reset model values")
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    brandSelected = ""
                }
            }
        }


        var list_of_connector = arrayOf("NEMA14-50", "NEMA5-15", "NEMA 5-20","J1772","J1772COMBO","CHAdeMO","Tesla")
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
                    connectorSelected = ""
                }
            }
        }

        _binding!!.btnSave.setOnClickListener {
            saved = true;
            viewModel.setVehicleBrand(brandSelected)
            viewModel.setVehicleModel(modelSelected)
            viewModel.setConnectorType(connectorSelected)
            println("selected:" + viewModel.getConnectorType())

            binding.btnSave.setText("Saved")
            binding.btnSave.setBackgroundColor(Color.GRAY)
            // findNavController().navigate(R.id.action_profileSelectionFragment_to_profileFragment)
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
                    println("brand selected" + brandSelected)
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
        println("into model selector" + brandSelected)
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
