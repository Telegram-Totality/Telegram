package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Totality;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class EthereumSettingsActivity extends BaseFragment {

    private EditTextBoldCursor publicKeyField;
    private EditTextBoldCursor privateKeyField;
    private View doneButton;
    private TextView checkTextView;
    private TextView helpTextViewPublicKey;
    private TextView helpTextViewPrivateKey;
    private AlertDialog.Builder dlgAlert;

    static class SetPublicKeyTask extends AsyncTask<String, Void, Void> {

        private WeakReference<EthereumSettingsActivity> activity;
        private String endpoint;
        private TLRPC.User user;
        private Totality.TotalityException e;
        private SharedPreferences.Editor edit;

        SetPublicKeyTask(Context context, EthereumSettingsActivity activity, SharedPreferences.Editor edit) {
            this.activity = new WeakReference<>(activity);
            this.endpoint = context.getResources().getString(R.string.TOTALITY_ENDPOINT);
            this.user = MessagesController.getInstance(this.activity.get().currentAccount)
                    .getUser(UserConfig.getInstance(this.activity.get().currentAccount)
                            .getClientUserId());
            this.edit = edit;
        }

        @Override
        protected void onPostExecute(Void s) {
            EthereumSettingsActivity activity = this.activity.get();
            if (activity == null){
                super.onPostExecute(s);
                return;
            }
            if (this.e != null){
                activity.dlgAlert.setMessage("Something went wrong");
                activity.dlgAlert.show();
                super.onPostExecute(s);
                return;
            }
            this.edit.apply();
            super.onPostExecute(s);
        }

        protected Void doInBackground(String... address) {
            try {
                Totality.SetAddress(this.endpoint, this.user.id, address[0]);
            } catch (Totality.TotalityException e) {
                this.e = e;
            }
            return null;
        }
    }

    static class GetPublicKeyTask extends AsyncTask<Integer, Void, String> {

        private WeakReference<EthereumSettingsActivity> activity;
        private String endpoint;
        private TLRPC.User user;
        private Totality.TotalityException e;

        GetPublicKeyTask(Context context, EthereumSettingsActivity activity) {
            this.activity = new WeakReference<>(activity);
            this.endpoint = context.getResources().getString(R.string.TOTALITY_ENDPOINT);
            this.user = MessagesController.getInstance(this.activity.get().currentAccount)
                            .getUser(UserConfig.getInstance(this.activity.get().currentAccount)
                            .getClientUserId());
        }

        @Override
        protected void onPostExecute(String s) {
            EthereumSettingsActivity activity = this.activity.get();
            if (activity == null) {
                super.onPostExecute(s);
                return;
            }
            if (this.e != null) {
                activity.dlgAlert.setMessage("Something went wrong");
                activity.dlgAlert.show();
                activity.finishFragment();
                super.onPostExecute(s);
                return;
            }
            activity.publicKeyField.setHint("Public key (0x..)");
            activity.publicKeyField.setText(s);
            activity.publicKeyField.setSelection(activity.publicKeyField.length());
            activity.publicKeyField.setFocusableInTouchMode(true);
            if (s == null) {
                activity.publicKeyField.requestFocus();
            }
            super.onPostExecute(s);
        }

        protected String doInBackground(Integer... id) {
            try {
                return Totality.GetAddress(this.endpoint, this.user.id);
            } catch (Totality.TotalityException e) {
                this.e = e;
            }
            return null;
        }
    }

    private final static int done_button = 1;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Ethereum Key Storage");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    saveKeyStorage(context);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));
        doneButton.setContentDescription(LocaleController.getString("Done", R.string.Done));

        fragmentView = new LinearLayout(context);
        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        fragmentView.setOnTouchListener((v, event) -> true);

        FrameLayout fieldContainer = new FrameLayout(context);
        linearLayout.addView(fieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 24, 20, 0));

        dlgAlert  = new AlertDialog.Builder(context);
        dlgAlert.setTitle("Key management");
        dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss the dialog
                    }
                }
        );

        publicKeyField = new EditTextBoldCursor(context);
        publicKeyField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        publicKeyField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        publicKeyField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        publicKeyField.setBackgroundDrawable(Theme.createEditTextDrawable(context, false));
        publicKeyField.setMaxLines(2);
        publicKeyField.setPadding(AndroidUtilities.dp(LocaleController.isRTL ? 24 : 0), 0, AndroidUtilities.dp(LocaleController.isRTL ? 0 : 24), AndroidUtilities.dp(6));
        publicKeyField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        publicKeyField.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        publicKeyField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        publicKeyField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        InputFilter[] inputFilters = new InputFilter[1];
        inputFilters[0] = new InputFilter.LengthFilter(42) {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source != null && TextUtils.indexOf(source, '\n') != -1) {
                    privateKeyField.requestFocus();
                    return "";
                }
                CharSequence result = super.filter(source, start, end, dest, dstart, dend);
                if (result != null && source != null && result.length() != source.length()) {
                    Vibrator v = (Vibrator) getParentActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        v.vibrate(200);
                    }
                    AndroidUtilities.shakeView(checkTextView, 2, 0);
                }
                return result;
            }
        };
        publicKeyField.setFilters(inputFilters);
        publicKeyField.setMinHeight(AndroidUtilities.dp(36));
        publicKeyField.setHint("Loading public key...");
        publicKeyField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        publicKeyField.setCursorSize(AndroidUtilities.dp(20));
        publicKeyField.setCursorWidth(1.5f);
        publicKeyField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                doneButton.performClick();
                return true;
            }
            return false;
        });
        publicKeyField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkTextView.setText(String.format("%d", (42 - publicKeyField.length())));
            }
        });
        publicKeyField.setFocusable(false);
        new GetPublicKeyTask(context, this).execute();
        fieldContainer.addView(publicKeyField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 0, 4, 0));

        checkTextView = new TextView(context);
        checkTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        checkTextView.setText(String.format("%d", 42));
        checkTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        checkTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        fieldContainer.addView(checkTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT, 0, 4, 4, 0));

        helpTextViewPublicKey = new TextView(context);
        helpTextViewPublicKey.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        helpTextViewPublicKey.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText8));
        helpTextViewPublicKey.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        helpTextViewPublicKey.setText("Put your public key in this field. This will be publicly linked to your telegram account.");
        linearLayout.addView(helpTextViewPublicKey, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 10, 24, 0));

        privateKeyField = new EditTextBoldCursor(context);
        privateKeyField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        privateKeyField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        privateKeyField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        privateKeyField.setBackgroundDrawable(Theme.createEditTextDrawable(context, false));
        privateKeyField.setMaxLines(4);
        privateKeyField.setPadding(AndroidUtilities.dp(LocaleController.isRTL ? 24 : 0), 0, AndroidUtilities.dp(LocaleController.isRTL ? 0 : 24), AndroidUtilities.dp(6));
        privateKeyField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        privateKeyField.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        privateKeyField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        privateKeyField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        privateKeyField.setMinHeight(AndroidUtilities.dp(36));
        privateKeyField.setHint("Private key (64 characters)");
        privateKeyField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        privateKeyField.setCursorSize(AndroidUtilities.dp(20));
        privateKeyField.setCursorWidth(1.5f);
        InputFilter[] ip = new InputFilter[1];
        ip[0] = new InputFilter.LengthFilter(64){
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source != null && TextUtils.indexOf(source, '\n') != -1) {
                    doneButton.performClick();
                    return "";
                }
                CharSequence result = super.filter(source, start, end, dest, dstart, dend);
                if (result != null && source != null && result.length() != source.length()) {
                    Vibrator v = (Vibrator) getParentActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        v.vibrate(200);
                    }
                }
                return result;
            }
        };
        privateKeyField.setFilters(ip);
        privateKeyField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                doneButton.performClick();
                return true;
            }
            return false;
        });
        linearLayout.addView(privateKeyField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 10, 24, 0));

        helpTextViewPrivateKey = new TextView(context);
        helpTextViewPrivateKey.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        helpTextViewPrivateKey.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText8));
        helpTextViewPrivateKey.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        SharedPreferences userDetails = context.getSharedPreferences("userdetails", Context.MODE_PRIVATE);
        final String privateKey = userDetails.getString("eth_private_key", "");
        String helpText = "Private key is not set.";
        if(!privateKey.isEmpty()){
            int length = Math.min(privateKey.length(), 5);
            helpText = String.format("Private key set, starts with `%s`, length: %s", privateKey.substring(0 , length), privateKey.length());
        }

        helpTextViewPrivateKey.setText(AndroidUtilities.replaceTags(String.format("%s \nWARNING: put your private key here. This will be stored on the device.", helpText)));
        linearLayout.addView(helpTextViewPrivateKey, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 10, 24, 0));
        return fragmentView;
    }

    private void saveKeyStorage(Context context) {
        SharedPreferences userDetails = context.getSharedPreferences("userdetails", Context.MODE_PRIVATE);
        String privateKey = userDetails.getString("eth_private_key", "");
        final String privateKeyFieldValue = privateKeyField.getText().toString().replace("\n", "").trim();
        String expectedPubKey = null;

        if (!privateKeyFieldValue.isEmpty()){
            try {
                // Check if privatekey field is valid and save on disk
                expectedPubKey = Keys.toChecksumAddress(Credentials.create(privateKeyFieldValue).getAddress());
            } catch (Exception ignored) {
                dlgAlert.setMessage("ERROR: private key is invalid format, progress is not saved.");
                dlgAlert.create().show();
                finishFragment();
                return;
            }
            privateKey = privateKeyFieldValue;
        }
        else if(!privateKey.isEmpty()){
            expectedPubKey = Keys.toChecksumAddress(Credentials.create(privateKey).getAddress());
        }

        final String publicKey = Keys.toChecksumAddress(publicKeyField.getText().toString().replace("\n", ""));
        SharedPreferences.Editor edit = userDetails.edit();
        // If the private key is null, the expected pub key is also null
        if(expectedPubKey == null){
            edit.putBoolean("keys_setup", false);
            dlgAlert.setMessage("Your private key is not set");
            dlgAlert.create().show();
            edit.apply();
        }
        else if(!expectedPubKey.equals(publicKey)){
            edit.putBoolean("keys_setup", false);
            dlgAlert.setMessage("WARNING: private key does not match public address");
            dlgAlert.create().show();
            edit.apply();
        }else{
            new SetPublicKeyTask(context, this, edit).execute(publicKey);
            edit.putString("eth_private_key", privateKey);
            edit.putBoolean("keys_setup", true);
            finishFragment();
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(publicKeyField, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(publicKeyField, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        themeDescriptions.add(new ThemeDescription(publicKeyField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(publicKeyField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));

        themeDescriptions.add(new ThemeDescription(helpTextViewPrivateKey, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText8));

        themeDescriptions.add(new ThemeDescription(checkTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        return themeDescriptions;
    }
}
