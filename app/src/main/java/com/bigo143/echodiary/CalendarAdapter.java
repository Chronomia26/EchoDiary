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

import java.util.List;
import java.util.Map;

public class CalendarAdapter extends BaseAdapter {
    private Context context;
    private List<String> days;
    private Map<Integer, Integer> moodMap;
    private int displayedMonth;
    private int displayedYear;


    public CalendarAdapter(Context context, List<String> days, Map<Integer, Integer> moodMap, int displayedMonth, int displayedYear) {
        this.context = context;
        this.days = days;
        this.moodMap = moodMap;
        this.displayedMonth = displayedMonth;
        this.displayedYear = displayedYear;
    }


    @Override
    public int getCount() {
        return days.size(); // Total number of days in the calendar grid
    }

    @Override
    public Object getItem(int position) {
        return days.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.calendar_day_item, parent, false);

        TextView dayText = view.findViewById(R.id.dayText);
        ImageView moodDot = view.findViewById(R.id.moodDot);

        String day = days.get(position);

        if (!day.isEmpty()) {
            dayText.setText(day);

            try {
                int dayInt = Integer.parseInt(day);

                // Mood logic
                if (moodMap.containsKey(dayInt)) {
                    moodDot.setImageResource(moodMap.get(dayInt));
                    moodDot.setVisibility(View.VISIBLE);
                } else {
                    moodDot.setVisibility(View.GONE);
                }

                // Highlight today's date
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                int today = calendar.get(java.util.Calendar.DAY_OF_MONTH);
                int currentMonth = calendar.get(java.util.Calendar.MONTH);
                int currentYear = calendar.get(java.util.Calendar.YEAR);

                // TODO: If your calendar allows viewing other months, compare them too
                if (dayInt == today &&
                        currentMonth == displayedMonth &&
                        currentYear == displayedYear) {
                    view.setBackgroundResource(R.drawable.border_today);
                } else {
                    view.setBackgroundColor(android.graphics.Color.WHITE);
                }



            } catch (NumberFormatException ignored) {
            }
        } else {
            dayText.setText("");
            moodDot.setVisibility(View.GONE);
            view.setBackground(null);
        }

        return view;
    }

}



