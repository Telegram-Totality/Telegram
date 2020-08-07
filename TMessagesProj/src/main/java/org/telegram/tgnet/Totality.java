package org.telegram.tgnet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Totality {
    public static class TotalityException extends Exception{
        TotalityException(String s){
            super(s);
        }
    }

    public static void SetAddress(String endpoint, int id, String address) throws TotalityException {
        String url = String.format("%s/tg/%s",
                endpoint, id
        );
        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody form = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("address", address)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(form)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.code() != 200 || response.body() == null) {
                throw new TotalityException("Something went wrong");
            }
        } catch (IOException e) {
            throw new TotalityException("Something went wrong");
        }
    }

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