/*
 * Archivo: LoginActivity.java 
 * Proyecto: Demos_Android_Doc
 * 
 * Autor: Aythami Estévez Olivas
 * Email: aythae@gmail.com
 * Fecha: 04-jul-2016
 * Repositorio GitHub: https://github.com/AythaE/Demos_Android
 */
package es.usal.tfg.demos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Clase LoginActivity que controla una ventana con los campos necesarios para
 * iniciar sesión en una campaña .
 */
public class LoginActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    /**
     * Instancia de la tarea que realizara la peticion de login de manera 
     * asíncrona.
     */
    private static UserLoginTask mAuthTask = null;

    // UI references.
   
    /** El campo de texto de la campaña. */
    private EditText mCampaignView;
    
    /** El campo de texto de la contraseña. */
    private EditText mPasswordView;
    
    /** The navigation view. */
    private NavigationView navigationView;

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
   
    /** Response devuelto por la conexión al servidor. */
    private static Response<String> loginResp = null;
    
    /** The connection timeout, 20 segundos. */
    private static int CONNECTION_TIMEOUT = 20;

    /** El {@link AlertDialog} de login. */
    private static AlertDialog loginDialog;

    /**
     * Flag para controlar cuando se muestra el dialogo de login para poder
     * mostrarlo en caso de que se gire la pantalla, lo que re-instancia esta
     * clase perdiendose todos los campos no estáticos.
     */
    private static boolean showingLoginDialog = false;

    /**
     * Carga y configura la UI
     * @see android.support.v7.app.AppCompatActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(MainActivity.TAG, "Entrando en LoginActivity");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_login);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_login);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank

                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                super.onDrawerClosed(drawerView);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view_login);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up the login form.
        mCampaignView = (EditText) findViewById(R.id.campaign_login);


        mPasswordView = (EditText) findViewById(R.id.password_login);
       

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.preregister_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchRegister();
            }
        });


        loginDialog = new AlertDialog.Builder(LoginActivity.this).create();

        //TODO check al girar pantalla
        loginDialog.setView(getLayoutInflater().inflate(R.layout.uploading_dialog, null));
        loginDialog.setTitle(R.string.login_dialog_title);
        loginDialog.setCancelable(false);
        loginDialog.setCanceledOnTouchOutside(false);

    }
    
    /** 
     * Al presionar el boton back si el {@link DrawerLayout} esta abierto lo
     * cierra, en caso contrario hace lo que haría por defecto
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_login);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Invocado cuando se pulsa en en el boton de registrar campaña, 
     * simplemente cambia a dicha actividad.
     */
    private void launchRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
        
    }

    /**
     * Intenta iniciar sesión en la campaña especificada en el formulario de 
     * login, comprueba los posibles errores en los campos y si todo es correcto
     * instancia y lanza {@link LoginActivity#mAuthTask} que efectúa la conexión
     * con el servidor.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mCampaignView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String campaing = mCampaignView.getText().toString();
        String password = mPasswordView.getText().toString();

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

        // Check for a valid campaign.
        if (TextUtils.isEmpty(campaing)) {
            mCampaignView.setError(getString(R.string.error_field_required));
            focusView = mCampaignView;
            cancel = true;
        } else if (!isCampaignValid(campaing)) {
            mCampaignView.setError(getString(R.string.error_invalid_campaign));
            focusView = mCampaignView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.setFocusable(true);
            focusView.requestFocus();
        } else {
           
            mAuthTask = new UserLoginTask(campaing, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Comprueba si una campaña es valida, esta se considera valida si contiene
     *  dos o más caracters alfa numéricos, o los signos ., _ y -.
     *
     * @param campaign cadena de caracteres introducida en 
     * {@link LoginActivity#mCampaignView} a comprobar
     * @return true, si la campa es valida
     */
	private boolean isCampaignValid(String campaign) {

		return campaign.length() >= 2 && campaign.matches("^[a-zA-Z0-9]+[a-zA-Z0-9\\._-]*$");
	}

    /**
     * Comprueba si una contraseña es valida, esta se considera valida si 
     * contiene al menos 8 caracteres que pueden ser: letras (mayusculas o 
     * minusculas), numeros, puntos, asteriscos, #,...
     *
     *
     * @param password la contraseña introducida en 
     * {@link LoginActivity#mPasswordView}
     * @return true, if is password valid
     */
    private boolean isPasswordValid(String password) {
       
        return password.length() >= 8 && password.matches("^[a-zA-Z0-9\\.\\*#%&()=+:;,<>_!?-]*$");
    }

    /** 
     * Controla las acciones a tomar al seleccionar los distintos campos de la 
     * {@link NavigationView} que permiten cambiar entre actividades
     * @see android.support.design.widget.NavigationView.OnNavigationItemSelectedListener#onNavigationItemSelected(android.view.MenuItem)
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        boolean retVal =true;

        if (id == R.id.login_drawer_item) {

        } else if (id == R.id.register_drawer_item) {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);

        } else if (id == R.id.instructions_drawer_item) {
            Intent intent = new Intent(LoginActivity.this, InstructionsActivity.class);

            startActivity(intent);
        } else if (id == R.id.main_drawer_item) {
            File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");


            if (!tokenFile.exists()) {
                Toast t = Toast.makeText(LoginActivity.this, R.string.toastMainForbidden, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v!=null){
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                retVal = false;
            }
            else{

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);

                startActivity(intent);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_login);
        drawer.closeDrawer(GravityCompat.START);
        return retVal;
    }

    /**
     * Al entrar en primer plano selecciona el campo Login Item de la
     * {@link NavigationView}, además comprueba si se estaba mostrando el
     * dialogo {@link LoginActivity#loginDialog}, en cuyo caso lo
     * despliega.
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.login_drawer_item);
        if (showingLoginDialog){
            loginDialog.show();
            showingLoginDialog = false;
        }
    }



    /**
     * Invocado al destruir la actividad, comprueba si se estaba mostrando el
     * dialogo {@link LoginActivity#loginDialog}, en cuyo caso lo
     * fija el flag {@link LoginActivity#showingLoginDialog} a true para que
     * se muestre cuando vuelva y cierra el dialogo actual.
     * @see android.support.v7.app.AppCompatActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {

        if (loginDialog.isShowing()) {
            loginDialog.dismiss();
            showingLoginDialog = true;
        }

        super.onDestroy();
    }

    /**
     * Representa la tarea encargada de conexión al servidor, esta lanza una 
     * peticion de login al servidor y comprueba su respuesta. 
     * 
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        /** La campaña. */
        private final String mCampaign;
        
        /** La contraseña. */
        private final String mPassword;

        /** El mensaje {@link Toast}. */
        private String toastMessage;



        /**
         * Checks if is login succeed.
         *
         * @return true, if is login succeed
         */
        public boolean isLoginSucceed() {
            return loginSucceed;
        }

        /**
         * Sets the login succeed.
         *
         * @param logSucceed the new login succeed
         */
        public void setLoginSucceed(boolean logSucceed) {
            this.loginSucceed = logSucceed;
        }

        /** Flag para determinar si el login ha sido correcto o no. */
        private boolean loginSucceed = false;

        /**
         * Crea una nueva instancia de esta clase.
         *
         * @param campaign the campaign
         * @param password the password
         */
        UserLoginTask(String campaign, String password) {
            mCampaign = campaign;
            mPassword = password;



        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            loginDialog.show();
        }

        /**
         * Realiza la conexion al servidor quedandose bloqueado hasta que este
         * responda. Para ello codifica en Base64 la campaña y la contraseña y
         * las envia generando un formulario application/x-www-form-urlencoded
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Boolean doInBackground(Void... params) {


            String serverRegister = MainActivity.SERVER_ADDR + "/campaign/login";
            CheckSessionActivity.trustServerCertificate(LoginActivity.this);


            try {

                String campaignB64 = Base64.encodeToString(mCampaign.getBytes("UTF-8"), Base64.NO_WRAP);
                String passwordB64 = Base64.encodeToString(mPassword.getBytes("UTF-8"), Base64.NO_WRAP);

                loginResp = Ion.with(LoginActivity.this)
                        .load(serverRegister)
                        .setTimeout(CONNECTION_TIMEOUT * 1000)
                        .setLogging(MainActivity.TAG + " login", Log.DEBUG)
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
            } catch (UnsupportedEncodingException e){
                checkResponse(e);
            }
            return isLoginSucceed();
        }

        
        /**
         * Comprueba la respuesta del servidor usando el atributo 
         * {@link LoginActivity#loginResp}. Si la respuesta es correcta fija el
         *  flag {@link UserLoginTask#loginSucceed} a true, en caso contrario 
         * a false. Tambien fija el mensaje que mostrará en un {@link Toast} al
         * usuario indicandole el resultado del login
         *
         * @param e posible excepcion producida en la conexión
         */
        private void checkResponse(Exception e) {

            if (e != null || (loginResp != null && loginResp.getHeaders().code() != 200)) {



                if (e != null) {
                    e.printStackTrace();
                }
                if (loginResp != null) {

                    
                    Log.d(MainActivity.TAG + " response message", loginResp.getHeaders().message());
                    if((400 <= loginResp.getHeaders().code()) &&  (500 > loginResp.getHeaders().code())) {
                    	try {
                            byte [] datos = Base64.decode( loginResp.getResult(),Base64.NO_WRAP);
                            toastMessage = new String(datos);
                    	} catch (Exception e2){
                    		toastMessage = getString(R.string.toastWrongLoginResult);
                    	}
                    }
                    else {
                        toastMessage = getString(R.string.toastWrongLoginResult);
                    }
                }else {
                    toastMessage = getString(R.string.toastWrongLoginResult);
                }
                setLoginSucceed(false);
            } else {

                toastMessage = getString(R.string.toastCorrectLoginResult);
                setLoginSucceed(true);
            }
        }

        /**
         * Hace desaparecer el dialogo {@link UserLoginTask#loginDialog}, 
         * muestra un {@link Toast} con el resultado de la operación al usuario
         * , si el flag {@link UserLoginTask#loginSucceed} es true guarda el 
         * token recibido en el método 
         * {@link UserLoginTask#saveSessionToken(String)} y pasa a 
         * {@link MainActivity}, en caso contrario muestra el error en Log 
         * y finaliza.
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            loginDialog.dismiss();

            if (isCancelled())
                cancelTask();
            else {
                Toast t = Toast.makeText(LoginActivity.this, toastMessage, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v!=null){
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                if (success) {
                    String token = loginResp.getResult();
                    saveSessionToken(token);
                    Log.d(MainActivity.TAG, "login correcto: " + token);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("campaignName", mCampaign);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        String responseResult = new String(Base64.decode(loginResp.getResult(), Base64.NO_WRAP));
                        Log.d(MainActivity.TAG + " response message", responseResult);
                    } catch (Exception e){}
                    Log.d(MainActivity.TAG, "login incorrecto");

                }
            }
        }

        /**
         * Guarda un token de sesion junto con el nombre de la campaña al que
         * corresponde en el fichero determinado para ello.
         *
         * @param token the token
         */
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
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
       
        protected void onCancelled() {
        	 /**
             * @reference http://stackoverflow.com/questions/11165860/asynctask-oncancelled-not-being-called-after-canceltrue/11166026#11166026
             */
            cancelTask();
        }

       
        private void cancelTask() {
            mAuthTask = null;
            loginSucceed = false;
            loginDialog.dismiss();
        }
    }


}
