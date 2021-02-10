package org.noue.fitbikeconnector

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.io.IOException
import kotlin.math.roundToInt

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {
    private val TAG = "FitbikeMainFragment"
    private lateinit var mActivity: MainActivity

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

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_settingsFragment)
        }

        mRPMBar = view.findViewById(R.id.rpmBar)
        mRPM = view.findViewById(R.id.valRPM)
        mGearBar = view.findViewById(R.id.gearBar)
        mSpeed = view.findViewById(R.id.valSpeed)
        mSpeedBar = view.findViewById(R.id.speedBar)
        mLabelSpeed = view.findViewById(R.id.textSpeed) // for debug purpose

        mActivity = activity as MainActivity
        if (mActivity.setupUSBSerial()) {
            startReadThread()
        }
    }

    private fun startReadThread() = Thread(Runnable {
        var msg: String
        var rpm: Double
        val rpms = MutableList(5) { 0.0 }
        var distance = 0
        //var unread_count = 0
        var updFlg = false
        try {
            while (true) {
                val buffer = ByteArray(16)
                val numBytesRead: Int = mActivity.port.read(buffer, 1000)
                if (numBytesRead > 0) {
                    msg = String(buffer, 0, numBytesRead)
                    Log.v(TAG, "read from usb serial:${msg}")
                    rpm = msg.toDouble() // タイミング的に数値が２つはいったりすると NumberFormatException になるので最初の行だけ読んだほうが良いかも
                    //rpm = (rpm + msg.toDouble()) / 2.0 // いまいちなので 5回平均にしたい
                    distance += 3
                    updFlg = true
                } else { //unread_count += 1
                    rpm = 0.0
                }
                rpms.add(rpm)
                rpms.removeAt(0)
                rpm = rpms.average()

                val speed = rpm / 120.0 * mGearBar.progress / mGearBar.max.toFloat() * 50
                //RPM=120で50 km/h が最高になるように適当に換算 Barの rating を増やすと速度も上がる（ギアを上げるイメージ）

                if (distance % 15 == 0 && updFlg) {
                    updFlg = false
                    mActivity.saveStore(distance.toDouble())
                }
                mActivity.runOnUiThread(Runnable {
                    //mLabelSpeed.text = "printf debug" //for debug
                    //mRPM.post(Runnable { mRPM.text = rpm.roundToInt().toString() })
                    //mSpeed.post(Runnable { mSpeed.text = speed.roundToInt().toString() })
                    mRPM.text = rpm.roundToInt().toString()
                    mRPMBar.progress = rpm.roundToInt()
                    mSpeed.text = speed.roundToInt().toString()
                    mSpeedBar.progress = speed.roundToInt()
                })
                //Thread.sleep(100)
            }
        } catch (e: IOException) {  //USB外れた場合
            Log.i(TAG, "USB serial disconnected");
            //mRPM.post(Runnable { makeText(applicationContext, "Serial device disconnected", Toast.LENGTH_LONG).show() })
            mActivity.runOnUiThread(Runnable {
                Toast.makeText(
                    mActivity.applicationContext,
                    "Serial device disconnected",
                    Toast.LENGTH_LONG
                ).show()
            })
            if (mActivity.port.isOpen) mActivity.port.close()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }).start()


}