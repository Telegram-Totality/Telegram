package org.telegram.tgnet;

import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.ui.Components.ChatActivityEnterView;

import java.io.IOException;
import java.lang.ref.WeakReference;

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

    public static class SendEthereumTransaction extends AsyncTask<Void, Void, Void> {

        private WeakReference<ChatActivityEnterView> activity;
        private String endpoint;
        private String pk;
        private String hash;
        private TotalityException e;
        private WeakReference<ChatActivityEnterView> v;

        public SendEthereumTransaction(String endpoint, String pk, String hash, ChatActivityEnterView v) {
            this.endpoint = endpoint;
            this.pk = pk;
            this.hash = hash;
            this.v = new WeakReference<>(v);
        }

        @Override
        protected Void doInBackground(Void... s) {
            try {
                JSONObject data = Totality.GetContractCall(this.endpoint, this.hash);
            } catch (TotalityException e) {
                this.e = e;
            }
            // if this.e --> upload failed result

            //do transaction
            //upload result
            return null;
        }

        @Override
        protected void onPostExecute(Void s) {
            if (this.v.get() != null && this.e != null){
                // can be used for other purposes as well.
                // e.g. you transferred 2 dai.
                // BUT maybe better practice to let the bot handle failed transactions
                // as the tgtotal- prefix can be used by other bots as well
                Toast.makeText(v.get().getContext(), "A transaction failed", Toast.LENGTH_LONG).show();
            }
            super.onPostExecute(s);
        }
    }


    public static JSONObject GetContractCall(String endpoint, String hash) throws TotalityException {
        String url = String.format("%s/call/%s",
                endpoint, hash
        );
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() != 200 || response.body() == null) {
                throw new TotalityException("Something went wrong");
            }
            return new JSONObject(response.body().string());
        } catch (JSONException e) {
            throw new TotalityException("Something went wrong");
        } catch (IOException e) {
            throw new TotalityException("Something went wrong");
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

    public static String GetAddress(String endpoint, int id) throws TotalityException {
        String url = String.format("%s/tg/%s",
                endpoint, id
        );
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() == 404){
                return null;
            }
            if (response.code() != 200 || response.body() == null) {
                throw new TotalityException("Something went wrong");
            }
            JSONObject j = new JSONObject(response.body().string());
            return j.getString("address");
        } catch (JSONException e) {
            throw new TotalityException("Something went wrong");
        } catch (IOException e) {
            throw new TotalityException("Something went wrong");
        }
    }
}