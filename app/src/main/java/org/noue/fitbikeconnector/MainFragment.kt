package org.noue.fitbikeconnector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.noue.fitbikeconnector.databinding.MainFragmentBinding
import kotlin.math.roundToInt

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {
    private val TAG = "FitbikeMainFragment"

    private var _binding: MainFragmentBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

/*    private lateinit var mRPMBar: ProgressBar
    private lateinit var mRPM: TextView
    private lateinit var mGearBar: SeekBar
    private lateinit var mSpeed: TextView
    private lateinit var mSpeedBar: ProgressBar
    private lateinit var mLabelSpeed: TextView*/

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.main_fragment, container, false)
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mActivity = activity as MainActivity

        setGearRatio()
        binding.gearBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    setGearRatio()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) { }
                override fun onStopTrackingTouch(seekBar: SeekBar) { }
            })

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            //(activity as MainActivity).createWorkerWhenStopped() // for DEBUG
            //(activity as MainActivity).onUSbTimeout()
            findNavController().navigate(R.id.action_MainFragment_to_settingsFragment)
        }

        view.findViewById<ImageButton>(R.id.imageButtonLeft).setOnClickListener {
            mActivity.changeDirection("left")
        }
        view.findViewById<ImageButton>(R.id.imageButtonRight).setOnClickListener {
            mActivity.changeDirection("right")
        }
    }

    fun setGearRatio(){
        //(activity as MainActivity).mGearRatio = progress / mGearBar.max.toDouble()
        val rate = binding.gearBar.progress / binding.gearBar.max.toDouble()
        val ratio = (activity as MainActivity).setGear(rate)
        binding.valGear.text = "×%.2f".format(ratio)
    }

    fun update(rpm :Double, speed :Double){
        //val speed = rpm / 120.0 * mGearBar.progress / mGearBar.max.toFloat() * 50
        //RPM=120で50 km/h が最高になるように適当に換算。Barの数字を増やすと速度も上がる（ギアを上げるイメージ）
        binding.valRPM.post{ binding.valRPM.text = rpm.roundToInt().toString() }
        binding.rpmBar.progress = rpm.roundToInt()
        binding.valSpeed.post{binding.valSpeed.text = speed.roundToInt().toString()}
        binding.speedBar.progress = speed.roundToInt()
    }
}
