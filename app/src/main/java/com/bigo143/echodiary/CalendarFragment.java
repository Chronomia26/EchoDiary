package com.bigo143.echodiary;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private static final int ANIMATION_DURATION = 300; // or any duration you prefer (ms)

    private int displayedYear;
    private int displayedMonth;

    private final int[] moodDrawables = {
            R.drawable.ic_happy_dot,
            R.drawable.ic_sad_dot,
            R.drawable.ic_neutral_dot
    };

    private TextView dailySummaryView;


    public CalendarFragment() {
        // Required empty constructor
    }
    private boolean isCalendarExpanded = false; // Global to persist state
    final int[] calendarNormalHeightHolder = new int[1]; // Store normal height after layout
    final int[] calendarExpandedHeightHolder = new int[1];


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
        ImageView addSpecificTaskBtn = view.findViewById(R.id.addSpecificTasks);
        dailySummaryView = view.findViewById(R.id.textViews);

        EditText taskInput = view.findViewById(R.id.taskInput);
        taskInput.setFocusable(false);
        taskInput.setFocusableInTouchMode(false);
        taskInput.setClickable(true);
        taskInput.setOnClickListener(v -> showTaskInputDialog(tasksContainer));

        yearText.setOnClickListener(v -> showYearPickerDialog());
        monthText.setOnClickListener(v -> showMonthPickerDialog());

        // Variables for vertical swipe detection & expand/collapse
        final float[] startY = new float[1];  // Use array to modify inside lambda
        final int SWIPE_THRESHOLD = 100;  // Minimum vertical swipe distance
        final int animationDuration = 300;

        addSpecificTaskBtn.setVisibility(View.GONE);  // Hide initially

        // Capture calendar normal & expanded height after layout
        calendarGrid.post(() -> {
            calendarNormalHeightHolder[0] = calendarGrid.getHeight();
            calendarExpandedHeightHolder[0] = (int) (calendarNormalHeightHolder[0] * 1.5f);
            calendarGrid.requestLayout();  // force re-layout
            calendarGrid.invalidate();     // force redraw
        });

        updateDailySummary(dailySummaryView);

        calendarGrid.setOnVerticalSwipeListener(new GestureDetectingGridView.OnVerticalSwipeListener() {
            @Override
            public void onSwipeDown() {
                if (!isCalendarExpanded) {
                    setCalendarExpandedState(true);
                }
            }

            @Override
            public void onSwipeUp() {
                if (isCalendarExpanded) {
                    setCalendarExpandedState(false);
                }
            }
        });



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

        rootLayout.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                // Save startY for swipe detection
                startY[0] = event.getRawY();

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

                    addSpecificTaskBtn.setVisibility(View.GONE);
                    calendarGrid.invalidateViews();

                    return true; // consume event to prevent further propagation
                }
            } else if (action == MotionEvent.ACTION_UP) {
                float endY = event.getRawY();
                float deltaY = endY - startY[0];

                if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                    if (deltaY > 0 && !isCalendarExpanded) {
                        animateCalendarHeight(calendarGrid, calendarExpandedHeightHolder[0], animationDuration);
                        scaleCalendarItems(calendarGrid, 1.2f, animationDuration);
                        calendarAdapter.setExpanded(true);
                        isCalendarExpanded = true;
                        return true;
                    } else if (deltaY < 0 && isCalendarExpanded) {
                        animateCalendarHeight(calendarGrid, calendarNormalHeightHolder[0], animationDuration);
                        scaleCalendarItems(calendarGrid, 1.0f, animationDuration);
                        calendarAdapter.setExpanded(false);
                        isCalendarExpanded = false;
                        return true;
                    }
                }
            }
            return false; // allow other events to be handled normally
        });

        if (!isMoodEnabled()) {
            moodLegend.setVisibility(View.GONE); // Hide the layout
        } else {
            moodLegend.setVisibility(View.VISIBLE); // Show the layout
        }

        calendarGrid.setOnItemClickListener((parent, view1, position, id) -> {
            CalendarDay day = calendarAdapter.getItem(position);
            addSpecificTaskBtn.setVisibility(View.GONE);  // Hide button if invalid day clicked
            if (day == null || day.day <= 0) return;

            if (day.isCurrentMonth) {
                selectedDay = day.day;
                calendarAdapter.setSelectedDay(selectedDay);
                calendarGrid.invalidateViews();

                selectedDate = String.format(Locale.getDefault(), "%04d/%02d/%02d",
                        currentCalendar.get(Calendar.YEAR),
                        currentCalendar.get(Calendar.MONTH) + 1, selectedDay);

                showTasksForDate(tasksContainer);

                addSpecificTaskBtn.setVisibility(View.VISIBLE); // Show button when valid date selected


                if (isMoodEnabled() && !promptedDates.contains(selectedDate)) {
                    showMoodNoteDialog(selectedDate);
                    promptedDates.add(selectedDate);
                }
            } else {
                if (day.isTrailing) {
                    currentCalendar.add(Calendar.MONTH, -1);
                } else if (day.isLeading) {
                    currentCalendar.add(Calendar.MONTH, 1);
                }
                selectedDay = day.day;
                loadCalendar(); // This will re-highlight the selected day for the new month
                setCalendarExpandedState(isCalendarExpanded); // Reapply expanded/collapsed layout

                addSpecificTaskBtn.setVisibility(View.GONE); // Hide button after month switch
            }
        });

        calendarGrid.setOnItemLongClickListener((parent, view1, position, id) -> {
            CalendarDay day = calendarAdapter.getItem(position);
            addSpecificTaskBtn.setVisibility(View.GONE);  // Hide button if invalid day clicked
            if (day == null || !day.isCurrentMonth || day.day <= 0) return false;

            selectedDay = day.day;
            calendarAdapter.setSelectedDay(selectedDay);
            calendarGrid.invalidateViews();

            selectedDate = String.format(Locale.getDefault(), "%04d/%02d/%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1, selectedDay);

            showTasksForDate(tasksContainer);
            addSpecificTaskBtn.setVisibility(View.VISIBLE); // Show button when valid date selected


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

    private void setCalendarExpandedState(boolean expanded) {
        float scale = requireContext().getResources().getDisplayMetrics().density;
        int padding = (int) ((expanded ? 60 : 8) * scale + 0.5f);
        int margin = (int) ((expanded ? 12 : 6) * scale + 0.5f);
        int targetHeight = expanded ? calendarExpandedHeightHolder[0] : calendarNormalHeightHolder[0];

        animateCalendarHeight(calendarGrid, targetHeight, ANIMATION_DURATION);

        calendarGrid.animate().alpha(0f).setDuration(100).withEndAction(() -> {
            calendarAdapter.setExpanded(expanded);
            calendarAdapter.setTotalHeight(targetHeight);
            calendarAdapter.setBottomCellMargin(margin);
            calendarGrid.animate().alpha(1f).setDuration(100).start();
        }).start();

        calendarGrid.setPadding(0, expanded ? 0 : padding, 0, expanded ? 0 : padding);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) calendarGrid.getLayoutParams();
        params.setMargins(0, expanded ? 0 : padding, 0, expanded ? 0 : padding);
        calendarGrid.setLayoutParams(params);

        isCalendarExpanded = expanded;
    }

    private void scaleCalendarItems(GridView gridView, float scaleFactor, long duration) {
        for (int i = 0; i < gridView.getChildCount(); i++) {
            View child = gridView.getChildAt(i);
            if (child instanceof TextView) {
                TextView dayView = (TextView) child;

                dayView.animate()
                        .scaleX(scaleFactor)
                        .scaleY(scaleFactor)
                        .setDuration(duration)
                        .start();

            }
        }
    }

    private void animateCalendarHeight(View view, int toHeight, int duration) {
        int fromHeight = view.getHeight();
        ValueAnimator animator = ValueAnimator.ofInt(fromHeight, toHeight);
        animator.setDuration(duration);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = val;

            // Make sure the GridView remains aligned to top
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) layoutParams).topMargin = 0;
            }

            view.setLayoutParams(layoutParams);
        });
        animator.start();
    }

    private void changeMonth(int offset) {
        currentCalendar.add(Calendar.MONTH, offset);
        loadCalendar();

        calendarGrid.post(() -> setCalendarExpandedState(isCalendarExpanded));
    }



    private boolean isMoodEnabled() {
        return requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean("mood_enabled", true);
    }

    private void loadCalendar() {
        List<CalendarDay> calendarDays = new ArrayList<>();
        Calendar calendar = (Calendar) currentCalendar.clone();
        Set<Integer> sundayPositions = new HashSet<>();
        Set<Integer> taskDays = new HashSet<>();

        displayedYear = calendar.get(Calendar.YEAR);
        displayedMonth = calendar.get(Calendar.MONTH); // 0-based

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Fill days from previous month
        Calendar prevMonth = (Calendar) calendar.clone();
        prevMonth.add(Calendar.MONTH, -1);
        int maxPrevDay = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = firstDayOfWeek - 1; i >= 0; i--) {
            CalendarDay cd = new CalendarDay(maxPrevDay - i, false, true, false);
            calendarDays.add(cd);
        }

        // Current month days
        for (int i = 1; i <= maxDay; i++) {
            CalendarDay cd = new CalendarDay(i, true, false, false);
            calendar.set(Calendar.DAY_OF_MONTH, i);
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                sundayPositions.add(firstDayOfWeek + i - 1);
            }
            calendarDays.add(cd);
        }

        // Leading days from next month
        int totalCells = calendarDays.size();
        int rows = (int) Math.ceil(totalCells / 7.0);
        int requiredCells = (rows == 5 && totalCells <= 35) ? 35 : 42;

        for (int i = 1; calendarDays.size() < requiredCells; i++) {
            CalendarDay cd = new CalendarDay(i, false, false, true);
            calendarDays.add(cd);
        }


        calendar.set(Calendar.DAY_OF_MONTH, 1);

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

        if (taskManager != null) {
            for (int i = 1; i <= maxDay; i++) {
                String dateKey = String.format(Locale.getDefault(), "%04d/%02d/%02d",
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        i);
                List<MoodNoteDBHelper.Task> tasks = taskManager.getTasks(dateKey); // <--- here fixed
                if (tasks != null && !tasks.isEmpty()) {
                    taskDays.add(i);
                }
            }
        }


        String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.getTime());
        monthText.setText(monthName);
        yearText.setText(year);

        calendarAdapter = new CalendarAdapter(requireContext(), calendarDays, moodMap,
                calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR), firstDayOfWeek);

        calendarAdapter.setSundayPositions(sundayPositions); // 🔴 Apply Sunday highlights
        calendarAdapter.setTaskDays(taskDays); // ✅ Make sure this method exists in your adapter
        calendarGrid.setAdapter(calendarAdapter);
        calendarAdapter.setExpanded(isCalendarExpanded);
        calendarGrid.invalidateViews();

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

        // Highlight today only if nothing is selected
        if (selectedDay == -1 && todayDay != -1) {
            calendarAdapter.setSelectedDay(-1); // Force adapter to differentiate
            calendarAdapter.setCurrentDay(todayDay); // Ensure today is highlighted
        }

    }

    private void showYearPickerDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_year_picker, null);
        NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        int currentYear = currentCalendar.get(Calendar.YEAR);

        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(currentYear);
        setNumberPickerTextSize(yearPicker, 16f);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Optional: transparent rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> {
            currentCalendar.set(Calendar.YEAR, yearPicker.getValue());
            loadCalendar();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Wrap content width/height to avoid extra space
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }


    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showMonthPickerDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_month_picker, null);
        NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        int currentMonth = currentCalendar.get(Calendar.MONTH);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(160), ViewGroup.LayoutParams.WRAP_CONTENT);
        monthPicker.setLayoutParams(params);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        });
        monthPicker.setValue(currentMonth);
        setNumberPickerTextSize(monthPicker, 16f);
        monthPicker.setWrapSelectorWheel(false); // to prevent looping which sometimes adds space


        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Optional: make background rounded
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> {
            currentCalendar.set(Calendar.MONTH, monthPicker.getValue());
            loadCalendar();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        // Force dialog to wrap content
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

    }

    private void setNumberPickerTextSize(NumberPicker numberPicker, float textSizeSp) {
        float scaledSizeInPixels = textSizeSp * getResources().getDisplayMetrics().scaledDensity;
        int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextSize(scaledSizeInPixels);
            }
        }
    }

    private void showEditDialog(CheckBox taskCheckbox, MoodNoteDBHelper.Task task) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        EditText inputField = dialogView.findViewById(R.id.inputField);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Edit Task");

        inputField.setText(task.getText());
        inputField.setSelection(inputField.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String editedText = inputField.getText().toString().trim();
            if (!editedText.isEmpty()) {
                taskManager.updateTaskById(
                        task.getId(),
                        editedText,
                        task.getTime(),
                        task.isChecked()
                );
                task.setText(editedText);
                taskCheckbox.setText(editedText);
                refreshTaskMarksOnCalendar();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Task cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }


    private void showTaskInputDialog(LinearLayout container) {
        if (getContext() == null || selectedDate == null) return;

        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        // Reference views from the layout
        EditText inputField = dialogView.findViewById(R.id.inputField);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Add Task");

        // Build AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set background for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Handle OK button click
        btnOk.setOnClickListener(v -> {
            String taskText = inputField.getText().toString().trim();
            if (!taskText.isEmpty()) {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                taskManager.insertTask(selectedDate, taskText, time, false);
                refreshTaskMarksOnCalendar();

                if (selectedDate.equals(getTodayDate())) {
                    updateDailySummary(dailySummaryView);
                }

                showTasksForDate(container);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Task cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Cancel button click
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
                showTasksForDate(tasksContainer);
                if (task.getDate().equals(getTodayDate())) {
                    updateDailySummary(dailySummaryView);
                }
                // Also remove checkbox from the UI container (e.g., tasksContainer.removeView(checkBox))
                ((ViewGroup) taskCheckBox.getParent()).removeView(taskCheckBox);
                refreshTaskMarksOnCalendar(); // <-- update underline on calendar
            } else {
                // Update the task as unchecked
                if (task.equals(getTodayDate())) {
                    updateDailySummary(dailySummaryView);
                }
                taskManager.updateTaskById(task.getId(), task.getText(), task.getTime(), false);
                task.setChecked(false);
                refreshTaskMarksOnCalendar();
            }
        });

        // Edit button to open your existing edit dialog
        taskEditButton.setOnClickListener(v -> showEditDialog(taskCheckBox, task));

        // Add the inflated task view to the container
        container.addView(taskView);
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

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mood_picker, null);
        Spinner moodSpinner = dialogView.findViewById(R.id.moodSpinner);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);

        dialogTitle.setText("Your Mood Today");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, moods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(adapter);
        moodSpinner.setSelection(selectedMoodIndex);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnDelete.setVisibility(existingMoodNote != null ? View.VISIBLE : View.GONE);


        btnSave.setOnClickListener(v -> {
            String selectedMood = "neutral";
            switch (moodSpinner.getSelectedItemPosition()) {
                case 0: selectedMood = "happy"; break;
                case 1: selectedMood = "sad"; break;
                case 2: selectedMood = "neutral"; break;
            }
            moodManager.saveMood(date, selectedMood, "");
            loadCalendar();
            setCalendarExpandedState(isCalendarExpanded); // restore state
            dialog.dismiss();

            if (date.equals(getTodayDate())) {
                updateDailySummary(dailySummaryView);
            }
            refreshTaskMarksOnCalendar();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            moodManager.deleteMood(date);
            loadCalendar();
            setCalendarExpandedState(isCalendarExpanded);
            dialog.dismiss();

            if (date.equals(getTodayDate())) {
                updateDailySummary(dailySummaryView);
            }
            refreshTaskMarksOnCalendar();
        });

        dialog.show();

        // Force dialog wrap content width and height to avoid excess space
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }


    private void refreshTaskMarksOnCalendar() {
        List<MoodNoteDBHelper.Task> tasks = taskManager.getTasksForMonth(displayedYear, displayedMonth + 1);
        Set<Integer> daysWithTasks = new HashSet<>();

        for (MoodNoteDBHelper.Task task : tasks) {
            String date = task.getDate(); // Expecting yyyy-MM-dd

            if (date != null && date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = date.split("-");
                try {
                    int day = Integer.parseInt(parts[2]);
                    daysWithTasks.add(day);
                } catch (NumberFormatException e) {
                    e.printStackTrace(); // or Log.w("Calendar", "Invalid date: " + date);
                }
            }
        }

        calendarAdapter.setTaskDays(daysWithTasks);
        calendarGrid.invalidateViews(); // Refresh visuals
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        taskManager = null;
    }

    private void updateDailySummary(TextView summaryView) {
        String todayDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // 1. Fetch Mood
        MoodNoteDBHelper.MoodNote moodNote = moodManager.getMood(todayDate);
        String moodSummary;
        if (moodNote == null) {
            moodSummary = "Mood not set";
        } else {
            switch (moodNote.mood) {
                case "happy":
                    moodSummary = "You're feeling happy today 😊";
                    break;
                case "sad":
                    moodSummary = "You're feeling sad today 😔";
                    break;
                case "neutral":
                default:
                    moodSummary = "You're feeling neutral today 😐";
                    break;
            }
        }

        // 2. Fetch Tasks
        List<MoodNoteDBHelper.Task> tasks = taskManager.getTasks(todayDate);
        int taskCount = tasks != null ? tasks.size() : 0;
        String taskSummary = (taskCount > 0)
                ? "You have " + taskCount + (taskCount == 1 ? " task" : " tasks") + " today."
                : "No tasks scheduled today.";

        // 3. Generate Advice
        String advice = (taskCount > 0)
                ? "Stay focused and check off your list! ✅"
                : "Enjoy your free time or plan something meaningful! 🌱";

        // 4. Combine and display
        String fullSummary = moodSummary + "\n" + taskSummary + "\n" + advice;
        summaryView.setText(fullSummary);
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }

}

