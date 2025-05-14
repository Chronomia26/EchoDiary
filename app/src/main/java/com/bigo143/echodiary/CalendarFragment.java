package com.bigo143.echodiary;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.motion.widget.MotionLayout;
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

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private Calendar currentCalendar;
    private GridView calendarGrid;
    private TextView monthYearText;
    private ImageButton prevMonthButton, nextMonthButton;
    private TextView yearText, monthText;


    private final Map<Integer, Integer> moodMap = new HashMap<>();
    private final Random random = new Random();

    private final Map<String, String> tasksPerDate = new HashMap<>();


    private final int[] moodDrawables = {
            R.drawable.ic_happy_dot,
            R.drawable.ic_sad_dot,
            R.drawable.ic_neutral_dot
    };

    public CalendarFragment() {
        // Required empty public constructor
    }

    public static CalendarFragment newInstance(String param1, String param2) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        currentCalendar = Calendar.getInstance(); // Initialize calendar here
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Bind views
        calendarGrid = view.findViewById(R.id.calendarGrid);
        yearText = view.findViewById(R.id.yearText);
        monthText = view.findViewById(R.id.monthText);

        EditText taskInput = view.findViewById(R.id.taskInput);
        FrameLayout editTextContainer = view.findViewById(R.id.editTextContainer);
        LinearLayout tasksContainer = view.findViewById(R.id.tasksContainer);
        View dimBackground = view.findViewById(R.id.dimBackground);


        yearText.setOnClickListener(v -> showYearPickerDialog());
        monthText.setOnClickListener(v -> showMonthPickerDialog());

        taskInput.setFocusable(false); // Prevent keyboard
        taskInput.setOnClickListener(v -> {
            showTaskInputDialog(tasksContainer);
        });

        if (calendarGrid instanceof GestureDetectingGridView) {
            ((GestureDetectingGridView) calendarGrid).setOnSwipeListener(new GestureDetectingGridView.OnSwipeListener() {
                @Override
                public void onSwipeLeft() {
                    changeMonth(1);
                }

                @Override
                public void onSwipeRight() {
                    changeMonth(-1);
                }
            });
        }


        taskInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                v.clearFocus();
                hideKeyboard(v);

                String taskText = taskInput.getText().toString().trim();
                if (!taskText.isEmpty()) {
                    addTaskToLayout(taskText, tasksContainer);
                    taskInput.setText(""); // Clear input
                }
                return true;
            }
            return false;
        });

        // Initial load
        loadCalendar();

        return view;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
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

        // Add blank cells before 1st of the month
        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add("");
        }

        // Add actual days
        for (int i = 1; i <= maxDay; i++) {
            days.add(String.valueOf(i));
        }

        // Generate mock mood data
        moodMap.clear();
        for (int i = 1; i <= maxDay; i++) {
            if (random.nextBoolean()) {
                moodMap.put(i, moodDrawables[random.nextInt(moodDrawables.length)]);
            }
        }

        // Extract month and year to pass to adapter
        int displayedMonth = calendar.get(Calendar.MONTH); // 0–11
        int displayedYear = calendar.get(Calendar.YEAR);

        // Update header text
        String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.getTime());

        monthText.setText(monthName);
        yearText.setText(year);

        // ✅ Fixed line with proper context and month/year
        CalendarAdapter adapter = new CalendarAdapter(requireContext(), days, moodMap, displayedMonth, displayedYear);
        calendarGrid.setAdapter(adapter);
    }


    private void showYearPickerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_year_picker, null);
        final NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        int currentYear = currentCalendar.get(Calendar.YEAR);

        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(currentYear);
        setNumberPickerTextSize(yearPicker, 30f);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Year")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int selectedYear = yearPicker.getValue();
                    currentCalendar.set(Calendar.YEAR, selectedYear);
                    loadCalendar();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMonthPickerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_month_picker, null);
        final NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);

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
                    int selectedMonth = monthPicker.getValue();
                    currentCalendar.set(Calendar.MONTH, selectedMonth);
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
                try {
                    ((EditText) child).setTextSize(textSizeSp); // e.g., 22sp
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showEditDialog(CheckBox taskCheckbox) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Task");

        final EditText input = new EditText(getContext());
        input.setText(taskCheckbox.getText().toString());

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String editedText = input.getText().toString().trim();
            if (!editedText.isEmpty()) {
                taskCheckbox.setText(editedText);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private void addTaskToLayout(String taskText, LinearLayout container) {
        LinearLayout taskLayout = new LinearLayout(getContext());
        taskLayout.setOrientation(LinearLayout.VERTICAL);
        taskLayout.setPadding(32, 24, 32, 24);
        taskLayout.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.task_background));
        taskLayout.setBackgroundResource(R.drawable.task_background);

        CheckBox taskCheckbox = new CheckBox(getContext());
        taskCheckbox.setText(taskText);

        // Get the current selected date (you could also pass the selected date from the calendar view)
        String selectedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(currentCalendar.getTime());

        // Store task with date key
        tasksPerDate.put(selectedDate, taskText);

        // Allow edit on long-press
        taskCheckbox.setOnLongClickListener(view -> {
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

    private void showTaskInputDialog(LinearLayout tasksContainer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final EditText input = new EditText(requireContext());
        input.setHint("Add your task...");
        builder.setTitle("Add Task")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String taskText = input.getText().toString().trim();
                    if (!taskText.isEmpty()) {
                        addTaskToLayout(taskText, tasksContainer);

                        String selectedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(currentCalendar.getTime());
                        tasksPerDate.put(selectedDate, taskText);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



}
