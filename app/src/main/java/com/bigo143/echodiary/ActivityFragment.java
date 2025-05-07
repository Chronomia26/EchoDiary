package com.bigo143.echodiary;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
                UsageDataSaver.saveUsageToXml(requireContext(), convertAppUsageToPackageUsage());
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
        try {
            PackageManager pm = requireContext().getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void generateColorsForApps() {
        Random rnd = new Random();
        for (String appName : appUsageMap.keySet()) {
            appColorMap.put(appName, Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
        }
    }

    private void displayPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        long totalTime = 0;

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            String appName = entry.getKey();
            long duration = entry.getValue();
            totalTime += duration;
            entries.add(new PieEntry(duration, appName));
            colors.add(appColorMap.get(appName));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.6f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLineColor(Color.BLACK);
        dataSet.setValueLineWidth(1.5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setUsePercentValues(true);
        pieChart.setData(data);
        pieChart.setEntryLabelColor(Color.BLACK);  // Set default label color
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setCenterText("Total Time\n" + formatDuration(totalTime));
        pieChart.setCenterTextSize(14f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDescription(null);
        pieChart.getLegend().setEnabled(false);
        pieChart.invalidate();
    }

    private void displayBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        final List<String> appNames = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            entries.add(new BarEntry(index++, entry.getValue()));
            appNames.add(entry.getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Time Used (ms)");
        dataSet.setColor(Color.parseColor("#3F51B5"));
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
            TextView label = new TextView(requireContext());
            long millis = appUsageMap.get(appName);
            label.setText(appName + " - " + formatDuration(millis));
            label.setTextSize(14);
            label.setTextColor(appColorMap.getOrDefault(appName, Color.BLACK));
            label.setPadding(10, 5, 10, 5);
            labelContainer.addView(label);
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format(Locale.getDefault(), "%d h %02d m", hours, minutes);
    }

    private String getAppNameFromPackage(String packageName) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            CharSequence label = pm.getApplicationLabel(appInfo);

            // If label is null or same as package name, try brute-force fallback
            if (label == null || label.toString().equalsIgnoreCase(packageName)) {
                return tryBruteForceAppName(packageName);
            }
            return label.toString();
        } catch (PackageManager.NameNotFoundException e) {
            return tryBruteForceAppName(packageName);
        }
    }

    private String tryBruteForceAppName(String packageName) {
        ActivityManager am = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();

        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals(packageName)) {
                    try {
                        PackageManager pm = requireContext().getPackageManager();
                        ApplicationInfo ai = pm.getApplicationInfo(process.processName, 0);
                        CharSequence label = pm.getApplicationLabel(ai);
                        if (label != null) {
                            return label.toString();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // ignore and fallback below
                    }
                }
            }
        }

        // Final fallback: strip "com." and return last segment
        if (packageName.contains(".")) {
            String[] parts = packageName.split("\\.");
            return capitalize(parts[parts.length - 1]);
        }

        return packageName;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private boolean hasDataBeenSavedToday() {
        // TODO: Implement persistence check
        return false;
    }

    private void setDataSavedToday() {
        // TODO: Implement persistence save
    }

    private Map<String, Long> convertAppUsageToPackageUsage() {
        return new HashMap<>();
    }
}
// TODO: app name tlaga d ko magawan ng paraan try ko ayusin bukas pati yung bar charts