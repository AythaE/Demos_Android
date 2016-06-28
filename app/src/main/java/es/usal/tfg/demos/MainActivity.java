package es.usal.tfg.demos;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.FileProvider;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String CAPTURE_IMAGE_FILE_PROVIDER = "es.usal.tfg.demos.fileprovider";
    private static final String PHOTOS_FOLDER="photos";
    public static final String TAG = "Demos";
    private static final String IP = "prodiasv08.fis.usal.es";     //Change this constant with the ip of the server
    public static final String SERVER_ADDR ="https://" + IP + ":443/Demos_Rest/rest";

    private static final int CONNECTION_TIMEOUT = 50;


    private static Uri photoFilePathFront, photoFilePathBack;
    private static File photoFileFront, photoFileBack;

    private EditText mSignPaper;
    private NavigationView navigationView;
    private static long numSignPaper;


    private static String campaignName;

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    private static Future<Response<String>> upload;
    private static AlertDialog UploadDialog, ResultDialog;
    private static String resultDialogMessage = "";
    private static boolean showingUploadDialog = false;
    private static boolean showingResultDialog = false;
    private static boolean invalidToken = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * Reference : http://developer.android.com/intl/es/guide/topics/media/camera.html
     * Reference : http://developer.android.com/intl/es/training/camera/photobasics.html
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);


        mSignPaper= (EditText) findViewById(R.id.sign_paper);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Entrando en onClick");

                resetPhotoTaking();

                attemptTakePicture();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_main);
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

        navigationView = (NavigationView) findViewById(R.id.nav_view_main);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        TextView headerCampaign = (TextView) header.findViewById(R.id.campaign_header_textview);
        Intent intent = getIntent();
        campaignName = intent.getStringExtra("campaignName");
        if (campaignName != null){
            Log.d(TAG, "CampaignName: "+campaignName+" TextView: "+headerCampaign);
            headerCampaign.setText(campaignName);
        }
        else {
            File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");

            if (tokenFile.exists()) {
                try {

                    BufferedReader br = new BufferedReader(new FileReader(tokenFile));

                    campaignName = br.readLine();

                    headerCampaign.setText(campaignName);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        UploadDialog = new AlertDialog.Builder(MainActivity.this).create();

        UploadDialog.setView(getLayoutInflater().inflate(R.layout.uploading_dialog, null));
        UploadDialog.setTitle(R.string.upload_dialog_title);
        UploadDialog.setCancelable(true);
        UploadDialog.setCanceledOnTouchOutside(false);

        UploadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (upload != null && upload.cancel())
                {
                    Log.d(TAG, "subida cancelada por usuario");
                }
                else
                {
                    Log.e(TAG, "ERROR cancelando subida");
                }

                showingUploadDialog = false;
            }
        });


        ResultDialog = new AlertDialog.Builder(MainActivity.this).setPositiveButton(R.string.result_dialog_button,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showingResultDialog = false;
                resultDialogMessage = "";
                mSignPaper.setText("");
                dialog.dismiss();
                if (invalidToken){
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    //TODO http://stackoverflow.com/questions/16419627/making-an-activity-appear-only-once-when-the-app-is-started
                    finish();
                }

            }
        }).create();
        ResultDialog.setCancelable(false);
        ResultDialog.setCanceledOnTouchOutside(false);
        ResultDialog.setTitle(R.string.result_dialog_title);

        Log.d(TAG, "Activity created successfully");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_main);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void attemptTakePicture(){
        // Reset errors.
        mSignPaper.setError(null);

        // Store values at the time of the picture attempt.
        String signPaper = mSignPaper.getText().toString();


        boolean cancel = false;
        View focusView = null;

        // Check for a valid signPaper.
        if (TextUtils.isEmpty(signPaper)) {
            mSignPaper.setError(getString(R.string.error_field_required));
            focusView = mSignPaper;
            cancel = true;
        } else if (!isSignPaperValid(signPaper)) {
            mSignPaper.setError(getString(R.string.error_invalid_sign_paper));
            focusView = mSignPaper;
            cancel = true;
        }




        if (cancel) {
            // There was an error; don't attempt taking picture and focus the
            // form field with an error.
            focusView.requestFocus();
        } else {

            numSignPaper =Long.parseLong(signPaper);
            dispatchTakePictureIntent();
        }
    }

    private boolean isSignPaperValid(String signPaper){
        long numSignPaperTemp = 0;
        try{
            numSignPaperTemp = Long.parseLong(signPaper);
        } catch (NumberFormatException e){}
        return  numSignPaperTemp > 0;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Log.d(TAG, "entrando en dispatchTakePicture");
        //Checks if there is a camera application that can take the intent
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
            Log.d(TAG, "Hay aplicacion capaz de manejar el intent");
            if (photoFilePathFront == null)
            {
                Toast t = Toast.makeText(MainActivity.this, R.string.toastMakeFrontPhoto, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v!=null){
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                photoFilePathFront = getOutputMediaFileUri();
                Log.d(TAG, photoFilePathFront.toString());
                //continue only if the file is created
                if (photoFilePathFront != null) {

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFilePathFront);
                    /**
                     * @reference https://medium.com/@a1cooke/using-v4-support-library-fileprovider-and-camera-intent-a45f76879d61#.6wu8mv2ya
                     */
                    List<ResolveInfo> resolvedIntentActivities = this.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);

                    for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                        String packageName = resolvedIntentInfo.activityInfo.packageName;

                        this.grantUriPermission(packageName, photoFilePathFront, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }


                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
            else if (photoFilePathBack == null)
            {
                Toast t = Toast.makeText(MainActivity.this, R.string.toastMakeBackPhoto, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v!=null){
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                photoFilePathBack = getOutputMediaFileUri();
                Log.d(TAG, photoFilePathBack.toString());
                //continue only if the file is created
                if (photoFilePathBack != null) {

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFilePathBack);

                    /**
                     * @reference https://medium.com/@a1cooke/using-v4-support-library-fileprovider-and-camera-intent-a45f76879d61#.6wu8mv2ya
                     */
                    List<ResolveInfo> resolvedIntentActivities = this.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);

                    for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                        String packageName = resolvedIntentInfo.activityInfo.packageName;

                        this.grantUriPermission(packageName, photoFilePathBack, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }


        } else
            Log.d(TAG, "NO hay aplicacion capaz de manejar el intent");
    }

    /**
     * Returns the URi of for saving the photo
     * @reference https://developer.android.com/reference/android/support/v4/content/FileProvider.html
     */
    private Uri getOutputMediaFileUri() {

        return FileProvider.getUriForFile(MainActivity.this ,CAPTURE_IMAGE_FILE_PROVIDER, getOutputMediaFile());
    }

    /**
     * Creates the FIle for the photo
     */
    private File getOutputMediaFile() {

        //TODO si no tiene tarjeta sd que
        File photoStorageDir;
        //To store the photo in a public directory that can be use from other apps (such as the media scanner)
        //photoStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoDNI");

        //To store the photo in a directory for this application .
        //Reference: http://developer.android.com/intl/es/reference/android/content/Context.html#getExternalFilesDir(java.lang.String)

        photoStorageDir = new File(getFilesDir(), "photos");

        //creates the directory if doesn't exist
        if (!photoStorageDir.exists())
            if (!photoStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create photo directory");
                return null;
            }

        //Creates the File name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss_SSSS").format(new Date());
        File photoFile;
        photoFile = new File(photoStorageDir.getPath() + File.separator + "DNI_" + timeStamp + ".jpg");

        return photoFile;
    }

    /**
     * Method invoked when the photo intent returns
     * <p>
     * Reference for the upload: https://github.com/koush/ion
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "Entrando en onActivityResult");
        if (requestCode == REQUEST_IMAGE_CAPTURE) {

            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Exito tomando una foto");

                if (photoFilePathFront !=null && photoFilePathBack == null) {
                    dispatchTakePictureIntent();
                }

                //The image was taken correctly so
                //Now upload the image to the server
                else if (photoFilePathFront != null && photoFilePathBack != null) {

                    File photoDirectory = new File(getFilesDir(), PHOTOS_FOLDER);


                    photoFileFront = new File(photoDirectory, photoFilePathFront.getLastPathSegment());
                    photoFileBack = new File(photoDirectory, photoFilePathBack.getLastPathSegment());

                    if (!photoFileBack.exists()){
                        Log.d(TAG, "No existe la foto trasera");
                    }
                    if (!photoFileFront.exists())
                    {
                        Log.d(TAG, "No existe la foto frontal");
                    }

                    /**
                     * @reference https://medium.com/@a1cooke/using-v4-support-library-fileprovider-and-camera-intent-a45f76879d61#.6wu8mv2ya
                     */
                    this.revokeUriPermission( photoFilePathFront, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    this.revokeUriPermission( photoFilePathBack, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    uploadFile();
                }

            } else if (resultCode == RESULT_CANCELED) {
                //User cancelled the image capture
                Log.d(TAG, "Codigo cancel de foto");
            } else {
                //Something goes wrong so advise the user
                Log.d(TAG, "Codigo erroneo de foto");
            }

        }
    }

    private void uploadFile() {

        /**
         * @Reference http://androidexample.com/Custom_Dialog_-_Android_Example/index.php?view=article_discription&aid=88&aaid=111
         */


        UploadDialog.show();

        if (upload != null && !upload.isCancelled() && !upload.isDone()) {
            resetUpload();
            return;
        }

        File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");



        String campaña64 = null;
        String token = null;
        String numSignPaper64=null;
        if (tokenFile.exists()) {
            Log.d(MainActivity.TAG, "Token en: " + tokenFile.getAbsolutePath());


            try {

                BufferedReader br = new BufferedReader(new FileReader(tokenFile));
                String line;
                String campaña = null;
                while ((line = br.readLine()) != null) {
                    if (campaña == null) {
                        campaña = new String(line);
                    } else {
                        if (token == null)
                            token = new String(line);
                    }
                    Log.d(MainActivity.TAG, line);
                }


                campaña64 = Base64.encodeToString(campaña.getBytes("UTF-8"), Base64.NO_WRAP);
                numSignPaper64 = Base64.encodeToString(Long.toString(numSignPaper).getBytes("UTF-8"), Base64.NO_WRAP);
                Log.d(TAG, "Num signPaper: "+numSignPaper);
                Log.d(MainActivity.TAG, "Token definitivo: " + token);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String uploadURL = SERVER_ADDR + "/files/upload";
            upload = Ion.with(this)
                    .load(uploadURL)
                    .setTimeout(CONNECTION_TIMEOUT * 1000) //50 seconds
                    .setLogging(TAG + " upload", Log.DEBUG)
                    .setMultipartParameter("campaign", campaña64)
                    .setMultipartParameter("token", token)
                    .setMultipartParameter("num_sign_paper", numSignPaper64)
                    .setMultipartFile("front", photoFileFront)
                    .setMultipartFile("back", photoFileBack)
                    .asString()
                    .withResponse()
                    .setCallback(new FutureCallback<Response<String>>() {

                        @Override
                        public void onCompleted(Exception e, Response<String> result) {

                            if (e != null || (result != null && result.getHeaders().code() != 200)) {


                                if (e != null) {

                                    e.printStackTrace();

                                }


                                if (upload.isCancelled()) {
                                    resultDialogMessage = getString(R.string.toastCanceledResult);


                                } else {
                                    String message64 = null;
                                    if (result != null) {
                                        message64 = result.getResult();
                                        Log.d(TAG + " response message", result.getHeaders().code()+" "+ result.getHeaders().message());
                                        Log.d(TAG, "message64: "+message64);

                                        if (result.getHeaders().code() == 404 || result.getHeaders().code() >= 510) {
                                            String message = null;
                                            byte[] datos = Base64.decode(message64, Base64.NO_WRAP);
                                            message = new String(datos);
                                            resultDialogMessage = message;
                                            if (result.getHeaders().code() == 404){
                                                File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");

                                                if (!tokenFile.delete()){
                                                    Log.d(TAG, "Error borrando archivo token");
                                                }
                                                invalidToken = true;
                                            }
                                            //toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                                        } else {
                                            resultDialogMessage = getString(R.string.toastWrongUploadingResult);

                                            //toast.makeText(MainActivity.this, R.string.toastWrongUploadingResult, Toast.LENGTH_LONG).show();
                                        }

                                    } else {
                                        resultDialogMessage = getString(R.string.toastWrongUploadingResult);
                                        //toast.makeText(MainActivity.this, R.string.toastWrongUploadingResult, Toast.LENGTH_LONG).show();
                                    }
                                }


                            } else {
                                resultDialogMessage = getString(R.string.toastCorrectUploadingResult);
                               // toast.makeText(MainActivity.this, R.string.toastCorrectUploadingResult, Toast.LENGTH_LONG).show();

                            }
                            if (UploadDialog.isShowing()) {
                                UploadDialog.dismiss();
                                ResultDialog.setMessage(resultDialogMessage);
                                ResultDialog.show();
                            }
                            resetPhotoTaking();
                            deletePhoto();
                            resetUpload();

                        }
                    });
        }
        else{
            Log.d(TAG, "no se encuentra el fichero token");
        }



    }

    /**
     * Method used to cancel pending uploads and reset progressbar after / before an upload
     */
    private void resetUpload() {
        //To cancel pending uploads


        upload.cancel();

        upload = null;

        //deletePhoto();
        /*
        uploadPBar.setProgress(0);
        uploadPBar.setVisibility(View.INVISIBLE);
        */
    }

    private void resetPhotoTaking ()
    {
        photoFileBack =null;
        photoFilePathBack = null;
        photoFilePathFront=null;
        photoFileFront=null;
    }
    private void deletePhoto() {
        //TODO borrar foto trasera

        File photoDirectory = new File(getFilesDir(), PHOTOS_FOLDER);

        if (photoDirectory != null && !photoDirectory.exists()){
            Log.d(TAG, "No existe el directorio");
        }

        File [] photos = photoDirectory.listFiles();

        for (int i =0; i<photos.length; i++)
        {
            File photoToDelete = photos[i];
            Log.d(TAG, "photo to delete: "+photoToDelete.getAbsolutePath());

            if (photoToDelete != null){
                if (photoToDelete.exists()) {
                    if (photoToDelete.delete() == false) {
                        Log.d(TAG, "Error borrando foto");
                    }
                }
                else {
                    Log.d(TAG, "Error borrando foto porque no existe");
                }
            } else {
                Log.d(TAG, "Error borrando foto porque es null");
            }
        }


        muestraAchivosEnDirectorio(getFilesDir());
    }

    private void muestraAchivosEnDirectorio(File dir){


        if (dir != null)
        {
            Log.d(TAG + " Ficheros",dir.getAbsolutePath());
            File[] files = null;

            files = dir.listFiles();

            if (files ==null)
                return;

            for (int i=0; i<files.length; i++)
            {

                muestraAchivosEnDirectorio(files[i]);
            }
        }
        return;
    }
    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
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
                "Main Page", // TODO: Define a title for the content shown.
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

        navigationView.setCheckedItem(R.id.main_drawer_item);
        if (showingUploadDialog)
        {
            UploadDialog.show();
            showingUploadDialog = false;
        }

        if (showingResultDialog)
        {
            ResultDialog.setMessage(resultDialogMessage);
            ResultDialog.show();
            showingResultDialog = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (UploadDialog.isShowing()) {
            UploadDialog.dismiss();
            showingUploadDialog = true;
        }
        if (ResultDialog.isShowing())
        {
            ResultDialog.dismiss();
            showingResultDialog = true;
        }
    }

    /*

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        boolean retValue=true;
        if (id == R.id.login_drawer_item) {

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);


        } else if (id == R.id.register_drawer_item) {

            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);


        } else if (id == R.id.instructions_drawer_item) {
            Intent intent = new Intent(MainActivity.this, InstructionsActivity.class);
            if (campaignName !=null)
                intent.putExtra("campaignName", campaignName);
            startActivity(intent);

        } else if (id == R.id.main_drawer_item) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_main);
        drawer.closeDrawer(GravityCompat.START);
        return retValue;
    }
}