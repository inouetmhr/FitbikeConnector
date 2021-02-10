package org.noue.fitbikeconnector

import android.content.*
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.journeyapps.barcodescanner.CaptureActivity
import java.io.IOException

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val TAG = "FitbikeMainAct"
    private val mReceiver = MyBroadcastReceiver()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var navController: NavController
    private lateinit var pref: SharedPreferences
    lateinit var port: UsbSerialPort

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //ToolBar と 戻るボタンの設定
        navController = findNavController(R.id.nav_host_fragment)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupActionBarWithNavController(navController, AppBarConfiguration(navController.graph))

        //mRPMBar = findViewById(R.id.rpmBar)  // fragment の view は取れない

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.registerOnSharedPreferenceChangeListener(this)
        if (pref.getBoolean(getString(R.string.pref_always_on),false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // USB再接続時にMainActivityを上げ直すため
        registerReceiver(mReceiver, IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED"))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }

    override fun onResume() {
        super.onResume()
        pref.registerOnSharedPreferenceChangeListener(this)
    }
    // TODO: Backgroud動作は適切？

    override fun onPause() {
        super.onPause()
        pref.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_always_on)) {
            val alwayson = sharedPreferences.getBoolean(key, false)
            Log.i(TAG, "Preference `${R.string.pref_always_on}` value was updated to: ${alwayson}")
            if (alwayson) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun onSupportNavigateUp()
            = findNavController(R.id.nav_host_fragment).navigateUp()

    class MyBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "android.hardware.usb.action.USB_DEVICE_ATTACHED") {
                context.startActivity(Intent(context, MainActivity::class.java))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                Log.v(TAG, "invoked option->settings..");
                //findNavController(R.id.nav_host_fragment).navigate(R.id.action_FirstFragment_to_SecondFragment)
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_global_settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun setupUSBSerial() :Boolean {
        // Find all available drivers from attached devices.
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            Log.i(TAG, "serial driver is empty");
            return false
        }

        // Open a connection to the first available driver.
        val driver = availableDrivers[0]
        val device = driver.device;
        if (!manager.hasPermission(device)) {
            manager.requestPermission(
                device,
                android.app.PendingIntent.getBroadcast(this, 0, Intent("start"), 0)
            )
            return false
        }
        val connection = manager.openDevice(driver.device)
            ?: // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return false
        port = driver.ports[0] // Most devices have just one port (port 0)
        try {
            port.open(connection)
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            //startReadThread()
        }
        catch (e: IOException) {
            Log.w(TAG, "Unknown USB I/O error", e);
            if (port.isOpen) port.close()
            return false
        }
        return true
    }

    fun saveStore(dist: Double) :Boolean{
        if (! pref.getBoolean(getString(R.string.pref_upload),false)) { return false }
        val userid = pref.getString(getString(R.string.pref_userid), null) ?: return false
        Log.d(TAG, "invoked saveStore: dist:${dist} userid:${userid}")
        val ts = Timestamp.now()
        val data: HashMap<String, Any> = HashMap()
        data.put("date", ts)
        //data.put("distance", FieldValue.increment(dist))
        data.put("distance", FieldValue.increment(15))
        db.collection("cycling").document(userid).set(data, SetOptions.merge())
            .addOnSuccessListener(OnSuccessListener<Any?> {
                Log.d(TAG, "Document added !" )
            })
            .addOnFailureListener(OnFailureListener {
                Log.w(TAG, "Error adding document", it)
            })
        return true
    }

}

class MyCaptureActivity : CaptureActivity()
