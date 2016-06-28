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

public class InstructionsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView navigationView;
    private ImageButton expandButton, expandButton2;
    private TextView textView, textView2;
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
        String campaignName = intent.getStringExtra("campaignName");
        if (campaignName != null){
            Log.d(MainActivity.TAG, "CampaignName: "+campaignName+" TextView: "+headerCampaign);
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

        textView = (TextView) findViewById(R.id.test);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickExpandButton(textView, expandButton);
            }
        });
        expandButton = (ImageButton) findViewById(R.id.expand_button);
        expandButton.setImageResource(R.drawable.ic_expand_more_white_24dp);
        expandButton.setTag("expand_more");
        expandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onClickExpandButton(textView, expandButton);

            }
        });

        textView2 = (TextView) findViewById(R.id.test2);
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickExpandButton(textView2, expandButton2);
            }
        });
        expandButton2 = (ImageButton) findViewById(R.id.expand_button2);
        expandButton2.setImageResource(R.drawable.ic_expand_more_white_24dp);
        expandButton2.setTag("expand_more");
        expandButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onClickExpandButton(textView2, expandButton2);

            }
        });
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_instructions);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
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
    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.instructions_drawer_item);
    }

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

                startActivity(intent);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_instructions);
        drawer.closeDrawer(GravityCompat.START);
        return retVal;
    }
}
