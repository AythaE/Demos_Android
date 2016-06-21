package es.usal.tfg.demos;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.koushikdutta.async.http.AsyncSSLSocketMiddleware;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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

public class CheckSessionActivity extends AppCompatActivity {

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    private static Response<String> checkSessionResp = null;
    private static int CONNECTION_TIMEOUT = 20;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_session);
        Log.d(MainActivity.TAG, "Entrando en checkSession");



        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    protected void onResume() {
        super.onResume();

        UserAuthenticateTask authTask = new UserAuthenticateTask();
        authTask.execute((Void) null);
    }


    /**
     * Represents an asynchronous authentification task used to register
     * the user.
     */
    public class UserAuthenticateTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            checkSessionToken();
            return null;
        }
        private void checkSessionToken() {


            File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");


            if (tokenFile.exists()) {
                Log.d(MainActivity.TAG, "Token en: " + tokenFile.getAbsolutePath());
                String serverAuthenticate = MainActivity.SERVER_ADDR + "/campaign/authenticate_token";
                trustServerCertificate();


                try {

                    BufferedReader br = new BufferedReader(new FileReader(tokenFile));
                    String line;
                    String token=null, campaña=null;
                    while ((line = br.readLine()) != null){
                        if (campaña == null)
                        {
                            campaña = new String(line);
                        }
                        else {
                            if (token == null)
                                token = new String(line);
                        }
                        Log.d(MainActivity.TAG, line);
                    }


                    String campaña64 = Base64.encodeToString(campaña.getBytes("UTF-8"), Base64.NO_WRAP);

                    Log.d(MainActivity.TAG, "Token definitivo: " +token);
                    checkSessionResp = Ion.with(CheckSessionActivity.this)
                            .load(serverAuthenticate)
                            .setTimeout(CONNECTION_TIMEOUT * 1000)
                            .setLogging(MainActivity.TAG + " checkSession", Log.DEBUG)
                            .setBodyParameter("token", token)
                            .setBodyParameter("campaign", campaña64)
                            .asString()
                            .withResponse()
                            .get();

                    checkResponse(null);
                } catch (InterruptedException e) {
                    checkResponse(e);
                } catch (ExecutionException e) {
                    checkResponse(e);
                } catch (UnsupportedEncodingException e) {
                    checkResponse(e);
                } catch (FileNotFoundException e) {
                    checkResponse(e);
                } catch (IOException e) {
                    checkResponse(e);
                }
                return;
            }
            else{
                Log.d(MainActivity.TAG, "Archivo no existe");
                Intent intent = new Intent(CheckSessionActivity.this, LoginActivity.class);
                startActivity(intent);
                //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
                finish();
            }
        }

        private void checkResponse(Exception e) {
            if (e != null || (checkSessionResp != null && checkSessionResp.getHeaders().code() != 200)) {

                Log.d(MainActivity.TAG, "Respuesta incorrecta");
                //TODO leer error response

                if (e != null) {
                    e.printStackTrace();
                }
                if (checkSessionResp != null) {
                    Log.d(MainActivity.TAG + " response message", checkSessionResp.getHeaders().message());
                    byte[] datos = Base64.decode(checkSessionResp.getResult(), Base64.NO_WRAP);
                    Log.d(MainActivity.TAG, new String(datos));
                }

                Intent intent = new Intent(CheckSessionActivity.this, LoginActivity.class);
                startActivity(intent);
                //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
                finish();
            } else {
                Log.d(MainActivity.TAG, "Respuesta correcta");
                Intent intent = new Intent(CheckSessionActivity.this, MainActivity.class);
                startActivity(intent);
                //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
                finish();
            }
        }


    }




    private void trustServerCertificate() {
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
                            if (certs != null && certs.length > 0) {
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
                            if (certs != null && certs.length > 0) {
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


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "CheckSession Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://es.usal.tfg.demos/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "CheckSession Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://es.usal.tfg.demos/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
