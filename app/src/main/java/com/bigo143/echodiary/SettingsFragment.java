package com.bigo143.echodiary;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_MOOD_ENABLED = "mood_enabled";


    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString("ARG_PARAM1", param1);
        args.putString("ARG_PARAM2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        LinearLayout btnSummarizationStyle = view.findViewById(R.id.btn_summarization_style);
        btnSummarizationStyle.setOnClickListener(v -> {
            animateClick(btnSummarizationStyle);
            showListDialog(getString(R.string.summarization_style),
                    getResources().getStringArray(R.array.summarization_styles));
        });


        LinearLayout btnMoodDetection = view.findViewById(R.id.btn_mood_detection);
        btnMoodDetection.setOnClickListener(v -> {
            animateClick(btnMoodDetection);
            showMoodDetection(getString(R.string.mood_detection));
        });

        LinearLayout btnEntryLanguage = view.findViewById(R.id.btn_entry_language);
        btnEntryLanguage.setOnClickListener(v -> {
            animateClick(btnEntryLanguage);
            showLanguageDialog();
        });

        LinearLayout btnDailyReminderTime = view.findViewById(R.id.btn_daily_reminder_time);
        btnDailyReminderTime.setOnClickListener(v -> {
            animateClick(btnDailyReminderTime);
            showTimePickerDialog();
        });

        LinearLayout btnVoiceControl = view.findViewById(R.id.btn_voice_control);
        btnVoiceControl.setOnClickListener(v -> {
            animateClick(btnVoiceControl);
            showToggleDialog(getString(R.string.voice_control));
        });

        LinearLayout btnTheme = view.findViewById(R.id.btn_theme);
        btnTheme.setOnClickListener(v -> {
            animateClick(btnTheme);
            showListDialog(getString(R.string.theme),
                    getResources().getStringArray(R.array.theme_options));
        });

        LinearLayout btnAboutEchoDiary = view.findViewById(R.id.btn_about_echodiary);
        btnAboutEchoDiary.setOnClickListener(v -> {
            animateClick(btnAboutEchoDiary);
            showAboutDialog();
        });

        return view;
    }

    private void animateClick(LinearLayout widget) {
        widget.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50)
                .withEndAction(() -> widget.animate().scaleX(1f).scaleY(1f).setDuration(350)).start();
    }

    private int selectableItemBackground() {
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        return outValue.resourceId;
    }

    private void onMenuItemClicked(String title) {
        if (title.equals(getString(R.string.summarization_style))) {
            showListDialog(getString(R.string.summarization_style),
                    getResources().getStringArray(R.array.summarization_styles));
        } else if (title.equals(getString(R.string.mood_detection))) {
            showMoodDetection(getString(R.string.mood_detection));
        } else if (title.equals(getString(R.string.entry_language))) {
            showLanguageDialog();
        } else if (title.equals(getString(R.string.daily_reminder_time))) {
            showTimePickerDialog();
        } else if (title.equals(getString(R.string.voice_control))) {
            showToggleDialog(getString(R.string.voice_control));
        } else if (title.equals(getString(R.string.theme))) {
            showListDialog(getString(R.string.theme),
                    getResources().getStringArray(R.array.theme_options));
        } else if (title.equals(getString(R.string.about_echodiary))) {
            showAboutDialog();
        }
    }

    private void showMoodDetection(String title) {
        final boolean[] isChecked = {getMoodEnabledFromPrefs()};
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(
                        new String[]{getString(R.string.on), getString(R.string.off)},
                        isChecked[0] ? 0 : 1,
                        (dialog, which) -> isChecked[0] = (which == 0)
                )
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    saveMoodEnabledToPrefs(isChecked[0]);
                    // Inform CalendarFragment or relevant UI to update mood display
                })
                .show();
    }

    private boolean getMoodEnabledFromPrefs() {
        return requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MOOD_ENABLED, true); // default enabled
    }

    private void saveMoodEnabledToPrefs(boolean enabled) {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MOOD_ENABLED, enabled)
                .apply();
    }

    private void showLanguageDialog() {
        String[] languages = {
                getString(R.string.english),
                getString(R.string.filipino),
                getString(R.string.japanese)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.entry_language))
                .setItems(languages, (dialog, which) -> {
                    String langCode = "en";
                    switch (which) {
                        case 0: langCode = "en"; break;
                        case 1: langCode = "fil"; break;
                        case 2: langCode = "ja"; break;
                    }

                    // Set locale
                    LocaleHelper.setLocale(requireContext(), langCode);

                    // Show loading dialog
                    AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                            .setView(R.layout.dialog_loading)
                            .setCancelable(false)
                            .create();
                    loadingDialog.show();

                    // Delay for 5 seconds, then recreate activity
                    new android.os.Handler().postDelayed(() -> {
                        loadingDialog.dismiss();
                        requireActivity().finish();
                        requireActivity().startActivity(requireActivity().getIntent());
                    }, 3000);
                })
                .show();
    }




    private void showListDialog(String title, String[] options) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    if (title.equals(getString(R.string.theme))) {
                        // Save preference and apply theme
                        int mode;
                        if (which == 0) {
                            mode = AppCompatDelegate.MODE_NIGHT_NO; // Light mode
                        } else if (which == 1) {
                            mode = AppCompatDelegate.MODE_NIGHT_YES; // Dark mode
                        } else {
                            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // System default
                        }

                        // Save to SharedPreferences
                        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putInt("theme_mode", mode)
                                .apply();

                        AppCompatDelegate.setDefaultNightMode(mode);

                        // Restart activity to apply theme
                        requireActivity().recreate();
                    }
                })
                .show();
    }


    private void showToggleDialog(String title) {
        final boolean[] isChecked = {false};
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(
                        new String[]{getString(R.string.on), getString(R.string.off)},
                        isChecked[0] ? 0 : 1,
                        (dialog, which) -> isChecked[0] = (which == 0)
                )
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }


    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            // Handle time
        }, 8, 0, true);
        timePickerDialog.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.about_echodiary))
                .setMessage(getString(R.string.about_message))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }


}
