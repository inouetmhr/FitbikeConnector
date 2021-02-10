package org.noue.fitbikeconnector

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreference
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 * 残骸。activity や fragmment 取得の参考用。
 */
class SecondFragment : Fragment() {
    //private lateinit var act: MainActivity
    private lateinit var settings: SettingsFragment

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //act = activity as MainActivity
        settings = childFragmentManager.findFragmentById(R.id.settings_fragment) as SettingsFragment

        view.findViewById<Button>(R.id.button_QR).setOnClickListener {
            val scanner = IntentIntegrator.forSupportFragment(this).apply{
                setPrompt("Scan a QR Code")
                captureActivity = MyCaptureActivity::class.java
                setOrientationLocked(false)
                setBeepEnabled(false)
                setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            }
            scanner.initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        when {
            result != null -> {
                val qrstr = result.contents
                val qrcontents = qrstr?.split(":", limit = 2)
                if (qrcontents?.size == 2) {
                    val username = qrcontents[0]
                    val userid = qrcontents[1]
                    Log.d("readQR", userid)

                    settings.findPreference<EditTextPreference>("username")?.text = username
                    settings.findPreference<EditTextPreference>("userid")?.text = userid

                    Snackbar.make(requireView(), "Connected to the server\n${userid}", Snackbar.LENGTH_LONG).show()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}