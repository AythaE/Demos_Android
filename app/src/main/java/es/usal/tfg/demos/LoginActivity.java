package es.usal.tfg.demos;

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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import java.util.concurrent.ExecutionException;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mCampaignView;
    private EditText mPasswordView;
    private NavigationView navigationView;

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    private static Response<String> loginResp = null;
    private static int CONNECTION_TIMEOUT = 20;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            //TODO no funciona, intentar arreglar o borrarlo
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login_form || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

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

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_login);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void launchRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
        //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
        //finish();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
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
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            //showProgress(true);
            mAuthTask = new UserLoginTask(campaing, password);
            mAuthTask.execute((Void) null);
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
            retVal =false;
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

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.login_drawer_item);
    }


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Login Page", // TODO: Define a title for the content shown.
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
                "Login Page", // TODO: Define a title for the content shown.
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

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mCampaign;
        private final String mPassword;

        private String toastMessage;

        private final AlertDialog loginDialog;

        public boolean isLoginSucceed() {
            return loginSucceed;
        }

        public void setLoginSucceed(boolean logSucceed) {
            this.loginSucceed = logSucceed;
        }

        private boolean loginSucceed = false;

        UserLoginTask(String campaign, String password) {
            mCampaign = campaign;
            mPassword = password;

            loginDialog = new AlertDialog.Builder(LoginActivity.this).create();

            //TODO check al girar pantalla
            loginDialog.setView(getLayoutInflater().inflate(R.layout.uploading_dialog, null));
            loginDialog.setTitle(R.string.login_dialog_title);
            loginDialog.setCancelable(false);
            loginDialog.setCanceledOnTouchOutside(false);


        }

        @Override
        protected void onPreExecute() {
            loginDialog.show();
        }

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

        private void checkResponse(Exception e) {

            if (e != null || (loginResp != null && loginResp.getHeaders().code() != 200)) {



                if (e != null) {
                    e.printStackTrace();
                }
                if (loginResp != null) {

                    //TODO leer error response
                    Log.d(MainActivity.TAG + " response message", loginResp.getHeaders().message());
                    if((400 <= loginResp.getHeaders().code()) &&  (500 > loginResp.getHeaders().code())) {
                            byte [] datos = Base64.decode( loginResp.getResult(),Base64.NO_WRAP);
                            toastMessage = new String(datos);

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
                    //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
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
        @Override
        /**
         * @reference http://stackoverflow.com/questions/11165860/asynctask-oncancelled-not-being-called-after-canceltrue/11166026#11166026
         */
        protected void onCancelled() {
            cancelTask();
        }

        private void cancelTask() {
            mAuthTask = null;
            loginSucceed = false;
            loginDialog.dismiss();
        }
    }


}
