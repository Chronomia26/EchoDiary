package com.bigo143.echodiary;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    // Menu item titles as constants for readability
    private static final String SUMMARIZATION_STYLE = "Summarization Style";
    private static final String MOOD_DETECTION = "Mood Detection";
    private static final String ENTRY_LANGUAGE = "Entry Language";
    private static final String DAILY_REMINDER_TIME = "Daily Reminder Time";
    private static final String VOICE_CONTROL = "Voice Control";
    private static final String APP_THEME = "App Theme";
    private static final String ABOUT_ECHODIARY = "About EchoDiary";

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
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
        // Inflate the fragment's layout
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        LinearLayout settingsMenu = view.findViewById(R.id.settings_menu);

        // Add menu items dynamically here
        addMenuItem(settingsMenu, SUMMARIZATION_STYLE);
        addMenuItem(settingsMenu, MOOD_DETECTION);
        addMenuItem(settingsMenu, ENTRY_LANGUAGE);
        addMenuItem(settingsMenu, DAILY_REMINDER_TIME);
        addMenuItem(settingsMenu, VOICE_CONTROL);
        addMenuItem(settingsMenu, APP_THEME);
        addMenuItem(settingsMenu, ABOUT_ECHODIARY);

        return view;
    }

    // Method to add each menu item as a clickable TextView
    private void addMenuItem(LinearLayout settingsMenu, String title) {
        TextView textView = new TextView(getContext(), null, R.style.SettingsMenuItem);
        textView.setText(title);
        textView.setGravity(Gravity.CENTER_VERTICAL);  // Center vertically
        textView.setTextSize(20); // Larger text
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.coffee_brown));
        textView.setPadding(32, 24, 32, 24); // More padding around the text
        textView.setOnClickListener(v -> onMenuItemClicked(title));

        // Set icon (resized)
        Drawable icon = getIconDrawableForTitle(title);
        if (icon != null) {
            int size = getResources().getDimensionPixelSize(R.dimen.icon_size); // define in dimens.xml
            icon.setBounds(0, 0, size, size);
            textView.setCompoundDrawables(icon, null, null, null);
            textView.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.icon_text_padding));  // Adjustable padding
        }

        textView.setBackgroundResource(selectableItemBackground()); // ripple effect
        settingsMenu.addView(textView);

        // Set layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 12;
        textView.setLayoutParams(params);
    }

    private int selectableItemBackground() {
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        return outValue.resourceId;
    }

    private Drawable getIconDrawableForTitle(String title) {
        int iconResId = getIconResIdForTitle(title);
        Drawable icon = ContextCompat.getDrawable(requireContext(), iconResId);
        if (icon != null) {
            // Apply coffee_brown color to the icon
            int color = ContextCompat.getColor(requireContext(), R.color.coffee_brown);
            icon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN); // Apply color filter
        }
        return icon;
    }

    private int getIconResIdForTitle(String title) {
        switch (title) {
            case SUMMARIZATION_STYLE:
                return R.drawable.baseline_format_list_bulleted_24;
            case MOOD_DETECTION:
                return R.drawable.baseline_emoji_emotions_24;
            case ENTRY_LANGUAGE:
                return R.drawable.baseline_language_24;
            case DAILY_REMINDER_TIME:
                return R.drawable.baseline_alarm_24;
            case VOICE_CONTROL:
                return R.drawable.baseline_mic_24;
            case APP_THEME:
                return R.drawable.baseline_brightness_6_24;
            case ABOUT_ECHODIARY:
                return R.drawable.baseline_info_24;
            default:
                return R.drawable.baseline_settings_24; // fallback
        }
    }

    // Handle clicks on menu items
    private void onMenuItemClicked(String title) {
        // Handle each menu item click, for example, open dialogs
        switch (title) {
            case SUMMARIZATION_STYLE:
                showListDialog("Choose Summarization Style", new String[]{"Formal", "Casual", "Concise"});
                break;
            case MOOD_DETECTION:
                showToggleDialog("Enable Mood Detection");
                break;
            case ENTRY_LANGUAGE:
                showListDialog("Select Entry Language", new String[]{"English", "Filipino", "Japanese"});
                break;
            case DAILY_REMINDER_TIME:
                showTimePickerDialog();
                break;
            case VOICE_CONTROL:
                showToggleDialog("Enable Voice Control");
                break;
            case APP_THEME:
                showListDialog("App Theme", new String[]{"Light", "Dark", "System Default"});
                break;
            case ABOUT_ECHODIARY:
                showAboutDialog();
                break;
        }
    }

    private void showListDialog(String title, String[] options) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    // Save selection if needed
                })
                .show();
    }

    private void showToggleDialog(String title) {
        final boolean[] isChecked = {false};
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(new String[]{"On", "Off"}, isChecked[0] ? 0 : 1, (dialog, which) -> {
                    isChecked[0] = (which == 0);
                })
                .setPositiveButton("OK", null)
                .show();
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            // Save the time if needed
        }, 8, 0, true);
        timePickerDialog.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About EchoDiary")
                .setMessage("EchoDiary v1.0\nDeveloped by Bigo143\nAll rights reserved.")
                .setPositiveButton("OK", null)
                .show();
    }
}
