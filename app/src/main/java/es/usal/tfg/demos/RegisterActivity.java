/*
 * Archivo: RegisterActivity.java 
 * Proyecto: Demos_Android
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import android.app.DatePickerDialog;
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
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Clase RegisterActivity que controla una ventana con los campos necesarios 
 * para registrar una campaña .
 */
public class RegisterActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


	/**
     * Instancia de la tarea que realizara la peticion de registro de manera 
     * asíncrona.
     */
    private static UserRegisterTask mRegisterTask = null;

    // UI references.
    
    /** El campo de texto de la campaña. */
    private EditText mCampaignView;
    
    /** El campo de texto de la contraseña. */
    private EditText mPasswordView;
    
    /** El campo de texto de verificación de la contraseña. */
    private EditText mPasswordView2;
    
    /** El campo de texto de la fecha de borrado. */
    private EditText mDateView;
    
    /** The navigation view. */
    private NavigationView navigationView;

    /** The {@link DatePickerDialog} de selección de fecha. */
    private static DatePickerDialog datePickerDialog;


    /** El {@link AlertDialog} de registro. */
    private static AlertDialog registerDialog;

    /** 
     * Flag para controlar cuando se muesta el dialogo de seleccion de fecha
     * de borrado para poder mostrarlo en caso de que se gire la pantalla, lo
     * que re-instancia esta clase perdiendose todos los campos no estáticos. 
     */
    private static boolean showingDateDialog = false;

    /**
     * Flag para controlar cuando se muesta el dialogo registro para poder
     * mostrarlo en caso de que se gire la pantalla, lo que re-instancia esta
     * clase perdiendose todos los campos no estáticos.
     */
    private static boolean showingRegisterDialog = false;
    
    /** The date format. */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    
    /** Response devuelto por la conexión al servidor. */
    private static Response<String> registerResp = null;
    
    /** The connection timeout, 20 segundos*/
    private static int CONNECTION_TIMEOUT = 20;
    

    /**
     * 
     * Carga y configura la UI
     * @see android.support.v7.app.AppCompatActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Set up the login form.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_register);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_register);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
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

        navigationView = (NavigationView) findViewById(R.id.nav_view_register);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.register_drawer_item);
        mCampaignView = (EditText) findViewById(R.id.campaign_register);


        mPasswordView = (EditText) findViewById(R.id.password_register);

        mPasswordView2 = (EditText) findViewById(R.id.password_register_repeat);


        /**
         * @see http://androidopentutorials.com/android-datepickerdialog-on-edittext-click-event/
         */
        mDateView = (EditText) findViewById(R.id.date_register);
        mDateView.setInputType(InputType.TYPE_NULL);


        Calendar calendar = Calendar.getInstance();
        datePickerDialog = new DatePickerDialog(RegisterActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar dialogCalend = Calendar.getInstance();
                dialogCalend.set(year,monthOfYear,dayOfMonth);
                mDateView.setText(dateFormat.format(dialogCalend.getTime()));
            }
        },calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));


        mDateView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    datePickerDialog.show();

                }
            }
        });

        mDateView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        registerDialog = new AlertDialog.Builder(RegisterActivity.this).create();

        //TODO check al girar pantalla
        registerDialog.setView(getLayoutInflater().inflate(R.layout.uploading_dialog, null));
        registerDialog.setTitle(R.string.register_dialog_title);
        registerDialog.setCancelable(false);
        registerDialog.setCanceledOnTouchOutside(false);

        Log.d(MainActivity.TAG, "Entrando en RegisterAtivity");
        
    }

    /** 
     * Al presionar el boton back si el {@link DrawerLayout} esta abierto lo
     * cierra, en caso contrario hace lo que haría por defecto
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_register);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Intenta registrar la campaña especificada en el formulario de 
     * registro, comprueba los posibles errores en los campos y si todo es 
     * correcto instancia y lanza {@link RegisterActivity#mRegisterTask} 
     * que efectúa la conexión con el servidor.
     */
    private void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }

        // Reset errors.
        mCampaignView.setError(null);
        mPasswordView.setError(null);
        mPasswordView2.setError(null);
        mDateView.setError(null);

        // Store values at the time of the login attempt.
        String campaign = mCampaignView.getText().toString();
        String password = mPasswordView.getText().toString();
        String password2 = mPasswordView2.getText().toString();
        String dateStr = mDateView.getText().toString();


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

        Date date=null;
        try {
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // Check for a valid deletion date.
        if (TextUtils.isEmpty(dateStr)) {
            mDateView.setError(getString(R.string.error_field_required));
            focusView = mDateView;
            cancel = true;
        } else if (date != null && !isDateValid(date)) {
            mDateView.setError(getString(R.string.error_invalid_date));
            focusView = mDateView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            
            mRegisterTask = new UserRegisterTask(campaign, password, dateStr);
            mRegisterTask.execute((Void) null);
        }
    }

    /**
     * Comprueba si una campaña es valida, esta se considera valida si contiene
     *  dos o más caracters alfa numéricos, o los signos ., _ y -.
     *
     * @param campaign cadena de caracteres introducida en 
     * {@link RegisterActivity#mCampaignView} a comprobar
     * @return true, si la campaña es valida
     */
    private boolean isCampaignValid(String campaign) {

        return campaign.length() >= 2 && campaign.matches("^[a-zA-Z0-9]+[a-zA-Z0-9\\._-]*$");
    }


    /**
     * Comprueba si la fecha es valida, esta se considera valida si es 
     * posterior a la fecha actual del dispositivo
     *
     *
     * @param date fecha introducida en {@link RegisterActivity#mDateView}
     * @return true, si la comprobación es valida
     */
    private boolean isDateValid(Date date) {

        return date.after(new Date());
    }
    
    /**
     * Comprueba si una contraseña es valida, esta se considera valida si 
     * contiene al menos 8 caracteres que pueden ser: letras (mayusculas o 
     * minusculas), numeros, puntos, asteriscos, #,...
     *
     *
     * @param password la contraseña introducida en 
     * {@link RegisterActivity#mPasswordView}
     * @return true, if is password valid
     */
    private boolean isPasswordValid(String password) {
        
        return password.length() >= 8 && password.matches("^[a-zA-Z0-9\\.\\*#%&()=+:;,<>_!?-]*$");
    }

    

  
    /**
     * Al entrar en primer plano selecciona el campo Register Item de la
     * {@link NavigationView} y además comprueba si se estaba mostrando el 
     * dialogo {@link RegisterActivity#datePickerDialog} o
     * {@link RegisterActivity#registerDialog}, en cuyo caso se
     * despliegan.
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.register_drawer_item);
        if (showingDateDialog)
        {
            datePickerDialog.show();
            showingDateDialog = false;
        }

        if (showingRegisterDialog){
            registerDialog.show();
            showingRegisterDialog= false;
        }

    }

    /** 
	 * Invocado al destruir la actividad, comprueba si se estaba mostrando el 
     * dialogo {@link RegisterActivity#datePickerDialog} o
     * {@link RegisterActivity#registerDialog}, en cuyo caso lo
     * fija el flag {@link RegisterActivity#showingDateDialog} o
     * {@link RegisterActivity#showingRegisterDialog} a true para que se muestre
     * cuando vuelva y cierra el dialogo actual.
     * @see android.support.v7.app.AppCompatActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {

        if (datePickerDialog.isShowing()) {
            datePickerDialog.dismiss();
            showingDateDialog = true;
        }

        if (registerDialog.isShowing()){
            registerDialog.dismiss();
            showingRegisterDialog = true;
        }

        super.onDestroy();
    }
    
    /** 
     * Controla las acciones a tomar al seleccionar los distintos campos de la 
     * {@link NavigationView} que permiten cambiar entre actividades
     * @see android.support.design.widget.NavigationView.OnNavigationItemSelectedListener#onNavigationItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        boolean retVal = true;
        if (id == R.id.login_drawer_item) {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);

        } else if (id == R.id.register_drawer_item) {

        } else if (id == R.id.instructions_drawer_item) {
            Intent intent = new Intent(RegisterActivity.this, InstructionsActivity.class);

            startActivity(intent);
        } else if (id == R.id.main_drawer_item) {

            File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");


            if (!tokenFile.exists()) {
                Toast t = Toast.makeText(RegisterActivity.this, R.string.toastMainForbidden, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v != null) {
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                retVal= false;
            }
            else {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
            }

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_register);
        drawer.closeDrawer(GravityCompat.START);
        return retVal;
    }

    /**
     * Representa la tarea encargada de conexión al servidor, esta lanza una 
     * peticion de registro al servidor y comprueba su respuesta. 
     * 
     */
    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {

    	/** La campaña. */
        private final String mCampaign;
        
        /** La contraseña. */
        private final String mPassword;
        
        /** La fecha de borrado. */
        private final String mDeleteDate;

        /** El mensaje {@link Toast}. */
        private String toastMessage;

        /**
         * Checks if is register succeed.
         *
         * @return true, if is register succeed
         */
        public boolean isRegisterSucceed() {
            return registerSucceed;
        }

        /**
         * Sets the register succeed.
         *
         * @param regSuscceed the new register succeed
         */
        public void setRegisterSucceed(boolean regSuscceed) {
            this.registerSucceed = regSuscceed;
        }

        /** Flag para determinar si el registro ha sido correcto o no. */
        private boolean registerSucceed = false;

        /**
         * Crea una nueva instancia de esta clase.
         *
         * @param campaign the campaign
         * @param password the password
         */
        UserRegisterTask(String campaign, String password, String deleteDate) {
            mCampaign = campaign;
            mPassword = password;
            mDeleteDate = deleteDate;



        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            registerDialog.show();
        }


        /**
         * Realiza la conexion al servidor quedandose bloqueado hasta que este
         * responda. Para ello codifica en Base64 la campaña, la contraseña y 
         * la fecha de borrado, enviandolas generando un formulario 
         * application/x-www-form-urlencoded
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Boolean doInBackground(Void... params) {


            String serverRegister = MainActivity.SERVER_ADDR + "/campaign/register";
            CheckSessionActivity.trustServerCertificate(RegisterActivity.this);


            try {
                String campaignB64 = Base64.encodeToString(mCampaign.getBytes("UTF-8"), Base64.NO_WRAP);
                String passwordB64 = Base64.encodeToString(mPassword.getBytes("UTF-8"), Base64.NO_WRAP);
                String deleteDateB64 = Base64.encodeToString(mDeleteDate.getBytes("UTF-8"), Base64.NO_WRAP);
                registerResp = Ion.with(RegisterActivity.this)
                        .load(serverRegister)
                        .setTimeout(CONNECTION_TIMEOUT * 1000)
                        .setLogging(MainActivity.TAG + " register", Log.DEBUG)
                        .setBodyParameter("campaign", campaignB64)
                        .setBodyParameter("password", passwordB64)
                        .setBodyParameter("delete_date", deleteDateB64)
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
            }
            return isRegisterSucceed();
        }

        /**
         * Comprueba la respuesta del servidor usando el atributo 
         * {@link RegisterActivity#registerResp}. Si la respuesta es correcta
         * fija el flag {@link UserRegisterTask#registerSucceed} a true, en 
         * caso contrario a false. Tambien fija el mensaje que mostrará en un 
         * {@link Toast} al usuario indicandole el resultado del registro
         *
         * @param e posible excepcion producida en la conexión
         */
        private void checkResponse(Exception e) {
            if (e != null || (registerResp != null && registerResp.getHeaders().code() != 200)) {


                if (e != null) {
                    e.printStackTrace();
                }
                if (registerResp != null) {
                   
                    Log.d(MainActivity.TAG + " response message", registerResp.getHeaders().message());

                    if ((400 <= registerResp.getHeaders().code()) &&
                            (500 > registerResp.getHeaders().code())) {
                    	try {
                            byte [] datos = Base64.decode( registerResp.getResult(),Base64.NO_WRAP);
                            toastMessage = new String(datos);
                    	} catch (Exception e2){
                    		toastMessage = getString(R.string.toastWrongRegisterResult);
                    	}
                    } else {
                        toastMessage = getString(R.string.toastWrongRegisterResult);
                    }
                } else {
                    toastMessage = getString(R.string.toastWrongRegisterResult);
                }
                setRegisterSucceed(false);
            } else {

                toastMessage = getString(R.string.toastCorrectRegisterResult);
                setRegisterSucceed(true);


            }
        }

        /**
         * Hace desaparecer el dialogo {@link UserRegisterTask#registerDialog}, 
         * muestra un {@link Toast} con el resultado de la operación al usuario
         * , si el flag {@link UserRegisterTask#registerSucceed} es true guarda
         *  el token recibido en el método 
         * {@link UserRegisterTask#saveSessionToken(String)} y pasa a 
         * {@link MainActivity}, en caso contrario muestra el error en Log 
         * y finaliza.
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(final Boolean success) {
            mRegisterTask = null;
            registerDialog.dismiss();

            if (isCancelled())
                cancelTask();
            else {

                Toast t = Toast.makeText(RegisterActivity.this, toastMessage, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v != null) {
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                if (success) {
                    String token = registerResp.getResult();
                    saveSessionToken(token);
                    Log.d(MainActivity.TAG, "registro correcto: " + token);
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.putExtra("campaignName", mCampaign);
                    startActivity(intent);
                      finish();
                } else {
                    try {
                        String responseResult = new String(Base64.decode(registerResp.getResult(), Base64.NO_WRAP));
                        Log.d(MainActivity.TAG + " response message", responseResult);
                    } catch (Exception e){}
                    Log.d(MainActivity.TAG, "registro incorrecto");
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
                Log.d(MainActivity.TAG, "Token: " + token + " escrito en " + getFilesDir().getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {
                    if (bw != null) {
                        bw.flush();
                        bw.close();
                    }
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }

                } catch (IOException e) {
                }


            }

        }


        @Override
        protected void onCancelled() {
        	/*
             * @reference http://stackoverflow.com/questions/11165860/asynctask-oncancelled-not-being-called-after-canceltrue/11166026#11166026
             */
            cancelTask();
        }

        
        private void cancelTask() {
            mRegisterTask = null;
            registerSucceed = false;
            registerDialog.dismiss();
        }


    }


}

