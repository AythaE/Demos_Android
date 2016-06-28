package es.usal.tfg.demos;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

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

/**
 * A login screen that offers login via email/password.
 */
public class RegisterActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserRegisterTask mRegisterTask = null;

    // UI references.
    private EditText mCampaignView;
    private EditText mPasswordView;
    private EditText mPasswordView2;
    private EditText mDateView;
    private NavigationView navigationView;

    private static DatePickerDialog datePickerDialog;

    private static boolean showingDateDialog = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    private static Response<String> registerResp = null;
    private static int CONNECTION_TIMEOUT = 20;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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

        Log.d(MainActivity.TAG, "Entrando en RegisterAtivity");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

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
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            //showProgress(true);
            mRegisterTask = new UserRegisterTask(campaign, password, dateStr);
            mRegisterTask.execute((Void) null);
        }
    }

    private boolean isCampaignValid(String campaign) {
        //TODO: Replace this with your own logic

        return campaign.length() >= 2 && campaign.matches("^[a-zA-Z0-9]+[a-zA-Z0-9\\._-]*$");
    }

    private boolean isDateValid(Date date) {
        //TODO: Replace this with your own logic


        return date.after(new Date());
    }
    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() >= 8 && password.matches("^[a-zA-Z0-9\\.\\*#%&()=+:;,<>_!?-]*$");
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Register Page", // TODO: Define a title for the content shown.
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
                "Register Page", // TODO: Define a title for the content shown.
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


    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.register_drawer_item);
        if (showingDateDialog)
        {
            datePickerDialog.show();
            showingDateDialog = false;
        }


    }

    @Override
    protected void onDestroy() {

        if (datePickerDialog.isShowing()) {
            datePickerDialog.dismiss();
            showingDateDialog = true;
        }

        super.onDestroy();
    }

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
            retVal= false;
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
     * Represents an asynchronous registration task used to register
     * the user.
     */
    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {

        private final String mCampaign;
        private final String mPassword;
        private final String mDeleteDate;
        private final AlertDialog registerDialog;

        private String toastMessage;

        public boolean isRegisterSucceed() {
            return registerSucceed;
        }

        public void setRegisterSucceed(boolean regSuscceed) {
            this.registerSucceed = regSuscceed;
        }

        private boolean registerSucceed = false;

        UserRegisterTask(String campaign, String password, String deleteDate) {
            mCampaign = campaign;
            mPassword = password;
            mDeleteDate = deleteDate;
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

        private void checkResponse(Exception e) {
            if (e != null || (registerResp != null && registerResp.getHeaders().code() != 200)) {


                if (e != null) {
                    e.printStackTrace();
                }
                if (registerResp != null) {
                    //TODO leer error response
                    Log.d(MainActivity.TAG + " response message", registerResp.getHeaders().message());

                    if ((400 <= registerResp.getHeaders().code()) &&
                            (500 > registerResp.getHeaders().code())) {

                        byte[] datos = Base64.decode(registerResp.getResult(), Base64.NO_WRAP);
                        toastMessage = new String(datos);
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
                    //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
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

        /**
         * @reference http://stackoverflow.com/questions/11165860/asynctask-oncancelled-not-being-called-after-canceltrue/11166026#11166026
         */


        @Override
        protected void onCancelled() {
            cancelTask();
        }

        private void cancelTask() {
            mRegisterTask = null;
            registerSucceed = false;
            registerDialog.dismiss();
        }


    }


}

