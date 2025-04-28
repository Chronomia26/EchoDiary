package com.bigo143.echodiary;

import android.os.Bundle;
import android.view.View;
import android.view.Window;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bigo143.echodiary.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Window window = getWindow();
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_background_color));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.coffee_2));
        //hanggang dito, kay rashed to wag galawin
        Objects.requireNonNull(getSupportActionBar()).hide();

        com.bigo143.echodiary.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_calendar, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

}