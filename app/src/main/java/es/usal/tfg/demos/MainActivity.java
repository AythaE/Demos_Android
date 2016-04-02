package es.usal.tfg.demos;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "Demos";
    private static final String IP ="192.168.0.13";     //Change this constant with the ip of the server
    private static final String SERVER_ADDR = "http://"+IP+":8080/Demos_Rest/rest/files/upload";
    private static Uri photoFilePath;
    private static File photoFile;

    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    private static Future<String> upload;
    private ProgressBar uploadPBar;
    /**
     * Reference : http://developer.android.com/intl/es/guide/topics/media/camera.html
     * Reference : http://developer.android.com/intl/es/training/camera/photobasics.html
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        uploadPBar = (ProgressBar) findViewById(R.id.uploadProgressBarLarge);
        if (upload != null && !upload.isCancelled() && !upload.isDone())
        {
            //An upload is actually running so the progress bar must be shown
            uploadPBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "upload != null");
        }
        else{


            uploadPBar.setProgress(0);
            uploadPBar.setVisibility(View.INVISIBLE);
            Log.d(TAG,"upload = null");

        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Entrando en onClick");
                dispatchTakePictureIntent();
            }
        });

        Log.d(TAG, "Activity created successfully");
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Log.d(TAG, "entrando en dispatchTakePicture");
        //Checks if there is a camera application that can take the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            Log.d(TAG, "Hay aplicacion capaz de manejar el intent");
            photoFilePath = getOutputMediaFileUri();
            //continue only if the file is created
            if (photoFilePath != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFilePath);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        }
        else
            Log.d(TAG, "NO hay aplicacion capaz de manejar el intent");
    }

    /** Returns the URi of for saving the photo */
    private Uri getOutputMediaFileUri()
    {
        return Uri.fromFile(getOutputMediaFile());
    }

    /** Creates the FIle for the photo */
    private File getOutputMediaFile() {


        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {

            File photoStorageDir;
            //To store the photo in a public directory that can be use from other apps (such as the media scanner)
            //photoStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoDNI");

            //To store the photo in a directory for this application .
            //Reference: http://developer.android.com/intl/es/reference/android/content/Context.html#getExternalFilesDir(java.lang.String)

            photoStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

            //creates the directory if doesn't exist
            if (! photoStorageDir.exists())
                if (! photoStorageDir.mkdirs()){
                    Log.d(TAG, "Failed to create photo directory");
                    return null;
                }

            //Creates the FIle name
            String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss_SSSS").format(new Date());
            File photoFile;
            photoFile = new File(photoStorageDir.getPath() + File.separator + "DNI_" + timeStamp + ".jpg");

            return photoFile;
        }
        else
            return null;
    }

    /**
     * Method invoked when the photo intent returns
     *
     * Reference for the upload: https://github.com/koush/ion
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (requestCode == REQUEST_IMAGE_CAPTURE ) {
            View layout =  findViewById(R.id.coordinator_layout);


            if (resultCode == RESULT_OK) {


                //The image was taken correctly so
                //Now upload the image to the server
                uploadFile();


            }
            else if (resultCode == RESULT_CANCELED) {
                //User cancelled the image capture
            }
            else{
                //Something goes wrong so advise the user
                Snackbar.make(layout,  "Error tomando la imagen", Snackbar.LENGTH_LONG).show();
            }

        }
    }

    private void uploadFile() {
        if (upload != null && !upload.isCancelled() && !upload.isDone()) {
            resetUpload();
            return;
        }
        uploadPBar.setVisibility(View.VISIBLE);

        photoFile=new File(photoFilePath.getPath());

        upload = Ion.with(MainActivity.this)
                .load(SERVER_ADDR)
                .setTimeout(15 * 1000) //15 seconds
                .setLogging(TAG + " upload", Log.DEBUG)
                .progressBar(uploadPBar)
                .setMultipartFile("file", photoFile)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {


                        if (e != null) {
                            Toast.makeText(MainActivity.this, "Error subiendo la foto al servidor,\nintentelo de nuevo más tarde", Toast.LENGTH_LONG).show();

                            Log.d(TAG, e.getMessage());
                        } else {

                            Toast.makeText(MainActivity.this, "Foto subida correctamente al servidor", Toast.LENGTH_LONG).show();

                        }
                        deletePhoto();
                        resetUpload();
                    }
                });
    }

    /**
     * Method used to cancel pending uploads and reset progressbar after / before an upload
     */
    private void resetUpload()
    {
        //To cancel pending uploads
        upload.cancel();
        upload=null;

        uploadPBar.setProgress(0);
        uploadPBar.setVisibility(View.INVISIBLE);
    }

    private void deletePhoto()
    {
        if (!photoFile.delete())
            Log.d(TAG, "Error borrando foto");
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