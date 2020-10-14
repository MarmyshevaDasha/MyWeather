package com.myweather.mainScreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.myweather.R;
import com.myweather.model.Root;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;


public class WeatherFragment extends Fragment {

    private WeatherViewModel mViewModel;

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
        final TextView textDegree = view.findViewById(R.id.textDegree);
        final ImageView imageCloud = view.findViewById(R.id.imageCloud);
        final TextView textCityName = view.findViewById(R.id.textCityName);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Root root = getRoot();
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (root != null) {
                            root.clouds.all = 80;
                            textDegree.setText(String.valueOf(Math.round(root.main.temp)));
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

    private Root getRoot() {
        HttpURLConnection httpURLConnection = null;
        Root root = null;
        try {
            URL url = new URL("https://api.openweathermap.org/data/2.5/weather?q=Sochi&appid=ed4a4902496e9a233366e0e0ec3e0c52&units=metric");
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