package org.noue.fitbikeconnector

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator

class SettingsFragment : PreferenceFragmentCompat() {
    private val TAG = "FitbikeSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        //PreferenceFragmentCompat以外のFragmentでやりたい場合の snippet
        //act = activity as MainActivity
        //settings = childFragmentManager.findFragmentById(R.id.settings_fragment) as SettingsFragment

        findPreference<Preference>(getString(R.string.pref_read_qr))?.setOnPreferenceClickListener {
            Log.d(TAG, "OnPreferenceClick")
            //findPreference<EditTextPreference>("username")?.text = Random.nextInt(1000).toString()
            val scanner = IntentIntegrator.forSupportFragment(this).apply{
                setPrompt("Scan a QR Code")
                captureActivity = MyCaptureActivity::class.java
                setOrientationLocked(false)
                setBeepEnabled(false)
                setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            }
            scanner.initiateScan()
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        when {
            result != null -> {
                val qrstr = result.contents
                Log.d(TAG, "read QR Code: ${qrstr}")
                val qrcontents = qrstr?.split(":", limit = 2)
                if (qrcontents?.size == 2) {
                    val username = qrcontents[0]
                    val userid = qrcontents[1]
                    findPreference<EditTextPreference>(getString(R.string.pref_username))?.text = username
                    findPreference<EditTextPreference>(getString(R.string.pref_userid))?.text = userid
/*                    val name = act.pref.edit()
                    with (act.pref.edit()) {
                        putString("username", username)
                        putString("userid", userid)
                        commit()
                    }*/
                    Snackbar.make(requireView(), "Connected to the server\n${userid}", Snackbar.LENGTH_LONG).show()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}