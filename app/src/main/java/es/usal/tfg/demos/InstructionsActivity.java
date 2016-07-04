/*
 * Archivo: InstructionsActivity.java 
 * Proyecto: Demos_Android
 * 
 * Autor: Aythami Estévez Olivas
 * Email: aythae@gmail.com
 * Fecha: 04-jul-2016
 * Repositorio GitHub: https://github.com/AythaE/Demos_Android
 */
package es.usal.tfg.demos;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Clase InstructionsActivity encargada de controlar una pantalla en la que se 
 * muestran algunas preguntas frecuentes de los usuarios para que les sirva de 
 * ayuda.
 */
public class InstructionsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    /** The navigation view. */
    private NavigationView navigationView;
    
    /** Los botones de expansion de los tres {@link TextView}. */
    private ImageButton expandButton, expandButton2, expandButton3;
    
    /** Los tres {@link TextView} de instrucciones. */
    private TextView textView, textView2, textView3;

    /** The campaign name. */
    private static String campaignName;
    
    /**
     * Creación de la actividad cargando y configurando su interfaz gráfica
     * @see android.support.v7.app.AppCompatActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_instructions);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_instructions);
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

        navigationView = (NavigationView) findViewById(R.id.nav_view_instructions);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        TextView headerCampaign = (TextView) header.findViewById(R.id.campaign_header_textview);
        Intent intent = getIntent();
        campaignName = intent.getStringExtra("campaignName");
        if (campaignName != null){
            Log.d(MainActivity.TAG, "CampaignName: "+campaignName+" TextView: "+headerCampaign);
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
                }
                finally {
					if (br!= null) {
						try {
							br.close();
						} catch (IOException e) {}
					}
				}
            }
        }

        textView = (TextView) findViewById(R.id.bad_picture_instruction);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickExpandButton(textView, expandButton);
            }
        });
        expandButton = (ImageButton) findViewById(R.id.bad_picture_expand);
        expandButton.setImageResource(R.drawable.ic_expand_more_white_24dp);
        expandButton.setTag("expand_more");
        expandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onClickExpandButton(textView, expandButton);

            }
        });

        textView2 = (TextView) findViewById(R.id.photo_destination_instructions);
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickExpandButton(textView2, expandButton2);
            }
        });
        expandButton2 = (ImageButton) findViewById(R.id.photo_destination_expand);
        expandButton2.setImageResource(R.drawable.ic_expand_more_white_24dp);
        expandButton2.setTag("expand_more");
        expandButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onClickExpandButton(textView2, expandButton2);

            }
        });

        textView3 = (TextView) findViewById(R.id.how_to_retrieve_instructions);
        textView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickExpandButton(textView3, expandButton3);
            }
        });
        expandButton3 = (ImageButton) findViewById(R.id.how_to_retrieve_expand);
        expandButton3.setImageResource(R.drawable.ic_expand_more_white_24dp);
        expandButton3.setTag("expand_more");
        expandButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onClickExpandButton(textView3, expandButton3);

            }
        });
    }
    
    /** 
     * Al presionar el boton back si el {@link DrawerLayout} esta abierto lo
     * cierra, en caso contrario hace lo que haría por defecto
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_instructions);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
     
     /**
      * Ejecutado al tocar en cualquiera de los {@link TextView} o ExpandButtons
      * Expande o contrae el TextView tv en funcion de su estado actual y cambia
      * el icono del {@link ImageButton} imButton segun su estado
      *
      * @param tv el {@link TextView}
      * @param imButton el {@link ImageButton}
      */
     private void onClickExpandButton(TextView tv, ImageButton imButton){
         Log.d(MainActivity.TAG, "cambiando icono");
         if (imButton.getTag().equals("expand_more")){
             tv.setMaxLines(Integer.MAX_VALUE);
             imButton.setImageResource(R.drawable.ic_expand_less_white_24dp);
             imButton.setTag("expand_less");
         } else if (imButton.getTag().equals("expand_less")){
             tv.setMaxLines(2);
             imButton.setImageResource(R.drawable.ic_expand_more_white_24dp);
             imButton.setTag("expand_more");
         }
     }
    
    /**
     * Al entrar en primer plano selecciona el campo Instructions Item de la
     * {@link NavigationView}
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.instructions_drawer_item);
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
        boolean retVal=true;
        if (id == R.id.login_drawer_item) {

            Intent intent = new Intent(InstructionsActivity.this, LoginActivity.class);
            startActivity(intent);


        } else if (id == R.id.register_drawer_item) {

            Intent intent = new Intent(InstructionsActivity.this, RegisterActivity.class);
            startActivity(intent);


        } else if (id == R.id.instructions_drawer_item) {

        } else if (id == R.id.main_drawer_item) {
            File tokenFile = new File(getFilesDir().getAbsolutePath() + "/.token");


            if (!tokenFile.exists()) {
                Toast t = Toast.makeText(InstructionsActivity.this, R.string.toastMainForbidden, Toast.LENGTH_LONG);
                TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                if (v!=null){
                    v.setGravity(Gravity.CENTER);
                }
                t.show();
                retVal = false;
            }
            else{

                Intent intent = new Intent(InstructionsActivity.this, MainActivity.class);
                intent.putExtra("campaignName", campaignName);
                startActivity(intent);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_instructions);
        drawer.closeDrawer(GravityCompat.START);
        return retVal;
    }
}
