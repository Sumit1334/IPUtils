package com.sumit.iputils;

import android.app.Activity;
import android.util.Log;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IPUtils extends AndroidNonvisibleComponent implements Component {
    private final Activity activity;
    private final String TAG = "IPUtils";

    public IPUtils(ComponentContainer container) {
        super(container.$form());
        this.activity = container.$context();
        Log.i(TAG, "Extension Initialized");
    }

    @SimpleEvent(description = "Raises when any error occurs")
    public void ErrorOccurred(String error) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", error);
    }

    @SimpleEvent(description = "This event raises when ip fetched")
    public void GotIP(String ip) {
        EventDispatcher.dispatchEvent(this, "GotIP", ip);
    }

    @SimpleFunction(description = "Fetch the ip address of the user")
    public void GetIP() {
        sendRequest("https://api.ipify.org", new Callback() {
            @Override
            public void onSuccess(String response) {
                GotIP(response);
            }
        });
    }

    @SimpleEvent(description = "This event raises when the details of IP is fetched")
    public void GotIPDetails(String response, String countryCode, String countryName) {
        EventDispatcher.dispatchEvent(this, "GotIPDetails", response, countryCode, countryName);
    }

    @SimpleFunction(description = "Fetch the details of the given IP")
    public void GetIPDetails(String ip) {
        sendRequest("http://www.geoplugin.net/json.gp?ip=" + ip, new Callback() {
            @Override
            public void onSuccess(String response) {
                GotIPDetails(response, GetValue(response, "geoplugin_countryCode").toString(), GetValue(response, "geoplugin_countryName").toString());
            }
        });
    }

    @SimpleFunction(description = "Get the value of given key from the given JSON response")
    public Object GetValue(String response, String key) {
        try {
            return new JSONObject(response).get(key);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            ErrorOccurred(e.getMessage());
            return null;
        }
    }

    private void sendRequest(String url, Callback callback) {
        try {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        InputStream stream;
                        if (connection.getResponseCode() == 200)
                            stream = connection.getInputStream();
                        else
                            stream = connection.getErrorStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        StringBuilder stringBuilder = new StringBuilder();
                        int cp;
                        while ((cp = reader.read()) != -1) {
                            stringBuilder.append((char) cp);
                        }
                        final String readLine = stringBuilder.toString();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(readLine);
                            }
                        });
                    } catch (Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e(TAG, "", e);
                                ErrorOccurred(e.getMessage());
                            }
                        });
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            Log.e(TAG, "", e);
            ErrorOccurred(e.getMessage());
        }
    }

    public interface Callback {
        void onSuccess(String response);
    }
}