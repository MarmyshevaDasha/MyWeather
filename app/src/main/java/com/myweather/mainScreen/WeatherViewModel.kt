package com.myweather.mainScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.GsonBuilder
import com.myweather.model.Root
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class WeatherViewModel : ViewModel() {
    private val liveDataDegree: MutableLiveData<String> = MutableLiveData()
    private val liveDataClouds: MutableLiveData<Int> = MutableLiveData()
    private val liveDataCity: MutableLiveData<String> = MutableLiveData()
    private val executorService = Executors.newCachedThreadPool()

    fun getDegree(): LiveData<String> = liveDataDegree
    fun getClouds(): LiveData<Int> = liveDataClouds
    fun getCity(): LiveData<String> = liveDataCity

    fun showWeather(lat: String?, lon: String?) {
        executorService.submit {
            val root = getRoot(lat, lon)
            if (root != null) {
                liveDataDegree.postValue(root.main.temp.roundToInt().toString())
                liveDataClouds.postValue(root.clouds.all)
                liveDataCity.postValue(root.name)
            }
        }
    }

    private fun getRoot(lat: String?, lon: String?): Root? {
        var httpURLConnection: HttpURLConnection? = null
        val root: Root?
        val url: URL
        try {
            url = if (lat == null && lon == null) {
                URL("https://api.openweathermap.org/data/2.5/weather?q=Sochi&appid=ed4a4902496e9a233366e0e0ec3e0c52&units=metric")
            } else {
                URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=ed4a4902496e9a233366e0e0ec3e0c52&units=metric")
            }
            httpURLConnection = url.openConnection() as HttpURLConnection
            val br = BufferedReader(InputStreamReader(httpURLConnection.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("\n")
            }
            br.close()
            val builder = GsonBuilder()
            val gson = builder.create()
            root = gson.fromJson(sb.toString(), Root::class.java)
            return root
        } catch (e: UnknownHostException) {
            println("UnknownHostException: $e")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            httpURLConnection?.disconnect()
        }
        return null
    }
}