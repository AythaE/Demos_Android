/*
 * Archivo: CheckSessionActivity.java 
 * Proyecto: Demos_Android
 * 
 * Autor: Aythami Estévez Olivas
 * Email: aythae@gmail.com
 * Fecha: 04-jul-2016
 * Repositorio GitHub: https://github.com/AythaE/Demos_Android
 */
package es.usal.tfg.demos;

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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.koushikdutta.async.http.AsyncSSLSocketMiddleware;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;


/**
 * Clase CheckSessionActivity encargada de mostrar una Splash Screen de carga
 * nada más lanzar la aplicación, durante ella busca si existe un token de 
 * sesion en el dispositivo y en caso de ser así lo manda al servidor, si este
 * es autenticado inicia sesión directamente en esa campaña entrando en
 * {@link MainActivity}, si no lo es o no existe token previo pasa a 
 * {@link LoginActivity}
 */
public class CheckSessionActivity extends AppCompatActivity {

	 //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    /** Response devuelto por la conexión al servidor. */
    private static Response<String> checkSessionResp = null;
    
    /** The connection timeout en segundos. */
    private static int CONNECTION_TIMEOUT = 5;

    /** The token file. */
    private static File tokenFile ;
    
    /** The campaign name. */
    private static String campaignName;
    
	/**
	 * Crea la actividad cargando la interfaz, el archivo token y lanzando la 
	 * tarea asincrona de autenticación
	 * @see android.support.v7.app.AppCompatActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_session);
		Log.d(MainActivity.TAG, "Entrando en checkSession");

		tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");

		UserAuthenticateTask authTask = new UserAuthenticateTask();
		authTask.execute((Void) null);
	}

    /**
     * Represents an asynchronous authentification task used to authenticate
     * the user.
     */
    public class UserAuthenticateTask extends AsyncTask<Void, Void, Void> {
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Void doInBackground(Void... params) {
            checkSessionToken();
            return null;
        }
        
        /**
         * Comprueba el token de sesión en segundo plano, para ello lanza una 
         * petición a authenticate_token en el servidor con el contenido del
         * fichero token.
         * 
         */
        private void checkSessionToken() {




            if (tokenFile.exists()) {
                Log.d(MainActivity.TAG, "Token en: " + tokenFile.getAbsolutePath());
                String serverAuthenticate = MainActivity.SERVER_ADDR + "/campaign/authenticate_token";
                trustServerCertificate(CheckSessionActivity.this);


                try {

                    BufferedReader br = new BufferedReader(new FileReader(tokenFile));
                    String line;
                    String token=null, campaña=null;
                    try {
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
                    } catch(Exception e){
                    	Log.d(MainActivity.TAG, "Error leyendo el archivo token");
                    } finally {
						if (br!= null) {
							br.close();
						}
					}

                    String campaña64 = Base64.encodeToString(campaña.getBytes("UTF-8"), Base64.NO_WRAP);
                    campaignName = campaña;
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
                finish();
            }
        }

        /**
         * Comprueba la respuesta del servidor usando el atributo 
         * {@link CheckSessionActivity#checkSessionResp}. Si la respuesta es 
         * correcta se pasa a {@link MainActivity}, en caso contrario se 
         * muestra el error en el Log de la aplicación, se borra el token
         * por ser invalido y se va a {@link LoginActivity}
         *
         * @param e posible excepcion producida en la conexión
         */
        private void checkResponse(Exception e) {
            if (e != null || (checkSessionResp != null && checkSessionResp.getHeaders().code() != 200)) {

                Log.d(MainActivity.TAG, "Respuesta incorrecta");
               

                if (e != null) {
                    e.printStackTrace();
                }
                if (checkSessionResp != null) {
                    Log.d(MainActivity.TAG + " response message", checkSessionResp.getHeaders().message());
                    try{
                        byte[] datos = Base64.decode(checkSessionResp.getResult(), Base64.NO_WRAP);
                        Log.d(MainActivity.TAG, new String(datos));
                    } catch (Exception e2){
                        Log.d(MainActivity.TAG, checkSessionResp.getResult());
                    }
                }

                if(!tokenFile.delete()){
                    Log.d(MainActivity.TAG, "Error borrando token");
                }
                Intent intent = new Intent(CheckSessionActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Log.d(MainActivity.TAG, "Respuesta correcta");
                Log.d(MainActivity.TAG, campaignName);
                Intent intent = new Intent(CheckSessionActivity.this, MainActivity.class);
                intent.putExtra("campaignName", campaignName);

                startActivity(intent);
                 finish();
            }
        }


    }




    /**
     * Enseña a la aplicación a confiar en los certificados firmados con el 
     * certificado raiz de la autoridad de certificación creada en el servidor,
     * dicho certificado raiz, que es autofirmado, se encuentra en el directorio
     * raw.
     * 
     * <p>
     * Por seguridad se invoca antes de cada conexion al servidor, debido a que
     *  aunque se haya confiado en el certificado es posible que pierda al pasar
     *  a segundo plano la aplicación por ejemplo.
     *
     * @param context contexto que quiere confiar en el certificado
     */
    public static void trustServerCertificate(Context context) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            //Load cert file stored in \app\src\main\res\raw
            InputStream caInput = context.getResources().openRawResource(R.raw.server_ca); 

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
            
            AsyncSSLSocketMiddleware sslMiddleWare = Ion.getDefault(context).getHttpClient().getSSLSocketMiddleware();
            sslMiddleWare.setTrustManagers(wrappedTrustManagers);
            sslMiddleWare.setSSLContext(sslContext);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    

    /**
     * Gets the wrapped trust managers.
     *
     * @param trustManagers the trust managers
     * @return the wrapped trust managers
     */
    public static TrustManager[] getWrappedTrustManagers(TrustManager[] trustManagers) {
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


    
}
