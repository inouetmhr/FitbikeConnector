package org.noue.fitbikeconnector

import android.content.*
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaFragment
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.journeyapps.barcodescanner.CaptureActivity
import java.io.IOException

class MainActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnStreetViewPanoramaReadyCallback {
    companion object {
        lateinit var sInstance: MainActivity  private set
    }

    private val TAG = "FitbikeMainAct"
    private val mReceiver = MyBroadcastReceiver()
    private val db = FirebaseFirestore.getInstance()

    private val maxSPEED = 50.0 // km/h
    private val maxRPM = 120.0
    private var mRPMtoKMH = 0.0 * maxSPEED / maxRPM // RPMをかけると時速になる係数
/*    var mGearRatio = 0.0
        set(value) {
            mGearMeter = value * maxSPEED * 1000 / 3600.0 / (maxRPM/60)
            // 120 RPM 時に mGearRation = 1.0 で 50km/h がでる換算
            field = value
        }*/
    var mGearMeter = 0.0 // 1回転で進む距離 [m]
        private set
    private lateinit var navController: NavController
    lateinit var pref: SharedPreferences
    lateinit var port: UsbSerialPort

    //画面回転でも Activity (instance) は再生成される
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() $this")
        setContentView(R.layout.activity_main)
        sInstance = this
        //Log.d(TAG, "service is stopped?: ${sService.isStopped}")

        //ToolBar と 戻るボタンの設定
        navController = findNavController(R.id.nav_host_fragment)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupActionBarWithNavController(navController, AppBarConfiguration(navController.graph))

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.registerOnSharedPreferenceChangeListener(this)
        if (pref.getBoolean(getString(R.string.pref_always_on),false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // USB再接続時にMainActivityを上げ直すため
        registerReceiver(mReceiver, IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED"))

        if (setupUSBSerial()) {
            Log.d(TAG, "USB setup succeed")
            createWorkerWhenStopped()
        }
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
                Log.v(TAG, "invoked option->settings..")
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_global_settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun updateMainFragment(rpm: Double){
        val navFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val fragment = navFragment?.childFragmentManager?.primaryNavigationFragment
        if (fragment is MainFragment && fragment.isVisible) {
            Log.v(TAG, "$this updateMainFragment $rpm")
            //this.runOnUiThread{ fragment.update(rpm) }
            fragment.update(rpm, getSpeed(rpm))
        }
    }

    fun setGear(gear: Double): Double {// gear: [0..1]
        //mGearRatio = gear
        mRPMtoKMH = gear * maxSPEED / maxRPM
        mGearMeter = gear * maxSPEED * 1000 / 3600.0 / (maxRPM/60) // 1回転で進む距離 [m]
        // 120 RPM 時に mGearRation = 1.0 で 50km/h がでる換算
        return mRPMtoKMH
    }

    private fun getSpeed(rpm: Double) : Double {
        //return maxSPEED * rpm / maxRPM * mGearRatio
        return rpm * mRPMtoKMH
    }

    private fun setupUSBSerial() :Boolean {
        Log.i(TAG, "getting a USB serial port")
        // Find all available drivers from attached devices.
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            Log.i(TAG, "serial driver is empty")
            return false
        }

        // Open a connection to the first available driver.
        val driver = availableDrivers[0]
        val device = driver.device
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
        }
        catch (e: IOException) {
            Log.w(TAG, "Unknown USB I/O error", e)
            if (port.isOpen) port.close()
            return false
        }
        return true
    }

    private fun createWorkerWhenStopped() {
        //if (UsbReaderWorker.sInstance == null || UsbReaderWorker.sInstance?.isStopped == true) // not worked
        val workManager = WorkManager.getInstance(applicationContext)
        val workQuery = WorkQuery.Builder
            .fromUniqueWorkNames(listOf(getString(R.string.work_request_unique)))
            .addStates(listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED))
            .build()
        val workInfos = workManager.getWorkInfos(workQuery).get()
        Log.d(TAG, "Latest worker: ${UsbReaderWorker.sInstance}" )
        Log.d(TAG, "Running WorkInfo: ${workInfos.size} ${workInfos}")
        if (workInfos.size == 0) {
            createWorker()
        }
        else {
            Log.i(TAG, "worker is already running")
        }
    }

    private fun createWorker() {
        Log.i(TAG, "creating a new worker request")
        val request = OneTimeWorkRequest.from(UsbReaderWorker::class.java)
        //workManager.getInstance(requireContext()).enqueue(request)
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            getString(R.string.work_request_unique),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun uploadToFirestore(dist: Double, direction: String = "") :Boolean{
        if (! pref.getBoolean(getString(R.string.pref_upload),false)) { return false }
        val userid = pref.getString(getString(R.string.pref_userid), null) ?: return false
        Log.d(TAG, "uploadToFirestore: dist:${dist} dir:${direction} userid:${userid}")
        val ts = Timestamp.now()
        val data: HashMap<String, Any> = HashMap()
        data.put("date", ts)
        if (dist != 0.0) data.put("distance", FieldValue.increment(dist))
        data.put("direction", direction)
        try {
            val document = db.collection("cycling").document(userid)
            document.set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.v(TAG, "uploadToFirestore: Document added.")
                    }
                    .addOnFailureListener {
                        Log.w(TAG, "uploadToFirestore: Error adding document", it)
                    }
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
            runOnUiThread {
                makeText(applicationContext, "Failed to connect server.\nUser id format error", Toast.LENGTH_SHORT).show()
            }
            return false
        }
        return true
    }

    fun move(distance: Double) {
        val navFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val fragment = navFragment?.childFragmentManager?.primaryNavigationFragment
        if (fragment is MapsFragment && fragment.isVisible) {
            Log.v(TAG, "$this move() $distance")
            //this.runOnUiThread{ fragment.update(rpm) }
            fragment.moveFor(distance)
        }
    }

    fun changeDirection(direction: String) {
        if (direction == "right" || direction == "left") {
            Log.i(TAG, "changeDirection: $direction")
            uploadToFirestore(0.0, direction)
        }
    }

    override fun onStreetViewPanoramaReady(streetViewPanorama: StreetViewPanorama) {
        val sanFrancisco = LatLng(37.754130, -122.447129)
        streetViewPanorama.setPosition(sanFrancisco)
    }

}

class MyCaptureActivity : CaptureActivity()

class MyBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.hardware.usb.action.USB_DEVICE_ATTACHED") {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}