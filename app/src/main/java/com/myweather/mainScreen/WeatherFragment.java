package com.myweather.mainScreen;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.myweather.MainActivity;
import com.myweather.R;
import com.myweather.model.Root;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

import static android.content.Context.LOCATION_SERVICE;


public class WeatherFragment extends Fragment {
    TextView tvLocationNet;
    TextView tvLocationGPS;
    private TextView textDegree;
    private ImageView imageCloud;
    private TextView textCityName;
    private WeatherViewModel mViewModel;

    private LocationManager locationManager;

    private final int REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION = 999;

    public static WeatherFragment newInstance() {
        return new WeatherFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.weather_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(WeatherViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationManager = (LocationManager) requireActivity().getSystemService(LOCATION_SERVICE);
        tvLocationGPS = view.findViewById(R.id.tvLocationGPS);
        tvLocationNet = view.findViewById(R.id.tvLocationNet);
        textDegree = view.findViewById(R.id.textDegree);
        imageCloud = view.findViewById(R.id.imageCloud);
        textCityName = view.findViewById(R.id.textCityName);
        int permissionStatus = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocation();
                } else {
                    Toast toast = Toast.makeText(requireContext(), "Доступ к геолокации не предоставлен", Toast.LENGTH_SHORT);
                    toast.show();
                }
                break;
        }
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showWeather(null, null);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                locationListener);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        showLocation(location);
        String lat = String.valueOf(location.getLatitude());
        String lon = String.valueOf(location.getLongitude());
        showWeather(lat, lon);
    }

    private void showLocation(Location location) {
        if (location == null)
            return;
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            tvLocationGPS.setText(formatLocation(location));
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            tvLocationNet.setText(formatLocation(location));
        }
    }

    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format(
                "Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3$tF %3$tT",
                location.getLatitude(), location.getLongitude(), new Date(
                        location.getTime()));
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }


        @Override
        public void onProviderEnabled(String provider) {
            if (ActivityCompat.checkSelfPermission(WeatherFragment.this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(WeatherFragment.this.requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            showLocation(locationManager.getLastKnownLocation(provider));
        }
    };

    private void showWeather(final String lat, final String lon) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Root root = getRoot(lat, lon);

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (root != null) {
                            root.clouds.all = 80;
                            textDegree.setText(String.valueOf((int) Math.round(root.main.temp)));
                            textCityName.setText(root.name);
                            if (root.clouds.all <= 20) {
                                imageCloud.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.white_balance_sunny));
                            } else if (root.clouds.all <= 50) {
                                imageCloud.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.weather_partly_cloudy));
                            } else {
                                imageCloud.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.cloud_outline));
                            }
                        }
                    }
                });

            }
        });
        thread.start();
    }

    private Root getRoot(String lat, String lon) {
        HttpURLConnection httpURLConnection = null;
        Root root = null;
        URL url;
        try {
            if (lat == null && lon == null) {
                url = new URL("https://api.openweathermap.org/data/2.5/weather?q=Sochi&appid=ed4a4902496e9a233366e0e0ec3e0c52&units=metric");
            } else {
                url = new URL("https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=ed4a4902496e9a233366e0e0ec3e0c52&units=metric");
            }
            httpURLConnection = (HttpURLConnection) url.openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            root = gson.fromJson(sb.toString(), Root.class);
            return root;

        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException: " + e);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return null;
    }

}