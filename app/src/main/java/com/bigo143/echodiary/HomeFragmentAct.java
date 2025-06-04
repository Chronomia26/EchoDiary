package com.bigo143.echodiary;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragmentAct extends Fragment { // Corrected class name

    private LinearLayout homeTaskList;
    private TextView dateText;
    private ImageView characterIcon;
    private TextView talkWithEchoText;
    private TextView daytraceTitle;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dateText = view.findViewById(R.id.date_text);
        characterIcon = view.findViewById(R.id.character_icon);
        talkWithEchoText = view.findViewById(R.id.talk_with_echo);
        daytraceTitle = view.findViewById(R.id.daytrace_title);
        homeTaskList = view.findViewById(R.id.home_task_list);

        // Set the current date
        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        dateText.setText(dateFormat.format(now.getTime()));

        if (hasUsageStatsPermission()) {
            loadAndDisplayActivityLog();
        } else {
            // Optionally show a message or button to request permission
            // For now, clear the list or show no data message
            homeTaskList.removeAllViews();
            TextView permissionNeededText = new TextView(requireContext());
            permissionNeededText.setText("Usage Stats permission needed to display activity log.");
            homeTaskList.addView(permissionNeededText);
            // You might want to add a button here to open settings
        }

        return view;
    }

    private void loadAndDisplayActivityLog() {
        homeTaskList.removeAllViews(); // Clear existing placeholder/data views

        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);

        Calendar endOfToday = Calendar.getInstance();

        List<AppActivityEvent> activityEvents = getAppActivityEvents(startOfToday.getTimeInMillis(), endOfToday.getTimeInMillis());

        // Sort events by timestamp (most recent first)
        Collections.sort(activityEvents, Comparator.comparingLong(AppActivityEvent::getTimestamp).reversed());

        // Display top 5 events
        int count = 0;
        for (AppActivityEvent event : activityEvents) {
            if (count >= 5) break;
            displayActivityItem(event);
            count++;
        }

        if (activityEvents.isEmpty()) {
            TextView noDataText = new TextView(requireContext());
            noDataText.setText("No recent activity recorded.");
            homeTaskList.addView(noDataText);
        }
    }

    private List<AppActivityEvent> getAppActivityEvents(long startTime, long endTime) {
        List<AppActivityEvent> eventsList = new ArrayList<>();
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = usm.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String packageName = event.getPackageName();

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND || event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (!isSystemApp(packageName)) { // Filter out system apps
                    String appName = getAppNameFromPackage(packageName);
                    String eventType = (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) ? "Opened" : "Closed";
                    long timestamp = event.getTimeStamp();
                    eventsList.add(new AppActivityEvent(appName, packageName, eventType, timestamp));
                }
            }
        }
        return eventsList;
    }

    private void displayActivityItem(AppActivityEvent event) {
        LinearLayout itemLayout = new LinearLayout(requireContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setBackgroundResource(R.drawable.item_background);
        itemLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) itemLayout.getLayoutParams()).topMargin = dpToPx(8);

        ImageView iconView = new ImageView(requireContext());
        iconView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)));
        iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            ApplicationInfo appInfo = requireContext().getPackageManager().getApplicationInfo(event.getPackageName(), 0);
            iconView.setImageDrawable(requireContext().getPackageManager().getApplicationIcon(appInfo));
        } catch (PackageManager.NameNotFoundException e) {
            iconView.setImageResource(android.R.drawable.sym_def_app_icon); // Default icon
        }

        TextView labelView = new TextView(requireContext());
        labelView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)); // Weight 1f to take remaining space
        labelView.setText(timeFormat.format(new Date(event.getTimestamp())) + " | " + event.getEventType() + " " + event.getAppName());
        labelView.setTextSize(16);
        labelView.setPadding(dpToPx(12), 0, 0, 0);

        itemLayout.addView(iconView);
        itemLayout.addView(labelView);

        homeTaskList.addView(itemLayout);
    }

    // Helper method adapted from ActivityFragment
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), requireContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // Helper method adapted from ActivityFragment
    private String getAppNameFromPackage(String packageName) {
        try {
            return requireContext().getPackageManager().getApplicationLabel(
                    requireContext().getPackageManager().getApplicationInfo(packageName, 0)
            ).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName; // Return package name if app name not found
        }
    }

    // Helper method adapted from ActivityFragment (might not be needed directly, but useful context)
    private boolean isSystemApp(String packageName) {
        // Basic check, you might need a more robust one
        PackageManager pm = requireContext().getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // App not found, could be system app or removed
            return true; // Err on the side of caution
        }
        return false;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    // Simple data class to hold activity event info
    private static class AppActivityEvent {
        private String appName;
        private String packageName;
        private String eventType; // "Opened" or "Closed"
        private long timestamp;

        public AppActivityEvent(String appName, String packageName, String eventType, long timestamp) {
            this.appName = appName;
            this.packageName = packageName;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }

        public String getAppName() {
            return appName;
        }

        public String getPackageName() { return packageName; }

        public String getEventType() {
            return eventType;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}