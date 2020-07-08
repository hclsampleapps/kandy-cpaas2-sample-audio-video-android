package com.hcl.kandy.cpass.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hcl.kandy.cpass.App;
import com.hcl.kandy.cpass.R;
import com.hcl.kandy.cpass.call.CallFragment;
import com.hcl.kandy.cpass.utils.jwt.JWT;

public class HomeActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Fragment callFragment = CallFragment.newInstance();
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        showProgressBar("");
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Bundle extras = getIntent().getExtras();
        String idToken = null;
        String accessToken = null;
        String baseUrl = null;
        if (extras != null) {
            idToken = extras.getString(LoginActivity.id_token);
            accessToken = extras.getString(LoginActivity.access_token);
            baseUrl = extras.getString(LoginActivity.base_url);
        }

        App app = (App) getApplicationContext();
        app.setCpass(baseUrl, accessToken, idToken, new CpassListner() {
            @Override
            public void onCpassSuccess() {
                hideProgressBAr();
            }

            @Override
            public void onCpassFail() {
                hideProgressBAr();
            }
        });

        setUserInfo(idToken);

        onNavigationItemSelected(navigationView.getMenu().getItem(0));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        int id = item.getItemId();

        if (id == R.id.nav_call) {
            fragmentTransaction
                    .replace(R.id.container, callFragment).commit();

            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null)
                supportActionBar.setTitle("Voice App");

            item.setChecked(true);
            invalidateOptionsMenu();
        } else if (id == R.id.nav_logout) {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            Toast.makeText(HomeActivity.this, "Logout", Toast.LENGTH_SHORT).show();
            finish();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setUserInfo(String idToken) {
        JWT jwt = new JWT(idToken);
        String email = jwt.getClaim("email").asString();
        String name = jwt.getClaim("name").asString();

        NavigationView navigationView = findViewById(R.id.nav_view);
        View hView = navigationView.getHeaderView(0);

        TextView tvName = hView.findViewById(R.id.tvName);
        TextView tvEmail = hView.findViewById(R.id.tvEmail);
        tvEmail.setText(email);
        tvName.setText(name);
    }

    public interface CpassListner {
        void onCpassSuccess();

        void onCpassFail();
    }
}
