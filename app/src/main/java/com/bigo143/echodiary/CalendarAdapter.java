package com.bigo143.echodiary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CalendarAdapter extends BaseAdapter {
    private Context context;
    private List<String> days;
    private Map<Integer, Integer> moodMap;
    private int displayedMonth;
    private int displayedYear;
    private int selectedDay = -1;
    private int currentDay = -1;
    private int firstDayOffset;  // number of blank cells before day 1




    public void setCurrentDay(int day) {
        currentDay = day;
        notifyDataSetChanged();
    }

    public void setSelectedDay(int day) {
        selectedDay = day;
        notifyDataSetChanged();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public int getSelectedDay() {
        return selectedDay;
    }


    public CalendarAdapter(Context context, List<String> days, Map<Integer, Integer> moodMap, int displayedMonth, int displayedYear, int firstDayOffset) {
        this.context = context;
        this.days = days;
        this.moodMap = moodMap;
        this.displayedMonth = displayedMonth;
        this.displayedYear = displayedYear;
        this.firstDayOffset = firstDayOffset;
    }



    @Override
    public int getCount() {
        return days.size(); // Total number of days in the calendar grid
    }

    @Override
    public String getItem(int position) {
        return (position >= 0 && position < days.size()) ? days.get(position) : null;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.calendar_day_item, parent, false);
        }

        TextView dayText = view.findViewById(R.id.dayText);
        ImageView moodIcon = view.findViewById(R.id.moodIcon); // Make sure your calendar_day_item.xml has this ImageView!

        String dayStr = days.get(position);
        dayText.setText(dayStr);

        // Clear background and mood icon by default
        view.setBackgroundResource(android.R.color.transparent);
        moodIcon.setVisibility(View.INVISIBLE);

        if (!dayStr.isEmpty()) {
            int day = Integer.parseInt(dayStr);

            // Set background for selected or current day
            if (day == selectedDay) {
                view.setBackgroundResource(R.drawable.border_selected);
            } else if (day == currentDay) {
                view.setBackgroundResource(R.drawable.border_today);
            }

            // Show mood icon if exists for this day
            if (moodMap != null && moodMap.containsKey(day)) {
                int moodDrawableRes = moodMap.get(day);
                moodIcon.setImageResource(moodDrawableRes);
                moodIcon.setVisibility(View.VISIBLE);
            }
        }

        return view;
    }


}