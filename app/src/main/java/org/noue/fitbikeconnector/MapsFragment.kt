package org.noue.fitbikeconnector

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil

/*
class SavedStateViewModel(private val state: SavedStateHandle) : ViewModel() {
    var panoId : String? = null
    var bearing : Float? = null
    fun save(panorama: StreetViewPanorama) {
        panoId = panorama.location.panoId
        bearing = panorama.panoramaCamera.bearing
    }
}*/

class MapsFragment : Fragment(), //OnStreetViewPanoramaReadyCallback,
    StreetViewPanorama.OnStreetViewPanoramaChangeListener,
    StreetViewPanorama.OnStreetViewPanoramaCameraChangeListener{
    private val TAG = this.javaClass.toString().substringAfterLast(".")
    //private val vm: SavedStateViewModel by activityViewModels()

    private lateinit var mSpeed: TextView
    private lateinit var panorama : StreetViewPanorama
    private lateinit var pref : SharedPreferences

    private var prevLocation = LatLng(37.754130, -122.447129) // san francisco
    private var moving = false
    private var ready = false

    private var distanceTogo = 0.0
    private var prevTime  = System.currentTimeMillis()
    private var distanceFromPrevTime = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    /* override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        Log.d(TAG, "restore bundle: $savedInstanceState")
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Log.d(TAG, "bundle: $savedInstanceState")
        pref = (activity as MainActivity).pref
        val mapFragment = childFragmentManager.findFragmentById(R.id.streetviewpanorama) as SupportStreetViewPanoramaFragment
        mapFragment.getStreetViewPanoramaAsync {
            panorama = it
            it.setOnStreetViewPanoramaChangeListener(this)
            it.setOnStreetViewPanoramaCameraChangeListener(this)
            //it.setOnStreetViewPanoramaClickListener(this)
            //it.setOnStreetViewPanoramaLongClickListener(this)
            val panoId = pref.getString("panoid", null)
            val bearing = pref.getFloat("bearing", 0F)
            Log.d(TAG, "$panoId, $bearing")
            if (panoId == null) {
                it.setPosition(prevLocation) // defaul start location
                Log.d(TAG, "state cleared")
            } else {
                it.setPosition(panoId)
                setHeading(bearing,0)
                Log.d(TAG, "state restored")
            }
            ready = true
        }

        mSpeed = view.findViewById<TextView>(R.id.svSpeed)
        mSpeed.setOnClickListener(){ moveFor(distance = 5.0) } // for debug
    }

    /*override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("panoid", panorama.location.panoId)
        outState.putFloat("bearing", panorama.panoramaCamera.bearing)
        Log.d(TAG, "saved: $outState")
        super.onSaveInstanceState(outState)
    }*/

    /*override fun onStreetViewPanoramaReady(pano: StreetViewPanorama) {
        val sanFrancisco = LatLng(37.754130, -122.447129)
        pano.setPosition(sanFrancisco)
        //panorama = pano
    }*/

    fun moveFor(distance: Double) {
        distanceTogo += distance
        Log.d(TAG, "moveFor(): $distance, $distanceTogo")
        if (distanceTogo > 0 && ! moving) {
            requireActivity().runOnUiThread() { moveForward() }
        }
    }

    private fun moveForward() {
        Log.d(TAG, "moveForward");

        val links =  try {
            panorama.location.links
        } catch (e: Exception) {
            Log.w(TAG, "panorama is not ready.")
            return
        }
        if (links.isEmpty()) {Log.w(TAG, "link is empty."); return}

        var nextLink =  links[0];
        links.forEach {
            if(directionDiff(it) < directionDiff(nextLink) ) {
                nextLink = it
            }
        }
        Log.v(TAG, "bearing diff: ${directionDiff(nextLink)}")
        prevLocation = panorama.location.position
        moving = true
        setHeading(nextLink.bearing, 100) //TODO headingsを一緒にセットすることはできないっぽい
        panorama.setPosition(nextLink.panoId)
        //bearingTogo = nextLink.bearing
        Log.v(TAG, "$nextLink")
    }

    //現在の進行方向と隣接linkとの向きの差分を計算
    private fun directionDiff(link: StreetViewPanoramaLink): Float {
        //link: 隣接 panorama , link.heading
        //bearing: 北0 南 180 で方位360℃の方向の示す
        var diff = Math.abs(panorama.panoramaCamera.bearing % 360 - link.bearing)
        if (diff > 180F) diff = 360F - diff
        return diff
    }

    private fun setHeading(bearing: Float, duration: Long = 200) {
        Log.v(TAG, "set bearing to: $bearing")
        val camera = StreetViewPanoramaCamera.Builder(panorama.panoramaCamera)
            .bearing(bearing)
            .build()
        panorama.animateTo(camera, duration)
        //bearingTogo = null
    }

    private fun newView() {
        val sanFrancisco = LatLng(37.754130, -122.447129)
        val view = StreetViewPanoramaView(
            this.requireContext(),
            StreetViewPanoramaOptions().position(sanFrancisco)
        )
    }

    private fun setLocationOfThePanorama(streetViewPanorama: StreetViewPanorama) {
        val sanFrancisco = LatLng(37.754130, -122.447129)

        // Set position with LatLng only.
        streetViewPanorama.setPosition(sanFrancisco)

        // Set position with LatLng and radius.
        streetViewPanorama.setPosition(sanFrancisco, 20)

        // Set position with LatLng and source.
        streetViewPanorama.setPosition(sanFrancisco, StreetViewSource.OUTDOOR)

        // Set position with LaLng, radius and source.
        streetViewPanorama.setPosition(sanFrancisco, 20, StreetViewSource.OUTDOOR)

        streetViewPanorama.location.links.firstOrNull()?.let { link: StreetViewPanoramaLink ->
            streetViewPanorama.setPosition(link.panoId)
        }
    }

    private fun zoom(streetViewPanorama: StreetViewPanorama) {
        val zoomBy = 0.5f
        val camera = StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.panoramaCamera.zoom + zoomBy)
            .tilt(streetViewPanorama.panoramaCamera.tilt)
            .bearing(streetViewPanorama.panoramaCamera.bearing)
            .build()
    }

    private fun pan(streetViewPanorama: StreetViewPanorama) {
        val panBy = 30f
        val camera = StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.panoramaCamera.zoom)
            .tilt(streetViewPanorama.panoramaCamera.tilt)
            .bearing(streetViewPanorama.panoramaCamera.bearing - panBy)
            .build()
    }

    private fun tilt(streetViewPanorama: StreetViewPanorama) {
        var tilt = streetViewPanorama.panoramaCamera.tilt + 30
        tilt = if (tilt > 90) 90f else tilt
        val previous = streetViewPanorama.panoramaCamera
        val camera = StreetViewPanoramaCamera.Builder(previous)
            .tilt(tilt)
            .build()
    }

    private fun animate(streetViewPanorama: StreetViewPanorama) {
        // Keeping the zoom and tilt. Animate bearing by 60 degrees in 1000 milliseconds.
        val duration: Long = 1000
        val camera = StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.panoramaCamera.zoom)
            .tilt(streetViewPanorama.panoramaCamera.tilt)
            .bearing(streetViewPanorama.panoramaCamera.bearing - 60)
            .build()
        streetViewPanorama.animateTo(camera, duration)
    }

    override fun onStreetViewPanoramaChange(location: StreetViewPanoramaLocation?) {
        Log.v(TAG, "onStreetViewPanoramaChange")
        positionChanged()
        if (distanceTogo > 0) moveForward()
        //bearingTogo?.let { setBearing(it) }
    }

    override fun onStreetViewPanoramaCameraChange(p0: StreetViewPanoramaCamera?) {
        Log.v(TAG, "onStreetViewPanoramaCameraChange")
    }

    private fun positionChanged() {
        //saveHistory(curr_location); // TODO firebaseへの保存はまずは不要で
        val traveled = SphericalUtil.computeDistanceBetween(
            prevLocation,
            panorama.location.position
        )
        Log.d(TAG, "position_changed. moved: $traveled")
        moving = false
        //if (isNaN(traveled)) traveled = 0;
        if (traveled < 20) {// 20m 以上の移動は手動なので無視
            distanceTogo -= traveled;
            distanceFromPrevTime += traveled;
            val now = System.currentTimeMillis()
            val duration = (now - prevTime) / 1000; // sec
            if (duration > 10 ) { // 10秒以上後なら
                prevTime = now;
                Log.d(TAG, "diff for speed: $distanceFromPrevTime");
                val speed = distanceFromPrevTime / duration * 3600.0 / 1000.0;
                mSpeed.post{mSpeed.text =  "%.1f km/h".format(speed) }
                distanceFromPrevTime = 0.0
            }
        }
        //vm.save(panorama)
        pref.edit()
            .putString("panoid", panorama.location.panoId)
            .putFloat("bearing", panorama.panoramaCamera.bearing)
            .apply()
        //if (isNaN(distance_togo)) distance_togo = 0.0;
        Log.d(TAG, "rest to go: $distanceTogo")
    }

    /* //進んだ距離
    private fun distance(from: LatLng, to: LatLng) : Double {
        val R = 6371.01 * 1000; // Radius of the Earth in meters
        //console.log(prev.lat,prev.lng)
        //console.log(curr.lat,curr.lng)
        val rlat1 = from.latitude * (Math.PI/180); // Convert degrees to radians
        val rlat2 = to.latitude * (Math.PI/180); // Convert degrees to radians
        val difflat = rlat2-rlat1; // Radian difference (latitudes)
        val difflon = (from.longitude-to.longitude) * (Math.PI/180); // Radian difference (longitudes)
        val d = 2 * R * asin(
            Math.sqrt(
                sin(difflat / 2) * sin(difflat / 2) + cos(rlat1) * cos(rlat2) * sin(
                    difflon / 2
                ) * sin(difflon / 2)
            )
        )
        return d
    }*/
}