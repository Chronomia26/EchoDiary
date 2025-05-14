package com.bigo143.echodiary;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
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
import java.util.concurrent.TimeUnit;

public class ActivityFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart;
    private final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    private final Map<String, Long> appUsageMap = new LinkedHashMap<>();
    private final Map<String, Integer> appColorMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);

        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            collectUsageData();
            generateColorsForApps();
            displayPieChart();
            displayBarChart();
            displayAppLabels(view);

            if (!hasDataBeenSavedToday()) {
                UsageDataSaver.saveUsageToXml(requireContext(), appUsageMap); // Save to XML
                setDataSavedToday();
            }
        }

        return view;
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), requireContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void collectUsageData() {
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startTime = cal.getTimeInMillis();

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

    private void displayPieChart() {
        if (appUsageMap == null || appUsageMap.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        long totalTime = 0;

        // First pass: calculate total time
        for (long duration : appUsageMap.values()) {
            if (duration > 0) {
                totalTime += duration;
            }
        }

        // Second pass: add only significant entries (e.g., > 0.5% of total)
        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            String appName = entry.getKey();
            long duration = entry.getValue();

            if (duration > 0) {
                float percentage = (float) duration / totalTime * 100f;
                if (percentage >= 0.5f) {
                    entries.add(new PieEntry(duration, ""));

                    Integer color = appColorMap.get(appName);
                    if (color == null) color = Color.GRAY;
                    colors.add(color);
                }
            }
        }

        if (entries.isEmpty()) {
            pieChart.setVisibility(View.GONE);
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

        // Updated center text formatting using formatDuration()
        pieChart.setCenterText(formatDuration(totalTime));
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.DKGRAY);

        pieChart.invalidate();
        pieChart.setVisibility(View.VISIBLE);
    }

    private void displayBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        final List<String> appNames = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            long durationInMinutes = entry.getValue() / 60000; // Convert to minutes
            entries.add(new BarEntry(index++, durationInMinutes));
            appNames.add(entry.getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Time Used (min)");
        dataSet.setColors(new ArrayList<>(appColorMap.values()));  // Set bar chart colors to match pie chart
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);
        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return i >= 0 && i < appNames.size() ? appNames.get(i) : "";
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.setFitBars(true);
        barChart.setDescription(new Description());
        barChart.getDescription().setText("App Usage Today");
        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
    }

    private void displayAppLabels(View view) {
        LinearLayout labelContainer = view.findViewById(R.id.appLabelContainer);
        labelContainer.removeAllViews();

        for (String appName : appUsageMap.keySet()) {
            long millis = appUsageMap.get(appName);

            // Create layout for each app label
            LinearLayout appLabelLayout = new LinearLayout(requireContext());
            appLabelLayout.setOrientation(LinearLayout.HORIZONTAL);
            appLabelLayout.setPadding(8, 4, 8, 4);

            // Color Box
            View colorBox = new View(requireContext());
            colorBox.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
            colorBox.setBackgroundColor(appColorMap.getOrDefault(appName, Color.BLACK));

            // App Label Text
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
        seconds %= 60;  // Get the remaining seconds
        return String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes % 60, seconds);
    }

    private boolean hasDataBeenSavedToday() {
        // You should implement this method to check if usage data has already been saved today
        return false;
    }

    private void setDataSavedToday() {
        // You should implement this method to mark that today's data has been saved
    }
}