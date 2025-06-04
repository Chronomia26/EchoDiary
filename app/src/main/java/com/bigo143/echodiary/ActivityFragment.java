package com.bigo143.echodiary;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Typeface;
import android.util.Log; // Added Log import

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Switch;

// Import necessary classes for JSON (Only if you still use them elsewhere in this file)
// import org.json.JSONException;
// import org.json.JSONObject;

// Import the GeminiApiHelper and its Callback interface
import com.bigo143.echodiary.GeminiApiHelper;
import com.bigo143.echodiary.GeminiApiHelper.ApiResponseCallback;


public class ActivityFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart;
    private TextView monthText;
    private TextView noDataText;
    private final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    private final Map<String, Long> appUsageMap = new LinkedHashMap<>();
    private final Map<String, Integer> appColorMap = new HashMap<>();

    private LinearLayout progressBarList;
    private ImageButton btnProgress, btnPie, btnBar;
    private TextView leftDay, rightDay;
    private LinearLayout dateContainer;
    private HorizontalScrollView calendarScrollView;
    private Button btnShowAll, btnHide;
    private LinearLayout progressBarsContainer;

    // Advisor UI elements
    private LinearLayout advisorLayout;
    private ImageView characterIcon;
    private TextView advisorBubble;
    private Switch advisorToggle;
    private static final String PREF_ADVISOR_ENABLED = "advisor_enabled";

    // Executor for background tasks (still needed for other background tasks like data loading)
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    // Handler to post results back to the main thread (still needed for UI updates)
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    // List of all days with data, sorted chronologically
    private List<Calendar> daysWithData = new ArrayList<>();
    private int selectedDayIndex = -1; // index in daysWithData
    private String currentChartType = "progress";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        monthText = view.findViewById(R.id.monthText);
        noDataText = view.findViewById(R.id.noDataText);
        progressBarList = view.findViewById(R.id.progressBarList);
        btnProgress = view.findViewById(R.id.btnProgress);
        btnPie = view.findViewById(R.id.btnPie);
        btnBar = view.findViewById(R.id.btnBar); // Corrected ID from R.id.barChart to R.id.btnBar
        leftDay = view.findViewById(R.id.leftDay);
        rightDay = view.findViewById(R.id.rightDay);
        dateContainer = view.findViewById(R.id.dateContainer);
        calendarScrollView = view.findViewById(R.id.calendarScrollView);

        // Add buttons for progress bar list
        btnShowAll = view.findViewById(R.id.btnShowAll);
        btnHide = view.findViewById(R.id.btnHide);
        progressBarsContainer = view.findViewById(R.id.progressBarsContainer);

        // Advisor UI elements
        advisorLayout = view.findViewById(R.id.advisorLayout);
        characterIcon = view.findViewById(R.id.character_icon);
        advisorBubble = view.findViewById(R.id.advisor_bubble);
        advisorToggle = view.findViewById(R.id.advisor_toggle);


        btnProgress.setOnClickListener(v -> {
            currentChartType = "progress";
            showChart(currentChartType);
        });
        btnPie.setOnClickListener(v -> {
            currentChartType = "pie";
            showChart(currentChartType);
        });
        btnBar.setOnClickListener(v -> {
            currentChartType = "bar";
            showChart(currentChartType);
        });

        setupAdvisor(); // Setup advisor toggle and initial state

        buildDaysWithData();
        selectedDayIndex = getTodayOrLatestIndex();
        updateMonthText();
        setupCalendarRow();
        loadUsageForSelectedDay(view); // This will now trigger data loading on background thread and advice update
        showChart(currentChartType); // Keep this here to set initial chart visibility based on default type
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Shutdown the executor service when the fragment view is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Build the list of all days with data, sorted, using real usage data
    private void buildDaysWithData() {
        daysWithData.clear();
        Set<Long> availableDays = getAvailableUsageDays();
        for (Long millis : availableDays) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            daysWithData.add((Calendar) cal.clone());
        }
        Collections.sort(daysWithData, Comparator.comparingLong(Calendar::getTimeInMillis));
    }

    // Returns the index of today in daysWithData, or the latest day before today if today is not present
    private int getTodayOrLatestIndex() {
        Calendar today = Calendar.getInstance();
        int latestBeforeToday = -1;
        for (int i = 0; i < daysWithData.size(); i++) {
            Calendar cal = daysWithData.get(i);
            if (isSameDay(cal, today)) {
                return i;
            }
            if (cal.before(today)) {
                latestBeforeToday = i;
            }
        }
        if (latestBeforeToday != -1) return latestBeforeToday;
        return daysWithData.size() > 0 ? daysWithData.size() - 1 : -1; // Handle empty daysWithData
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void setupCalendarRow() {
        if (daysWithData.isEmpty()) {
            leftDay.setVisibility(View.GONE);
            rightDay.setVisibility(View.GONE);
            calendarScrollView.setVisibility(View.GONE);
            dateContainer.removeAllViews(); // Clear any residual views
            updateMonthText(); // Update month text to reflect no data
            return;
        }

        leftDay.setVisibility(View.VISIBLE);
        rightDay.setVisibility(View.VISIBLE);
        calendarScrollView.setVisibility(View.VISIBLE);


        int leftIdx = 0;
        int rightIdx = daysWithData.size() - 1; // Corrected to be the actual last index
        Calendar earliestDay = daysWithData.get(leftIdx);
        Calendar latestDay = daysWithData.get(rightIdx);


        leftDay.setText(String.valueOf(earliestDay.get(Calendar.DAY_OF_MONTH)));
        leftDay.setSelected(selectedDayIndex == leftIdx);
        leftDay.setOnClickListener(v -> {
            selectedDayIndex = leftIdx;
            updateMonthText();
            updateCalendarSelection(); // Call updateCalendarSelection instead of setupCalendarRow
            loadUsageForSelectedDay(getView());
            // showChart(currentChartType); // Show chart after data loads
        });

        rightDay.setText(String.valueOf(latestDay.get(Calendar.DAY_OF_MONTH)));
        rightDay.setSelected(selectedDayIndex == rightIdx);
        rightDay.setOnClickListener(v -> {
            selectedDayIndex = rightIdx;
            updateMonthText();
            updateCalendarSelection(); // Call updateCalendarSelection instead of setupCalendarRow
            loadUsageForSelectedDay(getView());
            // showChart(currentChartType); // Show chart after data loads
        });

        dateContainer.removeAllViews();
        for (int i = leftIdx + 1; i < rightIdx; i++) {
            Calendar cal = daysWithData.get(i);
            TextView dayView = new TextView(requireContext());
            dayView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));
            dayView.setGravity(Gravity.CENTER);
            dayView.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            dayView.setTextSize(16);

            dayView.setBackgroundResource(R.drawable.day_selector);
            dayView.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.dayTextColor)); // Use color state list for selection
            dayView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            int finalI = i;
            dayView.setOnClickListener(v -> {
                selectedDayIndex = finalI;
                updateMonthText();
                updateCalendarSelection(); // Call updateCalendarSelection instead of setupCalendarRow
                loadUsageForSelectedDay(getView());
                // showChart(currentChartType); // Show chart after data loads
            });
            dateContainer.addView(dayView);
        }
        updateCalendarSelection(); // Initial selection highlight
    }

    private void updateCalendarSelection() {
        // Reset backgrounds for all day views
        leftDay.setSelected(false);
        rightDay.setSelected(false);
        for (int i = 0; i < dateContainer.getChildCount(); i++) {
            dateContainer.getChildAt(i).setSelected(false);
        }

        // Set selected state for the current selected day
        if (selectedDayIndex != -1 && selectedDayIndex < daysWithData.size()) {
            if (selectedDayIndex == 0) {
                leftDay.setSelected(true);
            } else if (selectedDayIndex == daysWithData.size() - 1) {
                rightDay.setSelected(true);
            } else if (selectedDayIndex > 0 && selectedDayIndex < daysWithData.size() - 1) { // Check bounds for middle days
                // Adjust index for middle days (since leftDay is index 0 and rightDay is last index)
                dateContainer.getChildAt(selectedDayIndex - 1).setSelected(true);
            }
        }
    }


    private void updateMonthText() {
        if (selectedDayIndex < 0 || selectedDayIndex >= daysWithData.size()) {
            monthText.setText("No Data Available"); // Set appropriate text if no day is selected
            return;
        }
        Calendar cal = daysWithData.get(selectedDayIndex);
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
        monthText.setText(monthYear);
    }

    private void loadUsageForSelectedDay(View view) {
        if (selectedDayIndex < 0 || selectedDayIndex >= daysWithData.size()) {
            // Handle case where no day is selected or available
            appUsageMap.clear();
            appColorMap.clear();
            if (noDataText != null) {
                noDataText.setText("No usage data available for this day.");
                noDataText.setVisibility(View.VISIBLE);
            }
            progressBarList.setVisibility(View.GONE);
            pieChart.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
            View appLabelContainer = getView() != null ? getView().findViewById(R.id.appLabelContainer) : null;
            if (appLabelContainer != null) {
                appLabelContainer.setVisibility(View.GONE);
            }
            // Update advisor if no data
            updateAdvisorMessage();
            return; // Exit if no valid day is selected
        }

        Calendar cal = daysWithData.get(selectedDayIndex);
        loadUsageForDay(
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.YEAR),
                view
        );
        // showChart(currentChartType); // Removed from here, called after data loads
    }

    // Only one chart visible at a time, progress is default
    private void showChart(String type) {
        boolean isProgress = type.equals("progress");
        boolean hasData = !appUsageMap.isEmpty();

        progressBarList.setVisibility(isProgress ? View.VISIBLE : View.GONE);
        if (btnShowAll != null) btnShowAll.setVisibility(isProgress && hasData && getFilteredAndSortedUsage(0).size() > 5 ? View.VISIBLE : View.GONE); // Only show if more than 5 apps
        if (btnHide != null) btnHide.setVisibility(View.GONE); // Hide hide button initially

        pieChart.setVisibility(type.equals("pie") && hasData ? View.VISIBLE : View.GONE);
        barChart.setVisibility(type.equals("bar") && hasData ? View.VISIBLE : View.GONE);

        // noDataText visibility handled in loadUsageForDay now

        View appLabelContainer = getView() != null ? getView().findViewById(R.id.appLabelContainer) : null;
        if (appLabelContainer != null) {
            if (isProgress) {
                // For progress view, labels are drawn inside displayProgressBarList
                appLabelContainer.setVisibility(View.GONE);
            } else {
                // Only show labels if data exists and it's not the progress view
                appLabelContainer.setVisibility(hasData ? View.VISIBLE : View.GONE);
                if (hasData) {
                    displayAppLabels(getView());
                }
            }
        }

        if (isProgress && hasData) {
            displayProgressBarList(); // Redraw progress bars based on current state
        }
    }

    private void loadUsageForDay(int day, int month, int year, View view) {
        appUsageMap.clear();
        appColorMap.clear();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endTime = cal.getTimeInMillis();

        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            // Execute the data loading and processing in a background thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            // Show loading indicator and potentially hide previous data/advice
            handler.post(() -> {
                if (noDataText != null) {
                    noDataText.setVisibility(View.VISIBLE);
                    noDataText.setText("Loading Data...");
                }
                progressBarList.setVisibility(View.GONE);
                pieChart.setVisibility(View.GONE);
                barChart.setVisibility(View.GONE);
                View appLabelContainer = getView() != null ? getView().findViewById(R.id.appLabelContainer) : null;
                if (appLabelContainer != null) {
                    appLabelContainer.setVisibility(View.GONE);
                }
                // Hide advisor bubble while loading
                if (advisorBubble != null) {
                    advisorBubble.setText("Thinking..."); // Set thinking message while loading data
                    advisorBubble.setVisibility(advisorToggle != null && advisorToggle.isChecked() ? View.VISIBLE : View.GONE); // Only show if advisor is enabled
                }
                // Keep characterIcon visible if advisorLayout is visible AND advisor is checked (handles initial state)
                if (characterIcon != null) {
                    characterIcon.setVisibility(advisorToggle != null && advisorToggle.isChecked() ? View.VISIBLE : View.GONE);
                }
            });

            executor.execute(() -> {
                collectUsageData(startTime, endTime);
                generateColorsForApps();

                // Post UI updates back to the main thread
                handler.post(() -> {
                    // Hide loading indicator
                    if (noDataText != null) {
                        noDataText.setText(""); // Clear loading text
                    }

                    if (appUsageMap.isEmpty()) {
                        if (noDataText != null) {
                            noDataText.setText("No usage data available for this day.");
                            noDataText.setVisibility(View.VISIBLE);
                        }
                        progressBarList.setVisibility(View.GONE); // Ensure these are hidden
                        pieChart.setVisibility(View.GONE);
                        barChart.setVisibility(View.GONE);
                        View appLabelContainer = getView() != null ? getView().findViewById(R.id.appLabelContainer) : null;
                        if (appLabelContainer != null) {
                            appLabelContainer.setVisibility(View.GONE);
                        }

                        // Also update advisor if no data
                        updateAdvisorMessage();
                    } else {
                        displayProgressBarList();
                        displayPieChart();
                        displayBarChart();
                        showChart(currentChartType);
                        // Generate and display advisor message after data is loaded
                        updateAdvisorMessage();
                    }
                });
            });
        }
    }

    // Returns a set of millis for days that have at least one non-system app with usage
    private Set<Long> getAvailableUsageDays() {
        Set<Long> daysWithUsage = new TreeSet<>();
        if (!hasUsageStatsPermission()) return daysWithUsage;
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Scan up to 60 days back
        for (int i = 0; i < 60; i++) {
            long startTime = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            long endTime = cal.getTimeInMillis();
            Map<String, Long> usage = getUsageForDay(startTime, endTime);
            if (!usage.isEmpty()) {
                Calendar dayCal = Calendar.getInstance();
                dayCal.setTimeInMillis(startTime);
                dayCal.set(Calendar.HOUR_OF_DAY, 0);
                dayCal.set(Calendar.MINUTE, 0);
                dayCal.set(Calendar.SECOND, 0);
                dayCal.set(Calendar.MILLISECOND, 0);
                daysWithUsage.add(dayCal.getTimeInMillis());
            }
            cal.add(Calendar.DAY_OF_MONTH, -1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
        return daysWithUsage;
    }

    // Helper to get usage for a day (non-system apps only)
    private Map<String, Long> getUsageForDay(long startTime, long endTime) {
        Map<String, Long> usageMap = new HashMap<>();
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = usm.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        Map<String, Long> foregroundTimestamps = new HashMap<>();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String packageName = event.getPackageName();
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundTimestamps.put(packageName, event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (foregroundTimestamps.containsKey(packageName)) {
                    long start = foregroundTimestamps.remove(packageName);
                    long duration = event.getTimeStamp() - start;
                    if (!isSystemApp(packageName)) {
                        String appName = getAppNameFromPackage(packageName);
                        usageMap.put(appName, usageMap.getOrDefault(appName, 0L) + duration);
                    }
                }
            }
        }
        return usageMap;
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), requireContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void collectUsageData(long startTime, long endTime) {
        appUsageMap.clear(); // Clear before collecting new data
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = usm.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        Map<String, Long> foregroundTimestamps = new HashMap<>();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String packageName = event.getPackageName();
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundTimestamps.put(packageName, event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (foregroundTimestamps.containsKey(packageName)) {
                    long start = foregroundTimestamps.remove(packageName);
                    long duration = event.getTimeStamp() - start;
                    if (!isSystemApp(packageName)) {
                        String appName = getAppNameFromPackage(packageName);
                        appUsageMap.put(appName, appUsageMap.getOrDefault(appName, 0L) + duration);
                    }
                }
            }
        }
    }

    private boolean isSystemApp(String packageName) {
        return packageName.startsWith("com.android");
    }

    private String getAppNameFromPackage(String packageName) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private void generateColorsForApps() {
        // Using Random for color generation as per user's original code preference
        Random rnd = new Random();
        appColorMap.clear(); // Clear existing colors
        for (String appName : appUsageMap.keySet()) {
            // Ensure colors are not too dark or too light for visibility
            int color = Color.rgb(rnd.nextInt(200) + 55, rnd.nextInt(200) + 55, rnd.nextInt(200) + 55);
            appColorMap.put(appName, color);
        }
    }


    // Helper to filter and sort app usage data for charts
    private List<Map.Entry<String, Long>> getFilteredAndSortedUsage(long minDurationMillis) {
        List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(appUsageMap.entrySet());

        // Filter by minimum duration
        sortedEntries.removeIf(entry -> entry.getValue() < minDurationMillis);

        // Sort by duration in descending order
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Limit to top 3 to 7 entries for charts, but not for progress bar list
        if (minDurationMillis > 0) { // Only apply limit if filtering by duration
            int size = sortedEntries.size();
            if (size > 7) {
                return sortedEntries.subList(0, 7);
            } else if (size >= 3) {
                return sortedEntries;
            }
        }
        // If less than 3 or not filtering by duration, return the unfiltered, sorted list
        return sortedEntries;
    }


    private void displayProgressBarList() {
        progressBarsContainer.removeAllViews(); // Use the new container
        if (appUsageMap == null || appUsageMap.isEmpty()) {
            progressBarsContainer.setVisibility(View.GONE);
            // noDataText visibility is handled in loadUsageForDay
            // Hide buttons when no data
            if (btnShowAll != null) btnShowAll.setVisibility(View.GONE);
            if (btnHide != null) btnHide.setVisibility(View.GONE);
            return;
        }

        if (noDataText != null) noDataText.setVisibility(View.GONE);
        progressBarsContainer.setVisibility(View.VISIBLE);

        long totalTime = 0;
        for (long duration : appUsageMap.values()) {
            if (duration > 0) totalTime += duration;
        }

        List<Map.Entry<String, Long>> sortedUsage = new ArrayList<>(appUsageMap.entrySet());
        sortedUsage.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        PackageManager pm = requireContext().getPackageManager();

        // Create views for all apps
        List<LinearLayout> allAppViews = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sortedUsage) {
            String appName = entry.getKey();
            long duration = entry.getValue();
            int percent = totalTime > 0 ? (int) (duration * 100 / totalTime) : 0;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8)); // Consistent padding
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView iconView = new ImageView(requireContext());
            iconView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)));
            iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try {
                String packageName = getPackageNameFromAppName(appName);
                if (packageName != null) {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    iconView.setImageDrawable(pm.getApplicationIcon(appInfo));
                } else {
                    iconView.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            } catch (Exception e) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            TextView nameView = new TextView(requireContext());
            nameView.setText(appName);
            nameView.setTextSize(14);
            nameView.setTextColor(Color.BLACK);
            nameView.setPadding(dpToPx(8), 0, dpToPx(4), 0); // Padding between icon and text

            ProgressBar progressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(0, dpToPx(20), 1f);
            progressBarParams.gravity = Gravity.CENTER_VERTICAL;
            progressBar.setLayoutParams(progressBarParams);
            progressBar.setMax(100);
            progressBar.setProgress(percent);
            progressBar.getProgressDrawable().setColorFilter(
                    appColorMap.getOrDefault(appName, Color.GRAY), android.graphics.PorterDuff.Mode.SRC_IN);

            TextView percentView = new TextView(requireContext());
            percentView.setText(percent + "%");
            percentView.setTextSize(14);
            percentView.setTextColor(Color.DKGRAY);
            percentView.setPadding(dpToPx(8), 0, 0, 0); // Padding after percent

            row.addView(iconView);
            row.addView(nameView);
            row.addView(progressBar);
            row.addView(percentView);
            allAppViews.add(row);
        }

        // Display only top 5 initially
        int initialCount = Math.min(5, allAppViews.size());
        for (int i = 0; i < initialCount; i++) {
            progressBarsContainer.addView(allAppViews.get(i));
        }

        // Set up button visibility and listeners
        if (allAppViews.size() > 5) {
            if (btnShowAll != null) {
                btnShowAll.setVisibility(View.VISIBLE);
                btnShowAll.setOnClickListener(v -> {
                    progressBarsContainer.removeAllViews();
                    for (LinearLayout view : allAppViews) {
                        progressBarsContainer.addView(view);
                    }
                    if (btnShowAll != null) btnShowAll.setVisibility(View.GONE);
                    if (btnHide != null) btnHide.setVisibility(View.VISIBLE);
                });
            }
            if (btnHide != null) {
                btnHide.setVisibility(View.GONE); // Initially hidden
                btnHide.setOnClickListener(v -> {
                    progressBarsContainer.removeAllViews();
                    int countAfterHide = Math.min(5, allAppViews.size());
                    for (int i = 0; i < countAfterHide; i++) {
                        progressBarsContainer.addView(allAppViews.get(i));
                    }
                    if (btnShowAll != null) btnShowAll.setVisibility(View.VISIBLE);
                    if (btnHide != null) btnHide.setVisibility(View.GONE);
                });
            }
        } else {
            // Hide buttons if 5 or fewer apps
            if (btnShowAll != null) btnShowAll.setVisibility(View.GONE);
            if (btnHide != null) btnHide.setVisibility(View.GONE);
        }
    }

    private String getPackageNameFromAppName(String appName) {
        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA); // Use GET_META_DATA flag
        for (ApplicationInfo appInfo : apps) {
            String label = pm.getApplicationLabel(appInfo).toString();
            if (label != null && label.equals(appName)) {
                return appInfo.packageName;
            }
        }
        return null;
    }

    private void displayPieChart() {
        // Use the helper to get filtered and sorted data
        List<Map.Entry<String, Long>> filteredAndSortedUsage = getFilteredAndSortedUsage(120 * 1000); // 2 minutes in millis

        if (filteredAndSortedUsage.isEmpty()) {
            pieChart.clear();
            pieChart.setVisibility(View.GONE);
            // noDataText visibility handled in loadUsageForDay
            return;
        }

        if (noDataText != null) noDataText.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        long totalTimeFiltered = 0;

        for (Map.Entry<String, Long> entry : filteredAndSortedUsage) {
            totalTimeFiltered += entry.getValue();
        }

        for (Map.Entry<String, Long> entry : filteredAndSortedUsage) {
            String appName = entry.getKey();
            long duration = entry.getValue();


            entries.add(new PieEntry(duration, appName));

            Integer color = appColorMap.get(appName);
            if (color == null) color = Color.GRAY; // Fallback color
            colors.add(color);
        }


        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(1f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.6f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLineColor(Color.BLACK);
        dataSet.setValueLineWidth(1.5f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setUsePercentValues(true);
        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);

        pieChart.setCenterText(formatDuration(totalTimeFiltered));
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.DKGRAY);

        pieChart.invalidate();
    }

    private void displayBarChart() {
        // Use the helper to get filtered and sorted data
        List<Map.Entry<String, Long>> filteredAndSortedUsage = getFilteredAndSortedUsage(120 * 1000); // 2 minutes in millis

        if (filteredAndSortedUsage.isEmpty()) {
            barChart.clear();
            barChart.setVisibility(View.GONE);
            // noDataText visibility handled in loadUsageForDay
            return;
        }

        if (noDataText != null) noDataText.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);

        List<BarEntry> entries = new ArrayList<>();
        final List<String> appNames = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Long> entry : filteredAndSortedUsage) {
            long durationInMinutes = entry.getValue() / 60000;
            entries.add(new BarEntry(index++, durationInMinutes));
            appNames.add(entry.getKey());
            Integer color = appColorMap.get(entry.getKey());
            if (color == null) color = Color.GRAY; // Fallback color
            colors.add(color);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Time Used (min)");
        dataSet.setColors(colors);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);
        barChart.setData(data);

        // Show all bars for the filtered/sorted set
        barChart.setVisibleXRangeMaximum(appNames.size());
        barChart.moveViewToX(0);
        barChart.setScaleEnabled(true);
        barChart.setPinchZoom(true);
        barChart.setDoubleTapToZoomEnabled(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Return empty string to remove labels below bars
                return "";
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        // Adjust label count based on the number of displayed apps
        xAxis.setLabelCount(appNames.size());

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextSize(12f);

        barChart.setFitBars(true);
        Description desc = new Description();
        desc.setText("Minutes");
        desc.setTextSize(14f);
        barChart.setDescription(desc);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.invalidate();
    }

    private void displayAppLabels(View view) {
        LinearLayout labelContainer = view.findViewById(R.id.appLabelContainer);
        labelContainer.removeAllViews();

        // Use the helper to get filtered and sorted data for labels as well
        List<Map.Entry<String, Long>> filteredAndSortedUsage = getFilteredAndSortedUsage(120 * 1000); // 2 minutes in millis

        if (filteredAndSortedUsage.isEmpty()) {
            labelContainer.setVisibility(View.GONE);
            return;
        }
        labelContainer.setVisibility(View.VISIBLE);

        PackageManager pm = requireContext().getPackageManager();

        for (Map.Entry<String, Long> entry : filteredAndSortedUsage) {
            String appName = entry.getKey();
            long millis = entry.getValue();

            LinearLayout appLabelLayout = new LinearLayout(requireContext());
            appLabelLayout.setOrientation(LinearLayout.HORIZONTAL);
            appLabelLayout.setGravity(Gravity.CENTER_VERTICAL);
            appLabelLayout.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4)); // Adjust padding slightly

            // Icon with color border
            LinearLayout iconLayout = new LinearLayout(requireContext());
            iconLayout.setOrientation(LinearLayout.VERTICAL);
            iconLayout.setGravity(Gravity.CENTER);
            // Adjust icon size to match progress bar list (40dp)
            LinearLayout.LayoutParams iconLayoutParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            iconLayout.setLayoutParams(iconLayoutParams);
            iconLayout.setBackgroundColor(appColorMap.getOrDefault(appName, Color.GRAY)); // Use app color as background
            iconLayout.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2)); // Keep padding for border effect

            ImageView iconView = new ImageView(requireContext());
            // Inner icon size (slightly smaller than layout for border effect)
            iconView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)));
            iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try {
                String packageName = getPackageNameFromAppName(appName);
                if (packageName != null) {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    iconView.setImageDrawable(pm.getApplicationIcon(appInfo));
                } else {
                    iconView.setImageResource(android.R.drawable.sym_def_app_icon); // Default icon
                }
            } catch (Exception e) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon); // Default icon on error
            }
            iconLayout.addView(iconView);


            // App name and Duration container
            LinearLayout textContainer = new LinearLayout(requireContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); // Take remaining space
            textContainer.setGravity(Gravity.CENTER_VERTICAL);
            textContainer.setPadding(dpToPx(8), 0, dpToPx(8), 0); // Padding between icon and text

            // App Name TextView
            TextView nameView = new TextView(requireContext());
            nameView.setText(appName);
            nameView.setTextSize(14); // Same text size as progress bar list
            nameView.setTextColor(Color.BLACK);
            nameView.setSingleLine(true);

            // Duration TextView
            TextView durationView = new TextView(requireContext());
            durationView.setText(formatDuration(millis));
            durationView.setTextSize(14); // Same text size as progress bar list
            durationView.setTextColor(Color.DKGRAY); // Use a slightly different color for duration
            durationView.setSingleLine(true);

            textContainer.addView(nameView);
            textContainer.addView(durationView);

            appLabelLayout.addView(iconLayout);
            appLabelLayout.addView(textContainer);

            labelContainer.addView(appLabelLayout);
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds);
    }

    private boolean hasDataBeenSavedToday() {
        // Implement this method to check if usage data has already been saved today
        return false;
    }

    private void setDataSavedToday() {
        // Implement this method to mark that today's data has been saved
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // --- Advisor Methods ---

    private void setupAdvisor() {
        // Load advisor state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isAdvisorEnabled = prefs.getBoolean(PREF_ADVISOR_ENABLED, true); // Default to true
        if (advisorToggle != null) {
            advisorToggle.setChecked(isAdvisorEnabled);
            setAdvisorVisibility(isAdvisorEnabled);

            // Set listener for toggle
            advisorToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PREF_ADVISOR_ENABLED, isChecked);
                editor.apply();
                setAdvisorVisibility(isChecked);
                // Update message immediately when toggled on
                if (isChecked) {
                    updateAdvisorMessage();
                } else {
                    // Clear bubble text when toggled off
                    if (advisorBubble != null) {
                        advisorBubble.setText("");
                        advisorBubble.setVisibility(View.GONE); // Hide bubble
                    }
                }
            });
        }


        // Initial message or check for data
        if (isAdvisorEnabled && advisorBubble != null) {
            // Message will be updated after data loads in loadUsageForDay
            advisorBubble.setText("Gathering data..."); // Initial state before data loads
            advisorBubble.setVisibility(View.VISIBLE);
        } else if (advisorBubble != null) {
            advisorBubble.setVisibility(View.GONE); // Hide bubble if advisor is off initially
        }
        // Ensure advisor layout is visible initially if it exists
        if (advisorLayout != null) {
            advisorLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setAdvisorVisibility(boolean isEnabled) {
        // Only control the visibility of the icon and bubble based on the toggle state
        if (characterIcon != null) {
            characterIcon.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        }
        // The advisorBubble visibility is also controlled by updateAdvisorMessage
        // We keep the advisorToggle and advisorLayout always visible to allow the user to re-enable the advisor
        if (advisorBubble != null) {
            // If disabling, hide the bubble immediately. If enabling, it will be shown by updateAdvisorMessage
            if (!isEnabled) {
                advisorBubble.setVisibility(View.GONE);
            } else {
                // When enabling, set to GONE initially, updateAdvisorMessage will make it VISIBLE
                advisorBubble.setVisibility(View.GONE);
            }
        }
    }

    private void updateAdvisorMessage() {
        if (advisorToggle != null && advisorToggle.isChecked()) {
            if (appUsageMap.isEmpty()) {
                if (advisorBubble != null) {
                    advisorBubble.setText("Looks like there's no significant app usage data for today yet. Maybe take a break!");
                    advisorBubble.setVisibility(View.VISIBLE);
                    if (characterIcon != null) characterIcon.setVisibility(View.VISIBLE); // Show icon even if no data
                }
                return; // No need to call Gemini if no data
            }

            // Show loading indicator
            if (advisorBubble != null) {
                advisorBubble.setText("Thinking..."); // Loading message while waiting for API
                advisorBubble.setVisibility(View.VISIBLE);
                if (characterIcon != null) characterIcon.setVisibility(View.VISIBLE);
            }

            // Call the NEW asynchronous method from GeminiApiHelper that uses the internal prompt
            if (getContext() != null) {
                GeminiApiHelper.getAppUsageAdviceAsyncWithInternalPrompt(getContext(), appUsageMap, new ApiResponseCallback<String>() { // <--- Changed to call the new method
                    @Override
                    public void onSuccess(String advice) {
                        // This code runs on the main thread
                        if (advisorBubble != null) {
                            if (advice != null && !advice.trim().isEmpty()) {
                                advisorBubble.setText(advice);
                                advisorBubble.setVisibility(View.VISIBLE);
                                if (characterIcon != null) characterIcon.setVisibility(View.VISIBLE);
                            } else {
                                // Fallback if advice is null or empty
                                // Log the issue to help debug why advice might be empty even with the new prompt
                                Log.w("ActivityFragment", "Gemini API returned empty or null advice with internal prompt.");
                                advisorBubble.setText("Could not generate specific advice.");
                                advisorBubble.setVisibility(View.VISIBLE);
                                if (characterIcon != null) characterIcon.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        // This code runs on the main thread
                        Log.e("ActivityFragment", "Gemini API call failed (Internal Prompt): " + error.getMessage(), error); // Log with method name context
                        if (advisorBubble != null) {
                            advisorBubble.setText("Error getting advice. Please try again later.");
                            advisorBubble.setVisibility(View.VISIBLE);
                            if (characterIcon != null) characterIcon.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }


        } else {
            // Advisor is turned off
            if (advisorBubble != null) {
                advisorBubble.setText(""); // Clear text when off
                advisorBubble.setVisibility(View.GONE); // Hide bubble if advisor is off
            }
            if (characterIcon != null) characterIcon.setVisibility(View.GONE); // Hide icon when advisor is off
        }
        // The advisorLayout itself and the toggle remain visible
        if (advisorLayout != null) {
            advisorLayout.setVisibility(View.VISIBLE);
        }
    }

    // Removed buildGeminiInputJson method as it's now handled within GeminiApiHelper.getAppUsageAdviceAsyncWithInternalPrompt
    // private String buildGeminiInputJson(Map<String, Long> usageData) { ... }
}