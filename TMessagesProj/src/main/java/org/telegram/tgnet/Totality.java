package org.telegram.tgnet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.android.exoplayer2.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.R;
import org.telegram.ui.EthereumSettingsActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Totality {
    public static String GetAddress(String endpoint, int id) {
        String url = String.format("%s/tg/%s",
                endpoint, id
        );
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() != 200 || response.body() == null) {
                return null;
            }
            JSONObject j = new JSONObject(response.body().string());
            return j.getString("address");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}