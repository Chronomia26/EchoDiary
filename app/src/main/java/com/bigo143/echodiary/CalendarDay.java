package com.bigo143.echodiary;

public class CalendarDay {
    public int day;
    public boolean isCurrentMonth;
    public boolean isTrailing;
    public boolean isLeading;

    public CalendarDay(int day, boolean isCurrentMonth, boolean isTrailing, boolean isLeading) {
        this.day = day;
        this.isCurrentMonth = isCurrentMonth;
        this.isTrailing = isTrailing;
        this.isLeading = isLeading;
    }
}
