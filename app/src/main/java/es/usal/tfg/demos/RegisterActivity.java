package es.usal.tfg.demos;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncSSLSocketMiddleware;
import com.koushikdutta.async.util.Charsets;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * A login screen that offers login via email/password.
 */
public class RegisterActivity extends AppCompatActivity {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserRegisterTask mRegisterTask = null;

    // UI references.
    private EditText mCampaignView;
    private EditText mPasswordView;
    private EditText mPasswordView2;
    private View mProgressView;
    private View mLoginFormView;

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    private static Response<String> registerResp = null;
    private static int CONNECTION_TIMEOUT = 20;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Set up the login form.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_register);
        setSupportActionBar(toolbar);
        mCampaignView = (EditText) findViewById(R.id.campaign_register);



        mPasswordView = (EditText) findViewById(R.id.password_register);

        mPasswordView2 = (EditText) findViewById(R.id.password_register_repeat);
        mPasswordView2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.register || id == EditorInfo.IME_NULL) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mLoginFormView = findViewById(R.id.scroll_register_form);
        mProgressView = findViewById(R.id.login_progress);

        Log.d(MainActivity.TAG, "Entrando en RegisterAtivity");
    }





    /**
     * Attempts to register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual register attempt is made.
     */
    private void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }

        // Reset errors.
        mCampaignView.setError(null);
        mPasswordView.setError(null);
        mPasswordView2.setError(null);

        // Store values at the time of the login attempt.
        String campaign = mCampaignView.getText().toString();
        String password = mPasswordView.getText().toString();
        String password2 = mPasswordView2.getText().toString();


        boolean cancel = false;
        View focusView = null;


        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }


        // Check for a valid password.
        if (TextUtils.isEmpty(password2)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView2;
            cancel = true;
        } else if (!password.equals(password2)) {
            mPasswordView2.setError(getString(R.string.error_passwords_not_equals));
            focusView = mPasswordView2;
            cancel = true;
        }

        // Check for a valid campaign address.
        if (TextUtils.isEmpty(campaign)) {
            mCampaignView.setError(getString(R.string.error_field_required));
            focusView = mCampaignView;
            cancel = true;
        } else if (!isCampaignValid(campaign)) {
            mCampaignView.setError(getString(R.string.error_invalid_campaign));
            focusView = mCampaignView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            //showProgress(true);
            mRegisterTask = new UserRegisterTask(campaign, password);
            mRegisterTask.execute((Void) null);
        }
    }

    private boolean isCampaignValid(String campaign) {
        //TODO: Replace this with your own logic

        return  campaign.length() >= 2 && campaign.matches("^[a-zA-Z0-9]+[a-zA-Z0-9\\._-]*$");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() >= 8 && password.matches("^[a-zA-Z0-9\\.\\*#%&()=+:;,<>_!?-]*$");
    }




    /**
     * Represents an asynchronous registration task used to register
     * the user.
     */
    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {

        private final String mCampaign;
        private final String mPassword;
        private final AlertDialog registerDialog;

        private String toastMessage;
        public boolean isRegisterSucceed() {
            return registerSucceed;
        }

        public void setRegisterSucceed(boolean regSuscceed) {
            this.registerSucceed = regSuscceed;
        }

        private boolean registerSucceed = false;

        UserRegisterTask(String campaign, String password) {
            mCampaign = campaign;
            mPassword = password;

            registerDialog = new AlertDialog.Builder(RegisterActivity.this).create();

            //TODO check al girar pantalla
            registerDialog.setView(getLayoutInflater().inflate(R.layout.uploading_dialog, null));
            registerDialog.setTitle(R.string.register_dialog_title);
            registerDialog.setCancelable(false);
            registerDialog.setCanceledOnTouchOutside(false);


        }

        @Override
        protected void onPreExecute() {
            registerDialog.show();
        }


        @Override
        protected Boolean doInBackground(Void... params) {


            String serverRegister = MainActivity.SERVER_ADDR + "/campaign/register";
            trustServerCertificate();




            try {
                String campaignB64 = Base64.encodeToString(mCampaign.getBytes("UTF-8"), Base64.NO_WRAP);
                String passwordB64 = Base64.encodeToString(mPassword.getBytes("UTF-8"), Base64.NO_WRAP);

                registerResp = Ion.with(RegisterActivity.this)
                        .load(serverRegister)
                        .setTimeout(CONNECTION_TIMEOUT * 1000)
                        .setLogging(MainActivity.TAG + " register", Log.DEBUG)
                        .setBodyParameter("campaign", campaignB64)
                        .setBodyParameter("password", passwordB64)
                        .asString()
                        .withResponse()
                        .get();

                checkResponse(null);
            } catch (InterruptedException e) {
               checkResponse(e);
            } catch (ExecutionException e) {
                checkResponse(e);
            }
            catch (UnsupportedEncodingException e) {
                checkResponse(e);
            }
            return isRegisterSucceed();
        }
        private void checkResponse(Exception e)
        {
            if (e != null || ( registerResp != null && registerResp.getHeaders().code() != 200)) {



                if (e!= null) {
                    e.printStackTrace();
                }
                if (registerResp != null ){
                    //TODO leer error response
                    Log.d(MainActivity.TAG + " response message",registerResp.getHeaders().message());

                    if ((400 <= registerResp.getHeaders().code()) &&
                        (500 > registerResp.getHeaders().code())) {

                        byte[] datos = Base64.decode(registerResp.getResult(), Base64.NO_WRAP);
                        toastMessage = new String(datos);
                    }
                    else
                    {
                        toastMessage = getString(R.string.toastWrongRegisterResult);
                    }
                }
                else
                {
                    toastMessage = getString(R.string.toastWrongRegisterResult);
                }
                setRegisterSucceed(false);
            } else {

                toastMessage = getString(R.string.toastCorrectRegisterResult);
                setRegisterSucceed(true);


            }
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            mRegisterTask = null;
            registerDialog.dismiss();

            if(isCancelled())
                cancelTask();
            else {

                Toast t = Toast.makeText(RegisterActivity.this, toastMessage, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v!=null){
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                if (success) {
                    String token = registerResp.getResult();
                    saveSessionToken(token);
                    Log.d(MainActivity.TAG, "registro correcto: " + token);
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
                    finish();
                } else {
                    String responseResult = new String(Base64.decode(registerResp.getResult(), Base64.NO_WRAP));
                    Log.d(MainActivity.TAG + " response message", responseResult);
                    Log.d(MainActivity.TAG, "registro incorrecto");
                }
            }
        }

        private void saveSessionToken(String token) {

            FileOutputStream fos = null;
            BufferedWriter bw = null;
            try {
                fos = openFileOutput(".token", Context.MODE_PRIVATE);

                bw = new BufferedWriter(new FileWriter(fos.getFD()));
                bw.write(mCampaign + "\n");
                bw.write(token);
                Log.d(MainActivity.TAG, "Token: "+ token+" escrito en "+getFilesDir().getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {

                try {
                    if (bw != null){
                        bw.flush();
                        bw.close();
                    }
                    if (fos!= null) {
                        fos.flush();
                        fos.close();
                    }

                } catch (IOException e) {}


            }

        }

        /**
         * @reference http://stackoverflow.com/questions/11165860/asynctask-oncancelled-not-being-called-after-canceltrue/11166026#11166026
         *
         */


        @Override
        protected void onCancelled() {
            cancelTask();
        }

        private void cancelTask()
        {
            mRegisterTask = null;
            registerSucceed = false;
            registerDialog.dismiss();
        }


    }



    private void trustServerCertificate()
    {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            //Load cert file stored in \app\src\main\res\raw
            InputStream caInput = getResources().openRawResource(R.raw.server_ca); //TODO Seleccionar el certificado correcto

            Certificate ca = cf.generateCertificate(caInput);
            caInput.close();
            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            TrustManager[] wrappedTrustManagers = getWrappedTrustManagers(tmf.getTrustManagers());

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, wrappedTrustManagers, null);
            //sslContext.init(null, tmf.getTrustManagers(), null);

            AsyncSSLSocketMiddleware sslMiddleWare = Ion.getDefault(this).getHttpClient().getSSLSocketMiddleware();
            sslMiddleWare.setTrustManagers(wrappedTrustManagers);
            //sslMiddleWare.setHostnameVerifier(getHostnameVerifier());
            sslMiddleWare.setSSLContext(sslContext);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
                // or the following:
                // HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                // return hv.verify("www.yourserver.com", session);
            }
        };
    }
    private TrustManager[] getWrappedTrustManagers(TrustManager[] trustManagers) {
        final X509TrustManager originalTrustManager = (X509TrustManager) trustManagers[0];
        return new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return originalTrustManager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        try {
                            if (certs != null && certs.length > 0){
                                certs[0].checkValidity();
                            } else {
                                originalTrustManager.checkClientTrusted(certs, authType);
                            }
                        } catch (CertificateException e) {
                            Log.w("checkClientTrusted", e.toString());
                        }
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        try {
                            if (certs != null && certs.length > 0){
                                certs[0].checkValidity();
                            } else {
                                originalTrustManager.checkServerTrusted(certs, authType);
                            }
                        } catch (CertificateException e) {
                            Log.w("checkServerTrusted", e.toString());
                        }
                    }
                }
        };
    }
}

