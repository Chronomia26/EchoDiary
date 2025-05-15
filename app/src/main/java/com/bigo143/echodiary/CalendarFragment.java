package com.bigo143.echodiary;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class CalendarFragment extends Fragment {

    private Calendar currentCalendar;
    private GestureDetectingGridView calendarGrid;
    private TextView yearText, monthText;
    private int selectedDay = -1;
    private int todayDay = -1;
    private CalendarAdapter calendarAdapter;

    private final Map<Integer, Integer> moodMap = new HashMap<>();
    private final Random random = new Random();

    private final Map<String, List<String>> tasksPerDate = new HashMap<>();

    private final int[] moodDrawables = {
            R.drawable.ic_happy_dot,
            R.drawable.ic_sad_dot,
            R.drawable.ic_neutral_dot
    };

    public CalendarFragment() {
        // Required empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentCalendar = Calendar.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarGrid = view.findViewById(R.id.calendarGrid);
        yearText = view.findViewById(R.id.yearText);
        monthText = view.findViewById(R.id.monthText);

        EditText taskInput = view.findViewById(R.id.taskInput);
        LinearLayout tasksContainer = view.findViewById(R.id.tasksContainer);

        yearText.setOnClickListener(v -> showYearPickerDialog());
        monthText.setOnClickListener(v -> showMonthPickerDialog());

        // Set swipe listener to respond to left/right swipes
        calendarGrid.setOnSwipeListener(new GestureDetectingGridView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                changeMonth(1);  // Swipe left to go to next month
            }

            @Override
            public void onSwipeRight() {
                changeMonth(-1); // Swipe right to go to previous month
            }
        });

        // Make taskInput non-editable but clickable
        taskInput.setFocusable(false);
        taskInput.setFocusableInTouchMode(false);
        taskInput.setClickable(true);
        taskInput.setOnClickListener(v -> showTaskInputDialog(tasksContainer));

        // Remove this listener since taskInput is non-editable (optional)
        // You can remove if you want to allow keyboard input, but currently it's dialog-based input.
        taskInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                v.clearFocus();
                hideKeyboard(v);

                String taskText = taskInput.getText().toString().trim();
                if (!taskText.isEmpty()) {
                    addTaskToLayout(taskText, tasksContainer);
                    taskInput.setText("");
                }
                return true;
            }
            return false;
        });

        loadCalendar();

        calendarGrid.setOnItemClickListener((parent, view1, position, id) -> {
            String dayStr = calendarAdapter.getItem(position);
            if (dayStr != null && !dayStr.isEmpty()) {
                selectedDay = Integer.parseInt(dayStr);
                calendarAdapter.setSelectedDay(selectedDay);
                calendarAdapter.setCurrentDay(-1);
                calendarGrid.invalidateViews();

                showTasksForDate(tasksContainer);
            }
        });

        return view;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void changeMonth(int offset) {
        currentCalendar.add(Calendar.MONTH, offset);
        loadCalendar();
    }

    private void loadCalendar() {
        List<String> days = new ArrayList<>();
        Calendar calendar = (Calendar) currentCalendar.clone();

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add("");
        }

        for (int i = 1; i <= maxDay; i++) {
            days.add(String.valueOf(i));
        }

        moodMap.clear();
        for (int i = 1; i <= maxDay; i++) {
            if (random.nextBoolean()) {
                moodMap.put(i, moodDrawables[random.nextInt(moodDrawables.length)]);
            }
        }

        String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.getTime());
        monthText.setText(monthName);
        yearText.setText(year);

        calendarAdapter = new CalendarAdapter(requireContext(), days, moodMap,
                calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR), firstDayOfWeek);
        calendarGrid.setAdapter(calendarAdapter);

        Calendar today = Calendar.getInstance();
        if (today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
            todayDay = today.get(Calendar.DAY_OF_MONTH);
            calendarAdapter.setCurrentDay(todayDay);
        } else {
            todayDay = -1;
            calendarAdapter.setCurrentDay(-1);
        }

        calendarAdapter.setSelectedDay(selectedDay);
        calendarGrid.invalidateViews();
    }

    private void showYearPickerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_year_picker, null);
        NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        int currentYear = currentCalendar.get(Calendar.YEAR);

        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(currentYear);
        setNumberPickerTextSize(yearPicker, 30f);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Year")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    currentCalendar.set(Calendar.YEAR, yearPicker.getValue());
                    loadCalendar();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMonthPickerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_month_picker, null);
        NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);

        int currentMonth = currentCalendar.get(Calendar.MONTH);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        });
        monthPicker.setValue(currentMonth);
        setNumberPickerTextSize(monthPicker, 30f);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Month")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    currentCalendar.set(Calendar.MONTH, monthPicker.getValue());
                    loadCalendar();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setNumberPickerTextSize(NumberPicker numberPicker, float textSizeSp) {
        int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText) {
                ((EditText) child).setTextSize(textSizeSp);
            }
        }
    }

    private void showEditDialog(CheckBox taskCheckbox) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Task");

        EditText input = new EditText(getContext());
        input.setText(taskCheckbox.getText().toString());

        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String editedText = input.getText().toString().trim();
            if (!editedText.isEmpty()) {
                // Update task text in UI
                String oldText = taskCheckbox.getText().toString();
                taskCheckbox.setText(editedText);

                // Update task in tasksPerDate
                Calendar date = (Calendar) currentCalendar.clone();
                if (selectedDay != -1) {
                    date.set(Calendar.DAY_OF_MONTH, selectedDay);
                }
                String selectedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date.getTime());

                List<String> tasks = tasksPerDate.get(selectedDate);
                if (tasks != null) {
                    int idx = tasks.indexOf(oldText);
                    if (idx != -1) {
                        tasks.set(idx, editedText);
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addTaskToLayout(String taskText, LinearLayout container) {
        if (getContext() == null) return;

        LinearLayout taskLayout = new LinearLayout(getContext());
        taskLayout.setOrientation(LinearLayout.VERTICAL);

        int paddingPx = (int) (32 * getResources().getDisplayMetrics().density);
        taskLayout.setPadding(paddingPx, (int)(paddingPx / 1.5f), paddingPx, (int)(paddingPx / 1.5f));
        taskLayout.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.task_background));

        CheckBox taskCheckbox = new CheckBox(getContext());
        taskCheckbox.setText(taskText);

        // Only add task to tasksPerDate if it doesn't exist yet (prevent duplicates when refreshing)
        Calendar date = (Calendar) currentCalendar.clone();
        if (selectedDay != -1) {
            date.set(Calendar.DAY_OF_MONTH, selectedDay);
        }
        String selectedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date.getTime());

        List<String> tasks = tasksPerDate.computeIfAbsent(selectedDate, k -> new ArrayList<>());
        if (!tasks.contains(taskText)) {
            tasks.add(taskText);
        }

        taskCheckbox.setOnLongClickListener(v -> {
            showEditDialog(taskCheckbox);
            return true;
        });

        TextView timeText = new TextView(getContext());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        timeText.setText("Added at " + time);
        timeText.setTextSize(12);
        timeText.setTextColor(Color.GRAY);

        taskLayout.addView(taskCheckbox);
        taskLayout.addView(timeText);

        container.addView(taskLayout);
    }

    private void showTaskInputDialog(LinearLayout container) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Task");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter task description");

        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String taskText = input.getText().toString().trim();
            if (!taskText.isEmpty()) {
                addTaskToLayout(taskText, container);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showTasksForDate(LinearLayout container) {
        container.removeAllViews();

        Calendar date = (Calendar) currentCalendar.clone();
        if (selectedDay != -1) {
            date.set(Calendar.DAY_OF_MONTH, selectedDay);
        }
        String selectedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date.getTime());

        List<String> tasks = tasksPerDate.get(selectedDate);
        if (tasks != null) {
            for (String taskText : tasks) {
                addTaskToLayout(taskText, container);
            }
        }
    }

}
