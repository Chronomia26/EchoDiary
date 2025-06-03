package com.bigo143.echodiary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private Calendar currentCalendar;
    private GestureDetectingGridView calendarGrid;
    private TextView yearText, monthText;
    private int selectedDay = -1;
    private int todayDay = -1;
    private CalendarAdapter calendarAdapter;
    private MoodNoteDBHelper dbHelper;
    private LinearLayout tasksContainer;
    private String selectedDate; // e.g., "2025/05/20"

    private View rootLayout;
    private MoodManager moodManager;
    private TaskManager taskManager;
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final Map<Integer, Integer> moodMap = new HashMap<>();
    private final Set<String> promptedDates = new HashSet<>();

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
        moodManager = new MoodManager(requireContext());
        dbHelper = new MoodNoteDBHelper(requireContext());
        selectedDate = dbDateFormat.format(currentCalendar.getTime());


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);


        calendarGrid = view.findViewById(R.id.calendarGrid);
        yearText = view.findViewById(R.id.yearText);
        monthText = view.findViewById(R.id.monthText);
        tasksContainer = view.findViewById(R.id.tasksContainer);
        taskManager = new TaskManager(requireContext());
        rootLayout = view.findViewById(R.id.rootLayout);
        LinearLayout moodLegend = view.findViewById(R.id.moodLegend);

        EditText taskInput = view.findViewById(R.id.taskInput);
        taskInput.setFocusable(false);
        taskInput.setFocusableInTouchMode(false);
        taskInput.setClickable(true);
        taskInput.setOnClickListener(v -> showTaskInputDialog(tasksContainer));

        yearText.setOnClickListener(v -> showYearPickerDialog());
        monthText.setOnClickListener(v -> showMonthPickerDialog());

        rootLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int[] loc = new int[2];
                calendarGrid.getLocationOnScreen(loc);
                int x = loc[0];
                int y = loc[1];
                int w = calendarGrid.getWidth();
                int h = calendarGrid.getHeight();

                float touchX = event.getRawX();
                float touchY = event.getRawY();

                boolean outside = !(touchX >= x && touchX <= x + w && touchY >= y && touchY <= y + h);

                if (outside) {
                    selectedDay = -1;
                    calendarAdapter.setSelectedDay(-1);

                    Calendar today = Calendar.getInstance();
                    if (today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                            today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
                        todayDay = today.get(Calendar.DAY_OF_MONTH);
                        calendarAdapter.setCurrentDay(todayDay);
                    } else {
                        todayDay = -1;
                        calendarAdapter.setCurrentDay(-1);
                    }

                    calendarGrid.invalidateViews();
                    return true;
                }
            }
            return false;
        });

        if (!isMoodEnabled()) {
            moodLegend.setVisibility(View.GONE); // Hide the layout
        } else {
            moodLegend.setVisibility(View.VISIBLE); // Show the layout
        }


        calendarGrid.setOnSwipeListener(new GestureDetectingGridView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                changeMonth(1);
            }

            @Override
            public void onSwipeRight() {
                changeMonth(-1);
            }
        });

        calendarGrid.setOnItemClickListener((parent, view1, position, id) -> {
            String dayStr = calendarAdapter.getItem(position);
            if (dayStr.isEmpty()) return;
            int day = Integer.parseInt(dayStr);

            selectedDay = day;
            calendarAdapter.setSelectedDay(day);
            calendarGrid.invalidateViews();

            selectedDate = String.format(Locale.getDefault(), "%04d/%02d/%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1, day);

            showTasksForDate(tasksContainer);

            if (isMoodEnabled() && !promptedDates.contains(selectedDate)) {
                showMoodNoteDialog(selectedDate);
                promptedDates.add(selectedDate);
            }
        });

        calendarGrid.setOnItemLongClickListener((parent, view1, position, id) -> {
            String dayStr = calendarAdapter.getItem(position);
            if (dayStr.isEmpty()) return false;
            int day = Integer.parseInt(dayStr);
            selectedDay = day;
            calendarAdapter.setSelectedDay(day);
            calendarGrid.invalidateViews();

            selectedDate = String.format(Locale.getDefault(), "%04d/%02d/%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1, day);

            showTasksForDate(tasksContainer);

            if (isMoodEnabled()) {
                showMoodNoteDialog(selectedDate);
            }
            return true;
        });

        loadCalendar();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        showTasksForDate(tasksContainer); // RELOAD from DB every time fragment is visible
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

    private boolean isMoodEnabled() {
        return requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean("mood_enabled", true);
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
        if (isMoodEnabled()) {
            for (int i = 1; i <= maxDay; i++) {
                String dateKey = String.format(Locale.getDefault(), "%04d/%02d/%02d",
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        i);
                MoodNoteDBHelper.MoodNote moodNote = moodManager.getMood(dateKey);
                if (moodNote != null) {
                    // Map moods to your drawable ids
                    int drawableId = R.drawable.ic_neutral_dot; // default
                    switch (moodNote.mood) {
                        case "happy":
                            drawableId = R.drawable.ic_happy_dot;
                            break;
                        case "sad":
                            drawableId = R.drawable.ic_sad_dot;
                            break;
                        case "neutral":
                            drawableId = R.drawable.ic_neutral_dot;
                            break;
                    }
                    moodMap.put(i, drawableId);
                }
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

        // Highlight today only if nothing is selected
        if (selectedDay == -1 && todayDay != -1) {
            calendarAdapter.setSelectedDay(-1); // Force adapter to differentiate
            calendarAdapter.setCurrentDay(todayDay); // Ensure today is highlighted
        }

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

    private void showEditDialog(CheckBox taskCheckbox, MoodNoteDBHelper.Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Task");

        EditText input = new EditText(getContext());
        input.setText(task.getText());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSelection(input.getText().length());

        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String editedText = input.getText().toString().trim();
            if (!editedText.isEmpty()) {
                // Use task ID to update the task in DB
                taskManager.updateTaskById(
                        task.getId(),    // task ID
                        editedText,      // newText
                        task.getTime(),  // keep old time
                        task.isChecked() // keep old checked status
                );

                // Update the task object locally as well
                task.setText(editedText);

                // Update the checkbox text on UI
                taskCheckbox.setText(editedText);
            } else {
                Toast.makeText(getContext(), "Task cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private void addTaskView(LinearLayout container, MoodNoteDBHelper.Task task) {
        // Inflate task_item.xml layout for one task
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View taskView = inflater.inflate(R.layout.task_item, container, false);

        CheckBox taskCheckBox = taskView.findViewById(R.id.taskCheckBox);
        TextView taskTimeText = taskView.findViewById(R.id.taskTimeText);
        ImageButton taskEditButton = taskView.findViewById(R.id.taskEditButton);

        // Set checkbox text and checked state
        taskCheckBox.setText(task.getText());
        taskCheckBox.setChecked(task.isChecked());

        // Set task time
        taskTimeText.setText(task.getTime());

        // Checkbox listener to update checked state in DB
        taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Delete task to avoid container overload
                taskManager.deleteTaskById(task.getId());
                // Also remove checkbox from the UI container (e.g., tasksContainer.removeView(checkBox))
                ((ViewGroup) taskCheckBox.getParent()).removeView(taskCheckBox);
            } else {
                // Update the task as unchecked
                taskManager.updateTaskById(task.getId(), task.getText(), task.getTime(), false);
                task.setChecked(false);
            }
        });

        // Edit button to open your existing edit dialog
        taskEditButton.setOnClickListener(v -> showEditDialog(taskCheckBox, task));

        // Add the inflated task view to the container
        container.addView(taskView);
    }




    private CheckBox createTaskCheckbox(MoodNoteDBHelper.Task task) {
        CheckBox checkBox = new CheckBox(getContext());
        checkBox.setText(task.getText());
        checkBox.setChecked(task.isChecked());

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Delete task to avoid container overload
                taskManager.deleteTaskById(task.getId());
                // Also remove checkbox from the UI container (e.g., tasksContainer.removeView(checkBox))
                ((ViewGroup) checkBox.getParent()).removeView(checkBox);
            } else {
                // Update the task as unchecked
                    taskManager.updateTaskById(task.getId(), task.getText(), task.getTime(), false);
                    task.setChecked(false);
            }
        });

        checkBox.setOnLongClickListener(v -> {
            showEditDialog(checkBox, task);
            return true;
        });

        return checkBox;
    }



    private void showTaskInputDialog(LinearLayout container) {
        if (getContext() == null || selectedDate == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Task");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter task description");

        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String taskText = input.getText().toString().trim();
            if (!taskText.isEmpty()) {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                taskManager.insertTask(selectedDate, taskText, time, false);
                showTasksForDate(tasksContainer);  // Refresh list
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showTasksForDate(LinearLayout container) {
        if (selectedDate == null) return;
        container.removeAllViews();

        List<MoodNoteDBHelper.Task> tasks = taskManager.getTasks(selectedDate);
        for (MoodNoteDBHelper.Task task : tasks) {
            addTaskView(container, task);
        }
    }


    private void showMoodNoteDialog(String date) {
        MoodNoteDBHelper.MoodNote existingMoodNote = moodManager.getMood(date);

        String[] moods = getResources().getStringArray(R.array.mood_options);
        int selectedMoodIndex = 2; // default "Neutral"
        if (existingMoodNote != null) {
            switch (existingMoodNote.mood) {
                case "happy": selectedMoodIndex = 0; break;
                case "sad": selectedMoodIndex = 1; break;
                case "neutral": selectedMoodIndex = 2; break;
            }
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        Spinner moodSpinner = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, moods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(adapter);
        moodSpinner.setSelection(selectedMoodIndex);
        layout.addView(moodSpinner);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Mood for " + date)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedMood = "neutral";
                    switch (moodSpinner.getSelectedItemPosition()) {
                        case 0: selectedMood = "happy"; break;
                        case 1: selectedMood = "sad"; break;
                        case 2: selectedMood = "neutral"; break;
                    }

                    moodManager.saveMood(date, selectedMood, "");
                    loadCalendar();
                })
                .setNegativeButton("Cancel", null);

        // Only show Delete button if there's an existing mood
        if (existingMoodNote != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                moodManager.deleteMood(date);
                loadCalendar();
            });
        }

        builder.show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        taskManager = null;
    }
}
