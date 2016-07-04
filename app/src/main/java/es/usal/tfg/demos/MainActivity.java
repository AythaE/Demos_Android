/*
 * Archivo: MainActivity.java 
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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


/**
 * Clase MainActivity que controla la ventana principal de la aplicación la 
 * cual permite realizar las subidas de fotos al servidor.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    /** 
     * Constante REQUEST_IMAGE_CAPTURE usada como requestCode para lanzar los 
     * {@link Intent} de realización de fotos. 
     */
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    
    /** 
     * Constante CAPTURE_IMAGE_FILE_PROVIDER usada para acceder al 
     * {@link FileProvider} definido en el Manifest. 
     */
    private static final String CAPTURE_IMAGE_FILE_PROVIDER = "es.usal.tfg.demos.fileprovider";
    
    /** Constante PHOTOS_FOLDER. */
    private static final String PHOTOS_FOLDER="photos";
    
    /** Constante TAG usada para los mensajes del logcat. */
    public static final String TAG = "Demos";
    
    /** ConstantE SERVER_NAME. */
    private static final String SERVER_NAME = "prodiasv08.fis.usal.es";     
    
    /** 
     * Constante SERVER_ADDR que contiene la dirección principal del servicio
     * web, sobre esta URL se añaden el método concreto de cada peticion al 
     * servidor. 
     * 
     */
    public static final String SERVER_ADDR ="https://" + SERVER_NAME + ":443/Demos_Rest/rest";

    /** Constante CONNECTION_TIMEOUT de 50 segundos. */
    private static final int CONNECTION_TIMEOUT = 50;


    /** {@link Uri} de las fotografías del DNI frontal y tasero. */
    private static Uri photoFilePathFront, photoFilePathBack;
    
    /** {@link File} de las fotografías del DNI frontal y tasero. */
    private static File photoFileFront, photoFileBack;

    /** El campo de texto de la hoja de firmas. */
    private EditText mSignPaper;
    
    /** The navigation view. */
    private NavigationView navigationView;
    
    /** El número de la hoja de firmas. */
    private static long numSignPaper;


    /** El nombre de la campaña. */
    private static String campaignName;

    
    //Statics fields to try to avoid orientations change bugs (because of recreation of the activity)
    
    /** {@link Future} de la respuesta sel servidor. */
    private static Future<Response<String>> upload;
    
    /** {@link AlertDialog} de subida y resultados. */
    private static AlertDialog UploadDialog, ResultDialog;
    
    /** The result dialog message. */
    private static String resultDialogMessage = "";
    
    /** 
     * Flag para controlar cuando se muesta el dialogo de subida para poder 
     * mostrarlo en caso de que se gire la pantalla, lo que re-instancia esta 
     * clase perdiendose todos los campos no estáticos. 
     */
    private static boolean showingUploadDialog = false;
    
    /** 
     * Flag para controlar cuando se muesta el dialogo de resultados para poder 
     * mostrarlo en caso de que se gire la pantalla, lo que re-instancia esta 
     * clase perdiendose todos los campos no estáticos. 
     */
    private static boolean showingResultDialog = false;
    
    /** 
     * Flag de token invalido usado para controlar cuando el token de sesion 
     * es invalido y es necesario borrarlo y volver a {@link LoginActivity} 
     * tras informar al usuario del error con su sesion. 
     */
    private static boolean invalidToken = false;
    
	/**
	 * Carga y configura los elementos de la UI
	 *
	 * @param savedInstanceState
	 *            the saved instance state
	 * @see <a href=
	 *      "http://developer.android.com/intl/es/guide/topics/media/camera.html">
	 *      Camera</a>
	 * @see <a href=
	 *      "http://developer.android.com/intl/es/training/camera/photobasics.html">
	 *      Photo Basics</a>
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
            	BufferedReader br = null;
                try {

                    br = new BufferedReader(new FileReader(tokenFile));

                    campaignName = br.readLine();

                    headerCampaign.setText(campaignName);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
					if (br !=null) {
						try {
							br.close();
						} catch (IOException e) {}
					}
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
                    borrarSesionToken();
                }

            }
        }).create();
        ResultDialog.setCancelable(false);
        ResultDialog.setCanceledOnTouchOutside(false);
        ResultDialog.setTitle(R.string.result_dialog_title);

        Log.d(TAG, "Activity created successfully");
        
    }
    
   /** 
    * Al presionar el boton back si el {@link DrawerLayout} esta abierto lo
    * cierra, en caso contrario hace lo que haría por defecto
    * @see android.support.v4.app.FragmentActivity#onBackPressed()
    */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_main);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Intenta tomar fotografías, comprueba los posibles errores en el campo de
     * numero de hoja de firmas y si todo es correcto lanza el método 
     * {@link MainActivity#dispatchTakePictureIntent()} para tomar las 
     * fotografías. 
     */
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

    /**
     * Comprueba que un numero de hoja de firmas se valido, este se considera 
     * valido si es un número mayor que 0 
     *
     * @param signPaper numero de hoja de firmas a comprobar
     * @return true, si la hoja de firmas es valida
     */
    private boolean isSignPaperValid(String signPaper){
        long numSignPaperTemp = 0;
        try{
            numSignPaperTemp = Long.parseLong(signPaper);
        } catch (NumberFormatException e){}
        return  numSignPaperTemp > 0;
    }

    /**
     * Lanza un intent para tomar una fotografía. Para ello comprueba que haya
     * alguna aplicación capaz de tomar fotografías, tras esto comprueba si se
     * ha tomado la foto delantera, en caso negativo lanza el {@link Intent} 
     * para tomarla indicando en este como extra la ruta donde se almacenará
     * dicha foto y dandole permisos a la aplicación de camara para escribir en
     *  el directorio privado de esta aplicación. Si la fotografía delantera ya
     *  ha sido tomada hace lo mismo con la trasera.
     */
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
	 * Devuelve el {@link Uri} para guardar la foto.
	 *
	 * @return the output media file uri
	 * @see <a href=
	 *      "https://developer.android.com/reference/android/support/v4/content/FileProvider.html">
	 *      Referencia</a>
	 */
    private Uri getOutputMediaFileUri() {

        return FileProvider.getUriForFile(MainActivity.this ,CAPTURE_IMAGE_FILE_PROVIDER, getOutputMediaFile());
    }

    /**
     * Crea el {@link File} para una fotografia, dicho archivo se almacenará en
     *  un directorio interno de la aplicación y tendra como nombre DNI_ + 
     *  fecha y hora actual del sistema+ .jpg donde la fecha y hora actual tiene
     *  el siguiente formato "ddMMyyyy_HHmmss_SSSS", ver 
     *  {@link SimpleDateFormat} para más informacióm.
     *
     * @return the output media file
     */
    private File getOutputMediaFile() {

        
        File photoStorageDir;
        
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
	 * Metodo invocado cuando un Intent de retorna (ya ha tomado una fotografia
	 * ) en el se comprueba que se haya romado correctamente, en cuyo caso si no
	 * se ha tomado la segunda fotografía se vuelve al método
	 * {@link MainActivity#dispatchTakePictureIntent()}, si ambas han sido
	 * tomadas se crean los ficheros con las rutas usadas por los Intents y se
	 * eliminan los permisos sobre dichas rutas.
	 * 
	 *
	 * @param requestCode
	 *            RequestCode del intent que arrancó la toma de fotografías
	 * @param resultCode
	 *            Codigo de resultado del intent
	 * @param data
	 *            datos devueltos
	 * 
	 * @see <a href=
	 *      "https://medium.com/@a1cooke/using-v4-support-library-fileprovider-and-camera-intent-a45f76879d61#.6wu8mv2ya">
	 *      FileProvider Example</a>
	 *
	 * 
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

    /**
     * Método que efectua la subida de las fotografías, para ello muestar un 
     * dialogo para informar al usuario del progreso de esta subida y realiza
     * la petición al servidor, añadiendo ademas de las fotografías el nombre 
     * de la campaña, el número de la hoja de firmas (ambos en Base64) y el 
     * token de sesión
     * 
	 * @see <a href="https://github.com/koush/ion">Referencia librería para
	 *      establecer las conexiones</a>
     */
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

            
            BufferedReader br = null;
            try {

                br = new BufferedReader(new FileReader(tokenFile));
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


            } catch (Exception e) {
                e.printStackTrace();
                
                Toast t = Toast.makeText(MainActivity.this, "Error con su sesión de usuario", Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v != null) {
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                borrarSesionToken();
            } 

            CheckSessionActivity.trustServerCertificate(MainActivity.this);
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
                                            try {
	                                            byte[] datos = Base64.decode(message64, Base64.NO_WRAP);
	                                            message = new String(datos);
	                                            resultDialogMessage = message;
                                            }catch (Exception e2){
                                            	resultDialogMessage = getString(R.string.toastWrongUploadingResult);

                                            }
                                            if (result.getHeaders().code() == 404){
                                               
                                                invalidToken = true;
                                            }
                                            
                                        } else {
                                            resultDialogMessage = getString(R.string.toastWrongUploadingResult);

                                        }

                                    } else {
                                        resultDialogMessage = getString(R.string.toastWrongUploadingResult);
                                    }
                                }


                            } else {
                                resultDialogMessage = getString(R.string.toastCorrectUploadingResult);
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
            
            Toast t = Toast.makeText(MainActivity.this, "Error con su sesión de usuario", Toast.LENGTH_LONG);
            TextView v = (TextView) t.getView().findViewById(android.R.id.message);
            if (v != null) {
                v.setGravity(Gravity.CENTER);
            }
            t.show();
            borrarSesionToken();
        }



    }

    /**
     * Method used to cancel pending uploads.
     */
    private void resetUpload() {
        //To cancel pending uploads


        upload.cancel();

        upload = null;

       
    }

    /**
     * Reinicia el tomado de fotos poniendo todo a null.
     */
    private void resetPhotoTaking ()
    {
        photoFileBack =null;
        photoFilePathBack = null;
        photoFilePathFront=null;
        photoFileFront=null;
    }
    
    /**
     * Reinicia el tomado de fotos poniendo todo a null.
     */
    private void borrarSesionToken(){
    	 File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");

         if (tokenFile != null) {
        	 if (tokenFile.exists()) {
        		 if(!tokenFile.delete()){
        	         
                     Log.d(TAG, "Error borrando archivo token");
                 }
			}
         }
        	 
         Intent intent = new Intent(MainActivity.this, LoginActivity.class);
         startActivity(intent);
         finish();
    }
    /**
     * Borra todo el contenido de la carpeta de fotos interna de la aplicación.
     */
    private void deletePhoto() {

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

    /**
     * Muestra achivos en directorio.
     *
     * @param dir the dir
     */
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
   

    
    /**
     * Al entrar en primer plano selecciona el campo Main Item de la
     * {@link NavigationView} y además comprueba si se estaba mostrando el 
     * dialogo {@link MainActivity#UploadDialog}, en cuyo caso lo 
     * despliega y lo mismo con {@link MainActivity#ResultDialog}. 
     * 
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
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
    
    /** 
  	 * Invocado al destruir la actividad, comprueba si se estaba mostrando los
     * dialogos {@link MainActivity#UploadDialog} y 
     * {@link MainActivity#ResultDialog}, en cuyo caso 
     * fija los flag {@link MainActivity#showingUploadDialog} y 
     * {@link MainActivity#showingResultDialog} a true (respectivamente) para 
     * que se muestre cuando vuelva y cierra el dialogo actual. 
     * @see android.support.v7.app.AppCompatActivity#onDestroy()
     */
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

    
    /** 
     * Controla las acciones a tomar al seleccionar los distintos campos de la 
     * {@link NavigationView} que permiten cambiar entre actividades
     * @see android.support.design.widget.NavigationView.OnNavigationItemSelectedListener#onNavigationItemSelected(android.view.MenuItem)
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