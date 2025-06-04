package com.bigo143.echodiary;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_MOOD_ENABLED = "mood_enabled";

    private static final String KEY_SUMMARIZATION_STYLE = "summarization_style";
    private static final String KEY_MOOD_DETECTION = "mood_detection";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_VOICE_CONTROL = "voice_control";
    private static final String KEY_THEME = "theme";

    private static final String DEFAULT_SUMMARIZATION = "Casual";
    private static final String DEFAULT_MOOD = "On";
    private static final String DEFAULT_LANGUAGE = "English";
    private static final String DEFAULT_VOICE = "Off";
    private static final String DEFAULT_THEME = "Light";


    private LinearLayout dropdownSummarization, dropdownMood, dropdownLanguage, dropdownVoice, dropdownTheme;

    public SettingsFragment() {}

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

        // Summarization Style
        LinearLayout btnSummarizationStyle = view.findViewById(R.id.btn_summarization_style);
        dropdownSummarization = view.findViewById(R.id.summarization_style_options);
        btnSummarizationStyle.setOnClickListener(v -> {
            animateClick(btnSummarizationStyle);
            toggleDropdown(dropdownSummarization, () -> {
            dropdownSummarization.removeAllViews();
            String[] styles = getResources().getStringArray(R.array.summarization_styles);
                for (String style : styles) {
                    addOption(dropdownSummarization, style, () -> {
                        showToast("Selected: " + style);
                    }, KEY_SUMMARIZATION_STYLE, DEFAULT_SUMMARIZATION);
                }

            });});

        // Mood Detection
        LinearLayout btnMoodDetection = view.findViewById(R.id.btn_mood_detection);
        dropdownMood = view.findViewById(R.id.mood_detection_options);
        btnMoodDetection.setOnClickListener(v -> {
            animateClick(btnMoodDetection);
            toggleDropdown(dropdownMood, () -> {
            dropdownMood.removeAllViews();
                addOption(dropdownMood, getString(R.string.on), () -> {
                    saveMoodEnabledToPrefs(true);
                    showToast("Mood detection: On");
                }, KEY_MOOD_DETECTION, DEFAULT_MOOD);

                addOption(dropdownMood, getString(R.string.off), () -> {
                    saveMoodEnabledToPrefs(false);
                    showToast("Mood detection: Off");
                }, KEY_MOOD_DETECTION, DEFAULT_MOOD);

            });});

        // Entry Language
        LinearLayout btnEntryLanguage = view.findViewById(R.id.btn_entry_language);
        dropdownLanguage = view.findViewById(R.id.entry_language_options);
        btnEntryLanguage.setOnClickListener(v -> {
            animateClick(btnEntryLanguage);
            toggleDropdown(dropdownLanguage, () -> {
            dropdownLanguage.removeAllViews();
                addOption(dropdownLanguage, getString(R.string.english), () -> setLanguage("en"), KEY_LANGUAGE, DEFAULT_LANGUAGE);
                addOption(dropdownLanguage, getString(R.string.filipino), () -> setLanguage("fil"), KEY_LANGUAGE, DEFAULT_LANGUAGE);
                addOption(dropdownLanguage, getString(R.string.japanese), () -> setLanguage("ja"), KEY_LANGUAGE, DEFAULT_LANGUAGE);

            });});

        // Voice Control
        LinearLayout btnVoiceControl = view.findViewById(R.id.btn_voice_control);
        dropdownVoice = view.findViewById(R.id.voice_control_options);
        btnVoiceControl.setOnClickListener(v -> {
            animateClick(btnVoiceControl);
            toggleDropdown(dropdownVoice, () -> {
            dropdownVoice.removeAllViews();
                addOption(dropdownVoice, getString(R.string.on), () -> showToast("Voice Control: On"), KEY_VOICE_CONTROL, DEFAULT_VOICE);
                addOption(dropdownVoice, getString(R.string.off), () -> showToast("Voice Control: Off"), KEY_VOICE_CONTROL, DEFAULT_VOICE);

            });});

        // Theme
        LinearLayout btnTheme = view.findViewById(R.id.btn_theme);
        dropdownTheme = view.findViewById(R.id.theme_options);
        btnTheme.setOnClickListener(v -> {
            animateClick(btnTheme);
            toggleDropdown(dropdownTheme, () -> {
            dropdownTheme.removeAllViews();
                String[] themes = getResources().getStringArray(R.array.theme_options);
                for (int i = 0; i < themes.length; i++) {
                    int finalI = i;
                    addOption(dropdownTheme, themes[i], () -> {
                        applyTheme(finalI);
                    }, KEY_THEME, DEFAULT_THEME);
                }

            });});

        // Daily Reminder Time (still dialog)
        LinearLayout btnDailyReminderTime = view.findViewById(R.id.btn_daily_reminder_time);
        btnDailyReminderTime.setOnClickListener(v -> {
            animateClick(btnDailyReminderTime);
            showTimePickerDialog();});

        // About EchoDiary (still dialog)
        LinearLayout btnAboutEchoDiary = view.findViewById(R.id.btn_about_echodiary);
        btnAboutEchoDiary.setOnClickListener(v -> {
            animateClick(btnAboutEchoDiary);
            showAboutDialog();});

        return view;
    }

    private void toggleDropdown(LinearLayout dropdown, Runnable populate) {
        if (dropdown.getVisibility() == View.VISIBLE) {
            dropdown.setVisibility(View.GONE);
        } else {
            populate.run();
            dropdown.setVisibility(View.VISIBLE);
        }
    }

    private void addOption(LinearLayout container, String text, Runnable action, String prefKey, String defaultValue) {
        TextView option = new TextView(requireContext());
        option.setText(text);
        option.setTextSize(14);
        option.setPadding(32, 24, 32, 24);
        option.setTextColor(ContextCompat.getColor(requireContext(), R.color.radio_text_color));
        option.setBackgroundResource(selectableItemBackground());
        option.setClickable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        option.setLayoutParams(params);

        // Highlight if selected
        String selectedValue = loadSelection(prefKey, defaultValue);
        if (text.equals(selectedValue)) {
            updateSelectedStyle(option, container);
        }

        option.setOnClickListener(v -> {
            updateSelectedStyle(option, container);
            saveSelection(prefKey, text);
            action.run();
        });

        container.addView(option);
    }



    private void showToast(String message) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void setLanguage(String langCode) {
        LocaleHelper.setLocale(requireContext(), langCode);

        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .create();
        loadingDialog.show();

        new android.os.Handler().postDelayed(() -> {
            loadingDialog.dismiss();
            requireActivity().finish();
            requireActivity().startActivity(requireActivity().getIntent());
        }, 3000);
    }

    private void applyTheme(int selectedIndex) {
        int mode;
        String modeName;

        if (selectedIndex == 0) {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
            modeName = "Light";
        } else if (selectedIndex == 1) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
            modeName = "Dark";
        } else {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            modeName = "System Default";
        }

        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt("theme_mode", mode)
                .putString(KEY_THEME, modeName)
                .apply();

        AppCompatDelegate.setDefaultNightMode(mode);
        requireActivity().recreate();
    }


    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            showToast("Time set: " + hourOfDay + ":" + String.format("%02d", minute));
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

    private int selectableItemBackground() {
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        return outValue.resourceId;
    }

    private boolean getMoodEnabledFromPrefs() {
        return requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MOOD_ENABLED, true);
    }

    private void saveMoodEnabledToPrefs(boolean enabled) {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MOOD_ENABLED, enabled)
                .apply();
    }

    private void animateClick(LinearLayout widget) {
        widget.animate().scaleX(0.9f).scaleY(0.9f).setDuration(350)
                .withEndAction(() -> widget.animate().scaleX(1f).scaleY(1f).setDuration(50)).start();
    }

    private void updateSelectedStyle(TextView selected, LinearLayout parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                child.setBackground(null); // reset background
                ((TextView) child).setTextColor(ContextCompat.getColor(requireContext(), R.color.radio_text_color)); // default
            }
        }

        selected.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.selected_background));
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.pale_mocha));
    }

    private void saveSelection(String key, String value) {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(key, value).apply();
    }

    private String loadSelection(String key, String defaultValue) {
        return requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, defaultValue);
    }


}
