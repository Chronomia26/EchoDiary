package com.bigo143.echodiary;

import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fab;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int mode = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);

        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.coffee_2));
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setupKeyboardVisibilityListener();

        setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayShowTitleEnabled(false); // Remove default title








        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> showBottomDialog());
        fab.setOnLongClickListener(view -> {
            // 🚀 Long press detected: go directly to NewJournalActivity
            Intent intent = new Intent(MainActivity.this, NewJournalActivity.class);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    MainActivity.this, R.anim.slide_up, R.anim.slide_down
            );
            startActivity(intent, options.toBundle());
            return true; // important to consume the long click
        });


        /*View customToolbar = getLayoutInflater().inflate(R.layout.toolbar_custom, null);
        toolbar.addView(customToolbar);

        ImageView profileIcon = customToolbar.findViewById(R.id.toolbar_profile);
        TextView profileName = customToolbar.findViewById(R.id.toolbar_title);

        // TODO: Load real profile data
        profileName.setText("John Doe"); // Replace with user's name

        profileIcon.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT)); */



        // Set up navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragmentAct()); // Changed from HomeFragment
            navigationView.setCheckedItem(R.id.nav_home);
        }

        /*
        ImageView helpIcon = new ImageView(this);
        helpIcon.setImageResource(R.drawable.baseline_help_outline_24); // your icon
        helpIcon.setPadding(24, 0, 24, 0);
        helpIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT,
                Gravity.END
        );
        helpIcon.setLayoutParams(layoutParams);
        helpIcon.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Help clicked", Toast.LENGTH_SHORT).show();
        });
        toolbar.addView(helpIcon);

        */

        // Handle Drawer Navigation
        setupDrawerNavigation();

        // Set up bottom navigation view
        setupBottomNavigationView();

        // Handle Draw();

        // Floating Action Button click listener to show bottom sheet
        fab.setOnClickListener(view -> showBottomDialog());
    }

    // Set up drawer navigation behavior
    private void setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = getDrawerSelectedFragment(item.getItemId());
            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
                drawerLayout.closeDrawers(); // Close drawer after selection
            }
            return selectedFragment != null;
        });
    }

    // Return fragment based on selected drawer item ID
    private Fragment getDrawerSelectedFragment(int itemId) {
        if (itemId == R.id.nav_home) {
            return new HomeFragmentAct(); // Changed from HomeFragment
        } else if (itemId == R.id.nav_settings) {
            return new SettingsFragment();
        } /*else if (itemId == R.id.nav_about) {
            return new AboutFragment();
        } else if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }*/ else {
            return null;
        }
    }



    // Set up bottom navigation behavior
    private void setupBottomNavigationView() {
        bottomNavigationView.setBackground(null); // Optional styling

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = getSelectedFragment(item.getItemId());
            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
            }
            return true;
        });
    }


    // Return fragment based on selected item ID (revised from switch to if-else)
    private Fragment getSelectedFragment(int itemId) {
        if (itemId == R.id.home) {
            return new HomeFragmentAct(); // Changed from HomeFragment
        } else if (itemId == R.id.calendar) {
            return new CalendarFragment();
        } else if (itemId == R.id.activity) {
            return new ActivityFragment();
        } else if (itemId == R.id.diary) {
            return new DiaryFragment();
        } else {
            return null;
        }
    }

    // Replace current fragment with new one
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();

        // Check if fragment is one where bottom UI should be hidden
        boolean shouldHideBottomUI = fragment instanceof SettingsFragment || fragment instanceof AboutFragment;

        // Hide or show BottomNavigationView
        bottomNavigationView.setVisibility(shouldHideBottomUI ? View.GONE : View.VISIBLE);

        // Hide or show FloatingActionButton
        fab.setVisibility(shouldHideBottomUI ? View.GONE : View.VISIBLE);

        // Hide or show FAB background and nav frame (optional styling views)
        View fabBg = findViewById(R.id.fab_background);
        View bottomNavFrame = findViewById(R.id.bottom_nav_frame);

        if (fabBg != null) fabBg.setVisibility(shouldHideBottomUI ? View.GONE : View.VISIBLE);
        if (bottomNavFrame != null) bottomNavFrame.setVisibility(shouldHideBottomUI ? View.GONE : View.VISIBLE);
    }




    // Display bottom sheet dialog for upload options
    private void showBottomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout videoLayout = dialog.findViewById(R.id.layoutVideo);
        LinearLayout shortsLayout = dialog.findViewById(R.id.layoutShorts);
        LinearLayout liveLayout = dialog.findViewById(R.id.layoutLive);
        //ImageView cancelButton = dialog.findViewById(R.id.cancelButton);

        videoLayout.setOnClickListener(v -> {
            dialog.dismiss(); // Close the bottom sheet

            Calendar today = Calendar.getInstance();

            DatePickerDialog datePicker = new DatePickerDialog(
                    MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDay = Calendar.getInstance();
                        selectedDay.set(year, month, dayOfMonth);

                        Intent intent = new Intent(MainActivity.this, SummaryActivity.class);
                        intent.putExtra("summaryDayMillis", selectedDay.getTimeInMillis());
                        startActivity(intent);
                    },
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH)
            );

            datePicker.show();
        });


        shortsLayout.setOnClickListener(v -> {
            dialog.dismiss();
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, NewJournalActivity.class);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_up, R.anim.slide_down);
            startActivity(intent, options.toBundle());
        });

        liveLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });

        //cancelButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }

    private void setupKeyboardVisibilityListener() {
        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean isKeyboardOpen = keypadHeight > screenHeight * 0.15;

            View fab = findViewById(R.id.fab);
            View fabBg = findViewById(R.id.fab_background);
            View bottomNavFrame = findViewById(R.id.bottom_nav_frame); // ⬅️ Add this ID in XML


            if (isKeyboardOpen) {
                fab.animate().alpha(0f).setDuration(150).withEndAction(() -> fab.setVisibility(View.GONE)).start();
                fabBg.setVisibility(View.GONE);
                bottomNavFrame.setVisibility(View.GONE);
            } else {
                fab.setVisibility(View.VISIBLE);
                fab.animate().alpha(1f).setDuration(150).start();
                fabBg.setVisibility(View.VISIBLE);
                bottomNavFrame.setVisibility(View.VISIBLE);
            }
        });
    }




    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

}
