package org.telegram.tgnet;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class Transaction extends Contract {

    protected Transaction(String contractBinary, String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) {
        super(contractBinary, contractAddress, web3j, transactionManager, gasProvider);
    }

    protected String Send(String func_name, JSONArray inputs, JSONObject params, BigInteger weiValue) throws Totality.TotalityException, JSONException {
        List<String> solidityInputTypes = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();
        List<String> solidityOutputTypes = new ArrayList<>();
        for (int i = 0; i < inputs.length(); i++) {
            JSONObject input = inputs.getJSONObject(i);
            String name = input.getString("name");
            String type = input.getString("type");
            solidityInputTypes.add(type);
            arguments.add(params.get(name));
        }
        final Function function;
        try {
            function = FunctionEncoder.makeFunction(
                    func_name,
                    solidityInputTypes,
                    arguments,
                    solidityOutputTypes
            );

            if (weiValue != null)
                executeRemoteCallTransaction(function, weiValue).send();
            executeRemoteCallTransaction(function).send();
        } catch (TransactionException e) {
            return e.getTransactionHash().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Totality.TotalityException("Something went wrong when sending the transaction");
        }
        return null;
    }
}

public class Totality {
    public static class TotalityException extends Exception{
        TotalityException(String s){
            super(s);
        }
    }

    public static class SendEthereumTransaction extends AsyncTask<Void, Void, Void> {

        private String endpoint;
        private String infuraToken;
        private int user_id;
        private String pk;
        private String hash;
        private Exception e;
        private Exception ioException;
        private WeakReference<ChatActivityEnterView> v;

        private SendMessagesHelper helper;
        private MessageObject messageObject;
        private TLRPC.KeyboardButton button;
        private ChatActivity parentFragment;
        private SharedPreferences preferences;

        public SendEthereumTransaction(
                String endpoint, String infuraToken, int id, String pk, String hash, ChatActivityEnterView v,
                SendMessagesHelper helper, MessageObject messageObject, TLRPC.KeyboardButton button, ChatActivity parentFragment,
                SharedPreferences preferences) {
            this.endpoint = endpoint;
            this.infuraToken = infuraToken;
            this.user_id = id;
            this.pk = pk;
            this.hash = hash;
            this.v = new WeakReference<>(v);

            this.helper = helper;
            this.messageObject = messageObject;
            this.button = button;
            this.parentFragment = parentFragment;
            this.preferences = preferences;
        }

        @Override
        protected Void doInBackground(Void... s) {
            try {
                String expectedPub = Keys.toChecksumAddress(Credentials.create(this.pk).getAddress());
                String addr = Totality.GetAddress(this.endpoint, this.user_id);
                if (!expectedPub.equals(addr)) {
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putBoolean("keys_setup", false);
                    edit.apply();
                    throw new TotalityException("Keys are not setup");
                }
                if (!Totality.CreateResult(this.endpoint, this.hash)) {
                    return null;
                }
                JSONObject data = Totality.GetContractCall(this.endpoint, this.hash);
                String address = data.getString("address");
                String funcName = data.getString("function");
                JSONObject params = data.getJSONObject("params");
                JSONObject abi = data.getJSONObject("abi");
                JSONArray inputs = abi.getJSONArray("inputs");

                int network = data.getInt("network");
                String endpoint;
                if (network == 1) {
                    endpoint = "https://mainnet.infura.io/v3/" + this.infuraToken;
                }
                else if (network == 3){
                    endpoint = "https://ropsten.infura.io/v3/" + this.infuraToken;
                }
                else {
                    throw new TotalityException("Network not supported");
                }
                Web3j web3 = Web3j.build(new HttpService(endpoint));
                PollingTransactionReceiptProcessor processor = new PollingTransactionReceiptProcessor(web3, 0, 0);
                TransactionManager txManager = new FastRawTransactionManager(web3, Credentials.create(this.pk), processor);
                Transaction tx = new Transaction(null, address, web3, txManager, new StaticGasProvider(
                        new BigInteger(data.getString("gasPrice")),
                        new BigInteger(data.getString("gasLimit"))
                ));
                String hash;
                if (abi.getBoolean("payable"))
                    hash = tx.Send(funcName, inputs, params, new BigInteger(data.getString("weiValue")));
                else
                    hash = tx.Send(funcName, inputs, params, null);

                Totality.SetResult(this.endpoint, this.hash, true, "success", hash);
            } catch (TotalityException | JSONException e) {
                e.printStackTrace();
                this.e = e;
                try {
                    Totality.SetResult(this.endpoint, this.hash, false, e.getMessage(), null);
                } catch (TotalityException ex) {
                    ex.printStackTrace();
                }
                return null;
            } catch (Exception e) {
                this.ioException = e;
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void s) {
            if (this.v.get() != null){
                // can be used for other purposes as well.
                // e.g. you transferred 2 dai.
                // BUT maybe better practice to let the bot handle failed transactions
                // as the tgtotal- prefix can be used by other bots as well
                if (this.e != null) {
                    e.printStackTrace();
                    Toast.makeText(v.get().getContext(), "A transaction failed to execute", Toast.LENGTH_LONG).show();
                }
                else if (this.ioException != null) {
                    ioException.printStackTrace();
                    Toast.makeText(v.get().getContext(), "Could not connect to totality services", Toast.LENGTH_LONG).show();
                }
                this.helper.sendCallback(true, messageObject, button, parentFragment);
            }
            super.onPostExecute(s);
        }
    }

    public static boolean CreateResult(String endpoint, String hash) throws TotalityException {
        String url = String.format("%s/result/%s",
                endpoint, hash
        );
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "none"))
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() >= 500){
                throw new TotalityException("Something went wrong");
            }
            return response.code() == 200;
        } catch (IOException e) {
            throw new TotalityException("Something went wrong");
        }
    }

    public static void SetResult(String endpoint, String hash, boolean success, String message, String tx) throws TotalityException {
        String url = String.format("%s/result/%s",
                endpoint, hash
        );
        try {
            JSONObject j = new JSONObject();
            j.put("success", success);
            j.put("message", message);
            j.put("tx", tx);
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), j.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.code() != 200) {
                throw new TotalityException("Something went wrong");
            }
        } catch (JSONException | IOException e) {
            throw new TotalityException("Something went wrong");
        }
    }

    public static JSONObject GetContractCall(String endpoint, String hash) throws TotalityException, IOException {
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
            if (response.code() != 200) {
                throw new TotalityException("Something went wrong");
            }
        } catch (IOException e) {
            throw new TotalityException("Something went wrong");
        }
    }

    private static String address;
    private static long last_updated;

    public static String GetAddress(String endpoint, int id) throws TotalityException {
        if (Totality.address != null) {
            final long now = System.currentTimeMillis();
            if (now - Totality.last_updated < 20*1000) { // 20 seconds cache
                return Totality.address;
            }
            Totality.last_updated = now;
        }

        String url = String.format("%s/tg/%s",
                endpoint, id
        );
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() == 404 && response.body() != null && response.body().string().contains("id not found")){
                return null;
            }
            if (response.code() != 200 || response.body() == null) {
                throw new TotalityException("Something went wrong");
            }
            JSONObject j = new JSONObject(response.body().string());
            Totality.address = j.getString("address");
            return Totality.address;
        } catch (JSONException e) {
            throw new TotalityException("Something went wrong");
        } catch (IOException e) {
            throw new TotalityException("Something went wrong");
        }
    }
}