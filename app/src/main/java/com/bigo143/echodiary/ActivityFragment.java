package com.bigo143.echodiary;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityFragment extends Fragment {

    private TextView textView;

    public ActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);
        textView = view.findViewById(R.id.textViewUsage);

        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            displayUsageByTimeSlot();
        }

        return view;
    }

    private boolean hasUsageStatsPermission() {
        //here is the code to check if the app has the usage stats permission
        AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), requireContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void displayUsageByTimeSlot() {
        //here is the code to display the usage by time slot
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startTime = cal.getTimeInMillis();

        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        Map<String, List<String>> timeSlotMap = new LinkedHashMap<>();
        timeSlotMap.put("🌅 Morning (6AM–12PM)", new ArrayList<>());
        timeSlotMap.put("🌤 Afternoon (12PM–6PM)", new ArrayList<>());
        timeSlotMap.put("🌙 Evening (6PM–12AM)", new ArrayList<>());
        timeSlotMap.put("🌃 Night (12AM–6AM)", new ArrayList<>());

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        for (UsageStats stat : usageStatsList) {
            if (stat.getTotalTimeInForeground() > 0) {
                Calendar lastTimeUsed = Calendar.getInstance();
                lastTimeUsed.setTimeInMillis(stat.getLastTimeUsed());
                int hour = lastTimeUsed.get(Calendar.HOUR_OF_DAY);

                String appName = getAppNameFromPackage(stat.getPackageName());//here is the code to get the app name from the package name
                String entry = appName + " - Last Used: " + sdf.format(lastTimeUsed.getTime());

                if (hour >= 6 && hour < 12)
                    timeSlotMap.get("🌅 Morning (6AM–12PM)").add(entry);
                else if (hour >= 12 && hour < 18)
                    timeSlotMap.get("🌤 Afternoon (12PM–6PM)").add(entry);
                else if (hour >= 18 && hour < 24)
                    timeSlotMap.get("🌙 Evening (6PM–12AM)").add(entry);
                else
                    timeSlotMap.get("🌃 Night (12AM–6AM)").add(entry);
            }
        }

        StringBuilder finalOutput = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : timeSlotMap.entrySet()) {
            finalOutput.append(entry.getKey()).append(":\n");
            if (entry.getValue().isEmpty()) {
                finalOutput.append("  No activity recorded.\n\n");
            } else {
                for (String app : entry.getValue()) {
                    finalOutput.append("  • ").append(app).append("\n");
                }
                finalOutput.append("\n");
            }
        }

        textView.setText(finalOutput.toString());

        }

    private String getAppNameFromPackage(String packageName) {

        //here is the code to get the app name from the package name
        //sira pa to, ayusin kasi imbis na appname ung lumabas, ang lumalabas ung package name
        //like com.example.roblox

        try {
            PackageManager pm = requireContext().getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            CharSequence label = pm.getApplicationLabel(ai);
            return (label != null) ? label.toString() : packageName;
        } catch (PackageManager.NameNotFoundException e) {
            return packageName; // fallback to package name if app not found
        }
    }

}

