package es.usal.tfg.demos;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.koushikdutta.ion.builder.Builders;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "Demos";
    private static final String IP = "prodiasv08.fis.usal.es";     //Change this constant with the ip of the server
    private static final String SERVER_ADDR = "http://" + IP + ":8080/Demos_Rest/rest/files/upload";
    private static final int CONNECTION_TIMEOUT = 50;


    private static Uri photoFilePathFront, photoFilePathBack;
    private static File photoFileFront, photoFileBack;


    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    private static Future<Response<String>> upload;
    private static AlertDialog UploadDialog;
    private static boolean showingUploadDialog = false;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (upload != null && !upload.isCancelled() && !upload.isDone()) {
            //An upload is actually running so the progress bar must be shown
           // uploadPBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "upload != null");
        } else {
            //TODO comprobar que pasa al girar pantalla durante subida

        }



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Entrando en onClick");

                resetPhotoTaking();

                dispatchTakePictureIntent();
                //If front Photo is a succeed take back photo, else reset all
               /* TODO if(succeedPhoto) {
                    Log.d(TAG, "Exito tomando foto delantera, ahora se tomara la trasera");
                    dispatchTakePictureIntent();
                }
                else{
                    resetPhotoTaking();
                }
                */
            }
        });

        UploadDialog = new AlertDialog.Builder(MainActivity.this).create();

        UploadDialog.setView(getLayoutInflater().inflate(R.layout.content_main, null));
        UploadDialog.setTitle(R.string.upload_dialog_title);
        UploadDialog.setCancelable(false);
        UploadDialog.setCanceledOnTouchOutside(false);

        Log.d(TAG, "Activity created successfully");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Log.d(TAG, "entrando en dispatchTakePicture");
        //Checks if there is a camera application that can take the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.d(TAG, "Hay aplicacion capaz de manejar el intent");
            if (photoFilePathFront == null)
            {
                Toast.makeText(MainActivity.this, R.string.toastMakeFrontPhoto, Toast.LENGTH_LONG).show();
                photoFilePathFront = getOutputMediaFileUri();
                //continue only if the file is created
                if (photoFilePathFront != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFilePathFront);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
            else if (photoFilePathBack == null)
            {
                Toast.makeText(MainActivity.this, R.string.toastMakeBackPhoto, Toast.LENGTH_LONG).show();
                photoFilePathBack = getOutputMediaFileUri();
                //continue only if the file is created
                if (photoFilePathBack != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFilePathBack);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }


        } else
            Log.d(TAG, "NO hay aplicacion capaz de manejar el intent");
    }

    /**
     * Returns the URi of for saving the photo
     */
    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }

    /**
     * Creates the FIle for the photo
     */
    private File getOutputMediaFile() {


        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {

            File photoStorageDir;
            //To store the photo in a public directory that can be use from other apps (such as the media scanner)
            //photoStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoDNI");

            //To store the photo in a directory for this application .
            //Reference: http://developer.android.com/intl/es/reference/android/content/Context.html#getExternalFilesDir(java.lang.String)

            photoStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

            //creates the directory if doesn't exist
            if (!photoStorageDir.exists())
                if (!photoStorageDir.mkdirs()) {
                    Log.d(TAG, "Failed to create photo directory");
                    return null;
                }

            //Creates the FIle name
            String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss_SSSS").format(new Date());
            File photoFile;
            photoFile = new File(photoStorageDir.getPath() + File.separator + "DNI_" + timeStamp + ".jpg");

            return photoFile;
        } else
            return null;
    }

    /**
     * Method invoked when the photo intent returns
     * <p>
     * Reference for the upload: https://github.com/koush/ion
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            View layout = findViewById(R.id.coordinator_layout);


            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Exito tomando una foto");

                if (photoFilePathFront !=null && photoFilePathBack == null) {
                    dispatchTakePictureIntent();
                }

                //The image was taken correctly so
                //Now upload the image to the server
                else if (photoFilePathFront != null && photoFilePathBack != null) {
                    uploadFile();
                }

            } else if (resultCode == RESULT_CANCELED) {
                //User cancelled the image capture
            } else {
                //Something goes wrong so advise the user
                Snackbar.make(layout, "Error tomando la imagen", Snackbar.LENGTH_LONG).show();
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



        photoFileFront = new File(photoFilePathFront.getPath());
        photoFileBack = new File(photoFilePathBack.getPath());

        upload = Ion.with(MainActivity.this)
                .load(SERVER_ADDR)
                .setTimeout(CONNECTION_TIMEOUT * 1000) //15 seconds
                .setLogging(TAG + " upload", Log.DEBUG)
                .setMultipartFile("front", photoFileFront)
                .setMultipartFile("back", photoFileBack)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {

                    @Override
                    public void onCompleted(Exception e, Response<String> result) {


                        if (e != null || result.getHeaders().code() != 200) {
                            Toast.makeText(MainActivity.this, R.string.toastWrongUploadingResult, Toast.LENGTH_LONG).show();

                            if (e!= null)
                                Log.d(TAG, e.getMessage());
                        } else {

                            Toast.makeText(MainActivity.this, R.string.toastCorrectUploadingResult, Toast.LENGTH_LONG).show();

                        }
                        if (UploadDialog.isShowing())
                            UploadDialog.dismiss();
                        resetPhotoTaking();
                        deletePhoto();
                        resetUpload();

                    }
                });
    }

    /**
     * Method used to cancel pending uploads and reset progressbar after / before an upload
     */
    private void resetUpload() {
        //To cancel pending uploads


        upload.cancel();

        upload = null;
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
        if (photoFileFront != null && photoFileFront.exists() && !photoFileFront.delete())
            Log.d(TAG, "Error borrando foto");
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
        if (showingUploadDialog)
        {
            UploadDialog.show();
            showingUploadDialog = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (UploadDialog.isShowing()) {
            UploadDialog.dismiss();
            showingUploadDialog = true;
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
}