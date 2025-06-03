package com.bigo143.echodiary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalendarAdapter extends BaseAdapter {
    private final Context context;
    private final List<CalendarDay> days;
    private final Map<Integer, Integer> moodMap;
    private final int displayedMonth;
    private final int displayedYear;
    private final int firstDayOffset;

    private int selectedDay = -1;
    private int currentDay = -1;
    private final Set<Integer> sundayPositions = new HashSet<>();
    private final Set<Integer> taskDays = new HashSet<>();
    private boolean isExpanded = false;

    private int totalHeight = 0;
    private final int numRows = 6;
    private int bottomCellMarginPx = 6;


    public CalendarAdapter(Context context, List<CalendarDay> days, Map<Integer, Integer> moodMap, int displayedMonth, int displayedYear, int firstDayOffset) {
        this.context = context;
        this.days = days;
        this.moodMap = moodMap;
        this.displayedMonth = displayedMonth;
        this.displayedYear = displayedYear;
        this.firstDayOffset = firstDayOffset;
    }

    public void setSundayPositions(Set<Integer> sundays) {
        sundayPositions.clear();
        sundayPositions.addAll(sundays);
        notifyDataSetChanged();
    }

    public void setCurrentDay(int day) {
        currentDay = day;
        notifyDataSetChanged();
    }

    public void setSelectedDay(int day) {
        selectedDay = day;
        notifyDataSetChanged();
    }

    public void setTaskDays(Set<Integer> taskDays) {
        this.taskDays.clear();
        this.taskDays.addAll(taskDays);
        notifyDataSetChanged();
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
        notifyDataSetChanged();
    }

    public void setBottomCellMargin(int marginPx) {
        this.bottomCellMarginPx = marginPx;
        notifyDataSetChanged();
    }

    public void setTotalHeight(int heightPx) {
        this.totalHeight = heightPx;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return days.size();
    }

    @Override
    public CalendarDay getItem(int position) {
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

        // Compute scaled dimensions
        float scale = context.getResources().getDisplayMetrics().density;
        int cellHeightPx = (totalHeight > 0) ? (totalHeight / numRows) : (int) (50 * scale + 0.5f);
        int paddingBottom = Math.max(cellHeightPx - (int) (25 * scale), (int) (4 * scale));

        // Safely update layout parameters
        ViewGroup.MarginLayoutParams layoutParams;
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            layoutParams.height = cellHeightPx;
            layoutParams.bottomMargin = bottomCellMarginPx;

        } else {
            layoutParams = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, cellHeightPx);
            layoutParams.bottomMargin = bottomCellMarginPx;
        }
        view.setLayoutParams(layoutParams);

        // Apply padding for the dayText
        dayText.setPadding(
                dayText.getPaddingLeft(), 0,
                dayText.getPaddingRight(), paddingBottom
        );


        CalendarDay cd = days.get(position);
        dayText.setText(cd.day == 0 ? "" : String.valueOf(cd.day));

        // Reset background and mood icon
        view.setBackgroundResource(android.R.color.transparent);
        moodIcon.setVisibility(View.INVISIBLE);

        // Apply dimmed style for non-current month
        if (!cd.isCurrentMonth) {
            dayText.setTextColor(Color.parseColor("#888888")); // Dim gray
        } else if (sundayPositions.contains(position)) {
            dayText.setTextColor(Color.RED);
        } else {
            dayText.setTextColor(Color.BLACK);
        }

        // Underline task days
        if (cd.isCurrentMonth && taskDays.contains(cd.day)) {
            dayText.setPaintFlags(dayText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            dayText.setPaintFlags(dayText.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }

        // Background highlights
        if (cd.isCurrentMonth && cd.day == currentDay) {
            view.setBackgroundResource(R.drawable.border_today);
        } else if (cd.isCurrentMonth && cd.day == selectedDay) {
            view.setBackgroundResource(R.drawable.border_selected);
        } else {
            view.setBackgroundResource(android.R.color.transparent);

        }

        // Show mood icon if present
        if (moodMap != null && moodMap.containsKey(cd.day)) {
            int moodDrawableRes = moodMap.get(cd.day);
            moodIcon.setImageResource(moodDrawableRes);
            moodIcon.setVisibility(View.VISIBLE);
        } else {
            moodIcon.setVisibility(View.INVISIBLE);
        }
        return view;
    }


}
