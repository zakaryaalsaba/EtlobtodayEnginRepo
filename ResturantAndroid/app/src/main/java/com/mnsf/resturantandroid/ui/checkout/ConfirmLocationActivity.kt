package com.mnsf.resturantandroid.ui.checkout

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityConfirmLocationBinding
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.LocationHelper
import kotlinx.coroutines.launch

class ConfirmLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityConfirmLocationBinding
    private var restaurantId: Int = -1
    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var defaultLatLng = LatLng(31.9454, 35.9284) // Amman, Jordan

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2001

        fun newIntent(context: Context, restaurantId: Int): Intent {
            return Intent(context, ConfirmLocationActivity::class.java).apply {
                putExtra("restaurant_id", restaurantId)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getIntExtra("restaurant_id", -1)
        if (restaurantId == -1) {
            Toast.makeText(this, getString(R.string.error_no_restaurant), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSearch.setOnClickListener {
            // Optional: open place search; for now just show a message
            Toast.makeText(this, getString(R.string.search), Toast.LENGTH_SHORT).show()
        }

        binding.btnEnterCompleteAddress.setOnClickListener {
            val latLng = selectedLatLng ?: defaultLatLng
            startActivity(
                NewAddressActivity.newIntent(
                    this,
                    restaurantId = restaurantId,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude
                )
            )
            finish()
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }

        lifecycleScope.launch {
            val location = if (LocationHelper.hasLocationPermission(this@ConfirmLocationActivity)) {
                LocationHelper.getCurrentLocation(this@ConfirmLocationActivity)
            } else null
            val latLng = location?.let { LatLng(it.latitude, it.longitude) } ?: defaultLatLng
            selectedLatLng = latLng
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title(getString(R.string.your_order_will_be_delivered_here)))
            map.setOnMapClickListener { tapped ->
                selectedLatLng = tapped
                map.clear()
                map.addMarker(MarkerOptions().position(tapped).title(getString(R.string.your_order_will_be_delivered_here)))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.let { map ->
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    map.isMyLocationEnabled = true
                }
                lifecycleScope.launch {
                    LocationHelper.getCurrentLocation(this@ConfirmLocationActivity)?.let { location ->
                        val latLng = LatLng(location.latitude, location.longitude)
                        selectedLatLng = latLng
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        map.clear()
                        map.addMarker(MarkerOptions().position(latLng).title(getString(R.string.your_order_will_be_delivered_here)))
                    }
                }
            }
        }
    }
}
