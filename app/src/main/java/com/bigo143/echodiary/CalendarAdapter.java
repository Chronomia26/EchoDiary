package com.bigo143.echodiary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
    private int firstDayOffset;

    public CalendarAdapter(Context context, List<String> days, Map<Integer, Integer> moodMap, int displayedMonth, int displayedYear, int firstDayOffset) {
        this.context = context;
        this.days = days;
        this.moodMap = moodMap;
        this.displayedMonth = displayedMonth;
        this.displayedYear = displayedYear;
        this.firstDayOffset = firstDayOffset;
    }

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

    @Override
    public int getCount() {
        return days.size();
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
        ImageView moodIcon = view.findViewById(R.id.moodIcon);

        String dayStr = days.get(position);
        dayText.setText(dayStr);

        // Reset background and mood icon
        view.setBackgroundResource(android.R.color.transparent);
        moodIcon.setVisibility(View.INVISIBLE);

        if (!dayStr.isEmpty()) {
            int day = Integer.parseInt(dayStr);

            // Always apply current day background if it is the current day
            if (day == currentDay) {
                view.setBackgroundResource(R.drawable.border_today); // static highlight
                // If current day is selected, don't show selected background
            } else if (day == selectedDay) {
                view.setBackgroundResource(R.drawable.border_selected);
            } else {
                view.setBackgroundResource(android.R.color.transparent);
            }

            // Show mood icon if present
            if (moodMap != null && moodMap.containsKey(day)) {
                int moodDrawableRes = moodMap.get(day);
                moodIcon.setImageResource(moodDrawableRes);
                moodIcon.setVisibility(View.VISIBLE);
            } else {
                moodIcon.setVisibility(View.INVISIBLE);
            }
        } else {
            // For empty day cells
            view.setBackgroundResource(android.R.color.transparent);
            moodIcon.setVisibility(View.INVISIBLE);
        }



        return view;
    }
}
