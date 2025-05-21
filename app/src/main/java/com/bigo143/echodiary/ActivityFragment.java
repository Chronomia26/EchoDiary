package com.bigo143.echodiary;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

    private int selectedDay = -1;
    private int firstDay = 1;
    private int lastDay = 31;
    private int currentMonth = -1;
    private int currentYear = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        monthText = view.findViewById(R.id.monthText);

        // Add a "No data" TextView programmatically if not in XML
        noDataText = view.findViewById(R.id.noDataText);
        if (noDataText == null) {
            noDataText = new TextView(requireContext());
            noDataText.setId(View.generateViewId());
            noDataText.setText("No data for this day");
            noDataText.setTextSize(16);
            noDataText.setTextColor(Color.DKGRAY);
            noDataText.setGravity(Gravity.CENTER);
            noDataText.setVisibility(View.GONE);
            LinearLayout chartContainer = view.findViewById(R.id.chartContainer);
            chartContainer.addView(noDataText, 0); // Add at the top
        }

        setupCalendarRow(view);

        // Default: show today
        Calendar calendar = Calendar.getInstance();
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);
        updateMonthText();
        loadUsageForDay(selectedDay, view);

        return view;
    }

    private void updateMonthText() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, currentMonth);
        cal.set(Calendar.YEAR, currentYear);
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
        monthText.setText(monthYear);
    }

    private void setupCalendarRow(View view) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int today = calendar.get(Calendar.DAY_OF_MONTH);

        // Get number of days in month
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        firstDay = 1;
        calendar.set(Calendar.MONTH, month + 1);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        lastDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Set first and last day TextViews
        TextView fixedDay = view.findViewById(R.id.fixedDay);
        TextView lastDayView = view.findViewById(R.id.lastDay);
        fixedDay.setText(String.valueOf(firstDay));
        lastDayView.setText(String.valueOf(lastDay));

        // Add days 2 to (lastDay-1) to dateContainer
        LinearLayout dateContainer = view.findViewById(R.id.dateContainer);
        dateContainer.removeAllViews();
        for (int day = 2; day < lastDay; day++) {
            TextView dayView = new TextView(requireContext());
            dayView.setText(String.valueOf(day));
            dayView.setTextSize(16);
            dayView.setGravity(Gravity.CENTER);
            dayView.setBackgroundResource(R.drawable.day_selector);
            dayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dayTextColor));
            dayView.setEllipsize(TextUtils.TruncateAt.END);
            dayView.setMaxLines(1);
            dayView.setWidth(dpToPx(48));
            dayView.setHeight(dpToPx(48));
            dayView.setPadding(0, 0, 0, 0);
            if (day == today) {
                dayView.setSelected(true);
            }
            int finalDay = day;
            dayView.setOnClickListener(v -> {
                clearDaySelection(view);
                dayView.setSelected(true);
                selectedDay = finalDay;
                loadUsageForDay(finalDay, view);
            });
            dateContainer.addView(dayView);
        }

        // Set click listeners for first and last day
        fixedDay.setOnClickListener(v -> {
            clearDaySelection(view);
            fixedDay.setSelected(true);
            selectedDay = firstDay;
            loadUsageForDay(firstDay, view);
        });
        lastDayView.setOnClickListener(v -> {
            clearDaySelection(view);
            lastDayView.setSelected(true);
            selectedDay = lastDay;
            loadUsageForDay(lastDay, view);
        });
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void clearDaySelection(View view) {
        TextView fixedDay = view.findViewById(R.id.fixedDay);
        TextView lastDayView = view.findViewById(R.id.lastDay);
        fixedDay.setSelected(false);
        lastDayView.setSelected(false);

        LinearLayout dateContainer = view.findViewById(R.id.dateContainer);
        for (int i = 0; i < dateContainer.getChildCount(); i++) {
            dateContainer.getChildAt(i).setSelected(false);
        }
    }

    private void loadUsageForDay(int day, View view) {
        appUsageMap.clear();
        appColorMap.clear();

        Calendar cal = Calendar.getInstance();
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
            displayPieChart();
            displayBarChart();
            displayAppLabels(view);

            // Save data for today only
            if (day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && !hasDataBeenSavedToday()) {
                UsageDataSaver.saveUsageToXml(requireContext(), appUsageMap);
                setDataSavedToday();
            }
        }
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
}