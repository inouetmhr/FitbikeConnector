package org.noue.fitbikeconnector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlin.math.roundToInt

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {
    private val TAG = "FitbikeMainFragment"

    private lateinit var mRPMBar: ProgressBar
    private lateinit var mRPM: TextView
    private lateinit var mGearBar: SeekBar
    private lateinit var mSpeed: TextView
    private lateinit var mSpeedBar: ProgressBar
    private lateinit var mLabelSpeed: TextView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mActivity = activity as MainActivity

        mRPMBar = view.findViewById(R.id.rpmBar)
        mRPM = view.findViewById(R.id.valRPM)
        mGearBar = view.findViewById(R.id.gearBar)
        mSpeed = view.findViewById(R.id.valSpeed)
        mSpeedBar = view.findViewById(R.id.speedBar)
        mLabelSpeed = view.findViewById(R.id.textSpeed) // for debug purpose

        setGearRatio(mGearBar.progress)
        mGearBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    setGearRatio(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) { }
                override fun onStopTrackingTouch(seekBar: SeekBar) { }
            })

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            //(activity as MainActivity).createWorkerWhenStopped() // for DEBUG
            findNavController().navigate(R.id.action_MainFragment_to_settingsFragment)
        }

        view.findViewById<ImageButton>(R.id.imageButtonLeft).setOnClickListener {
            mActivity.changeDirection("left")
        }
        view.findViewById<ImageButton>(R.id.imageButtonRight).setOnClickListener {
            mActivity.changeDirection("right")
        }
    }

    fun setGearRatio(progress: Int){
        (activity as MainActivity).mGearRatio = progress / mGearBar.max.toDouble()
    }

    fun update(rpm :Double, speed :Double){
        //val speed = rpm / 120.0 * mGearBar.progress / mGearBar.max.toFloat() * 50
        //RPM=120で50 km/h が最高になるように適当に換算。Barの数字を増やすと速度も上がる（ギアを上げるイメージ）
        mRPM.post{ mRPM.text = rpm.roundToInt().toString() }
        mRPMBar.progress = rpm.roundToInt()
        mSpeed.post{mSpeed.text = speed.roundToInt().toString()}
        mSpeedBar.progress = speed.roundToInt()
    }
}