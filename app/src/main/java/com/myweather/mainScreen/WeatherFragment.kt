package com.myweather.mainScreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.gson.GsonBuilder
import com.myweather.R
import com.myweather.model.Root
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.Date
import kotlin.math.roundToInt

class WeatherFragment : Fragment() {
    private lateinit var tvLocationNet: TextView
    private lateinit var tvLocationGPS: TextView
    private lateinit var textDegree: TextView
    private lateinit var imageCloud: ImageView
    private lateinit var textCityName: TextView
    private lateinit var locationManager: LocationManager
    private val REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION = 999
    private val viewModel: WeatherViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.weather_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tvLocationGPS = view.findViewById(R.id.tvLocationGPS)
        tvLocationNet = view.findViewById(R.id.tvLocationNet)
        textDegree = view.findViewById(R.id.textDegree)
        imageCloud = view.findViewById(R.id.imageCloud)
        textCityName = view.findViewById(R.id.textCityName)
        viewModel.getDegree().observe(viewLifecycleOwner) { degree -> textDegree.text = degree }
        viewModel.getClouds().observe(viewLifecycleOwner) { clouds ->
            when {
                clouds <= 20 -> {
                    imageCloud.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.white_balance_sunny))
                }
                clouds <= 50 -> {
                    imageCloud.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.weather_partly_cloudy))
                }
                else -> {
                    imageCloud.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.cloud_outline))
                }
            }
        }
        viewModel.getCity().observe(viewLifecycleOwner) { city -> textCityName.text = city }

        val permissionStatus = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation()
            } else {
                val toast = Toast.makeText(requireContext(), "Доступ к геолокации не предоставлен", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            viewModel.showWeather(null, null)
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
            1000 * 10.toLong(), 10f, locationListener)
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 1000 * 10.toLong(), 10f,
            locationListener)
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        showLocation(location)
        val lat = location?.latitude.toString()
        val lon = location?.longitude.toString()
        viewModel.showWeather(lat, lon)
    }

    private fun showLocation(location: Location?) {
        if (location == null) return
        if (location.provider == LocationManager.GPS_PROVIDER) {
            tvLocationGPS.text = formatLocation(location)
        } else if (location.provider ==
            LocationManager.NETWORK_PROVIDER) {
            tvLocationNet.text = formatLocation(location)
        }
    }

    private fun formatLocation(location: Location?): String {
        return if (location == null) "" else String.format(
            "Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3\$tF %3\$tT",
            location.latitude, location.longitude, Date(
            location.time))
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            showLocation(location)
        }

        override fun onProviderEnabled(provider: String) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            showLocation(locationManager.getLastKnownLocation(provider))
        }
    }
}