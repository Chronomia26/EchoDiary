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
        btnBar = view.findViewById(R.id.btnBar);
        leftDay = view.findViewById(R.id.leftDay);
        rightDay = view.findViewById(R.id.rightDay);
        dateContainer = view.findViewById(R.id.dateContainer);
        calendarScrollView = view.findViewById(R.id.calendarScrollView);

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

        buildDaysWithData();
        selectedDayIndex = getTodayOrLatestIndex();
        updateMonthText();
        setupCalendarRow();
        loadUsageForSelectedDay(view);
        showChart(currentChartType);
        return view;
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
        return daysWithData.size() - 1;
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void setupCalendarRow() {
        if (daysWithData.isEmpty()) return;
        int leftIdx = 0;
        int rightIdx = getTodayOrLatestIndex();
        Calendar leftCal = daysWithData.get(leftIdx);
        Calendar rightCal = daysWithData.get(rightIdx);

        leftDay.setText(String.valueOf(leftCal.get(Calendar.DAY_OF_MONTH)));
        leftDay.setSelected(selectedDayIndex == leftIdx);
        leftDay.setOnClickListener(v -> {
            selectedDayIndex = leftIdx;
            updateMonthText();
            setupCalendarRow();
            loadUsageForSelectedDay(getView());
            showChart(currentChartType);
        });

        rightDay.setText(String.valueOf(rightCal.get(Calendar.DAY_OF_MONTH)));
        rightDay.setSelected(selectedDayIndex == rightIdx);
        rightDay.setOnClickListener(v -> {
            selectedDayIndex = rightIdx;
            updateMonthText();
            setupCalendarRow();
            loadUsageForSelectedDay(getView());
            showChart(currentChartType);
        });

        dateContainer.removeAllViews();
        for (int i = leftIdx + 1; i < rightIdx; i++) {
            Calendar cal = daysWithData.get(i);
            TextView dayView = new TextView(requireContext());
            dayView.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            dayView.setTextSize(16);
            dayView.setGravity(Gravity.CENTER);
            dayView.setBackgroundResource(R.drawable.day_selector);
            dayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dayTextColor));
            dayView.setWidth(dpToPx(48));
            dayView.setHeight(dpToPx(48));
            dayView.setSelected(selectedDayIndex == i);
            int finalI = i;
            dayView.setOnClickListener(v -> {
                selectedDayIndex = finalI;
                updateMonthText();
                setupCalendarRow();
                loadUsageForSelectedDay(getView());
                showChart(currentChartType);
            });
            dateContainer.addView(dayView);
        }
    }

    private void updateMonthText() {
        if (selectedDayIndex < 0 || selectedDayIndex >= daysWithData.size()) return;
        Calendar cal = daysWithData.get(selectedDayIndex);
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
        monthText.setText(monthYear);
    }

    private void loadUsageForSelectedDay(View view) {
        if (selectedDayIndex < 0 || selectedDayIndex >= daysWithData.size()) return;
        Calendar cal = daysWithData.get(selectedDayIndex);
        loadUsageForDay(
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.YEAR),
                view
        );
        showChart(currentChartType);
    }

    // Only one chart visible at a time, progress is default
    private void showChart(String type) {
        progressBarList.setVisibility(type.equals("progress") ? View.VISIBLE : View.GONE);
        pieChart.setVisibility(type.equals("pie") && !appUsageMap.isEmpty() ? View.VISIBLE : View.GONE);
        barChart.setVisibility(type.equals("bar") && !appUsageMap.isEmpty() ? View.VISIBLE : View.GONE);
        if (noDataText != null) noDataText.setVisibility(appUsageMap.isEmpty() ? View.VISIBLE : View.GONE);
        View appLabelContainer = getView() != null ? getView().findViewById(R.id.appLabelContainer) : null;
        if (appLabelContainer != null) {
            if (type.equals("progress")) {
                appLabelContainer.setVisibility(View.GONE);
            } else {
                appLabelContainer.setVisibility(View.VISIBLE);
                displayAppLabels(getView());
            }
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
            collectUsageData(startTime, endTime);
            generateColorsForApps();
            displayProgressBarList();
            displayPieChart();
            displayBarChart();
            // displayAppLabels(view); // removed, now handled in showChart
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
            return requireContext().getPackageManager().getApplicationLabel(
                    requireContext().getPackageManager().getApplicationInfo(packageName, 0)
            ).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private void generateColorsForApps() {
        Random rnd = new Random();
        for (String appName : appUsageMap.keySet()) {
            appColorMap.put(appName, Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
        }
    }

    private void displayProgressBarList() {
        progressBarList.removeAllViews();
        if (appUsageMap == null || appUsageMap.isEmpty()) {
            progressBarList.setVisibility(View.GONE);
            if (noDataText != null) noDataText.setVisibility(View.VISIBLE);
            return;
        }
        if (noDataText != null) noDataText.setVisibility(View.GONE);
        progressBarList.setVisibility(View.VISIBLE);
        long totalTime = 0;
        for (long duration : appUsageMap.values()) {
            if (duration > 0) totalTime += duration;
        }
        PackageManager pm = requireContext().getPackageManager();
        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            String appName = entry.getKey();
            long duration = entry.getValue();
            int percent = totalTime > 0 ? (int) (duration * 100 / totalTime) : 0;
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);
            row.setGravity(Gravity.CENTER_VERTICAL);
            ImageView iconView = new ImageView(requireContext());
            iconView.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
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
            nameView.setPadding(16, 0, 8, 0);
            ProgressBar progressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setLayoutParams(new LinearLayout.LayoutParams(0, 40, 1f));
            progressBar.setMax(100);
            progressBar.setProgress(percent);
            TextView percentView = new TextView(requireContext());
            percentView.setText(percent + "%");
            percentView.setTextSize(14);
            percentView.setTextColor(Color.DKGRAY);
            percentView.setPadding(16, 0, 0, 0);
            row.addView(iconView);
            row.addView(nameView);
            row.addView(progressBar);
            row.addView(percentView);
            progressBarList.addView(row);
        }
    }

    private String getPackageNameFromAppName(String appName) {
        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (ApplicationInfo appInfo : apps) {
            String label = pm.getApplicationLabel(appInfo).toString();
            if (label.equals(appName)) {
                return appInfo.packageName;
            }
        }
        return null;
    }

    private void displayPieChart() {
        if (appUsageMap == null || appUsageMap.isEmpty()) {
            pieChart.clear();
            pieChart.setVisibility(View.GONE);
            if (noDataText != null) noDataText.setVisibility(View.VISIBLE);
            return;
        }
        if (noDataText != null) noDataText.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        long totalTime = 0;

        for (long duration : appUsageMap.values()) {
            if (duration > 0) {
                totalTime += duration;
            }
        }

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            String appName = entry.getKey();
            long duration = entry.getValue();

            if (duration > 0) {
                float percentage = (float) duration / totalTime * 100f;
                if (percentage >= 0.5f) {
                    entries.add(new PieEntry(duration, appName));

                    Integer color = appColorMap.get(appName);
                    if (color == null) color = Color.GRAY;
                    colors.add(color);
                }
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setVisibility(View.GONE);
            if (noDataText != null) noDataText.setVisibility(View.VISIBLE);
            return;
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

        pieChart.setCenterText(formatDuration(totalTime));
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.DKGRAY);

        pieChart.invalidate();
    }

    private void displayBarChart() {
        if (appUsageMap == null || appUsageMap.isEmpty()) {
            barChart.clear();
            barChart.setVisibility(View.GONE);
            if (noDataText != null) noDataText.setVisibility(View.VISIBLE);
            return;
        }
        if (noDataText != null) noDataText.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);

        List<BarEntry> entries = new ArrayList<>();
        final List<String> appNames = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            long durationInMinutes = entry.getValue() / 60000;
            entries.add(new BarEntry(index++, durationInMinutes));
            appNames.add(entry.getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Time Used (min)");
        dataSet.setColors(new ArrayList<>(appColorMap.values()));
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);
        barChart.setData(data);

        // Show all bars by default, let user zoom in if desired
        barChart.setVisibleXRangeMaximum(appNames.size());
        barChart.moveViewToX(0);
        barChart.setScaleEnabled(true);
        barChart.setPinchZoom(true);
        barChart.setDoubleTapToZoomEnabled(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                if (i >= 0 && i < appNames.size()) {
                    String name = appNames.get(i);
                    // Ellipsize long names for readability
                    if (name.length() > 10) {
                        return name.substring(0, 9) + "…";
                    }
                    return name;
                }
                return "";
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setLabelCount(Math.min(appNames.size(), 10));

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextSize(12f);

        barChart.setFitBars(true);
        Description desc = new Description();
        desc.setText("App Usage (min)");
        desc.setTextSize(14f);
        barChart.setDescription(desc);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.invalidate();
    }

    private void displayAppLabels(View view) {
        LinearLayout labelContainer = view.findViewById(R.id.appLabelContainer);
        labelContainer.removeAllViews();

        for (String appName : appUsageMap.keySet()) {
            long millis = appUsageMap.get(appName);

            LinearLayout appLabelLayout = new LinearLayout(requireContext());
            appLabelLayout.setOrientation(LinearLayout.HORIZONTAL);
            appLabelLayout.setPadding(8, 4, 8, 4);

            View colorBox = new View(requireContext());
            colorBox.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
            colorBox.setBackgroundColor(appColorMap.getOrDefault(appName, Color.BLACK));

            TextView label = new TextView(requireContext());
            label.setText(appName + " - " + formatDuration(millis));
            label.setTextSize(14);
            label.setTextColor(Color.BLACK);
            label.setPadding(8, 0, 8, 0);

            appLabelLayout.addView(colorBox);
            appLabelLayout.addView(label);

            labelContainer.addView(appLabelLayout);
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        return String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes % 60, seconds);
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
}