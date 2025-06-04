package com.bigo143.echodiary;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private AppCompatDelegate view;

    public HomeFragment() {
    }
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String today = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());

//        ImageView characterIcon = view.findViewById(R.id.character_icon);
//
//        characterIcon.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Example: open ProfileActivity (replace with your actual target)
//                Intent intent = new Intent(getActivity(), ChatbotActivity.class);
//                startActivity(intent);
//            }
//        });


        MoodNoteDBHelper db = new MoodNoteDBHelper(getContext());
        List<MoodNoteDBHelper.Task> tasksToday = db.getTasksByDate(today);

        LinearLayout homeTaskList = view.findViewById(R.id.home_task_list); // make sure this exists in fragment_home.xml
        homeTaskList.removeAllViews();

        for (MoodNoteDBHelper.Task task : tasksToday) {
            View taskView = createTaskView(task);
            homeTaskList.addView(taskView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayTasks(); // Custom method that fetches tasks and updates the layout
    }

    private void loadTodayTasks() {
        String today = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());
        MoodNoteDBHelper db = new MoodNoteDBHelper(getContext());
        List<MoodNoteDBHelper.Task> tasksToday = db.getTasksByDate(today);

        LinearLayout homeTaskList = getView().findViewById(R.id.home_task_list);
        homeTaskList.removeAllViews();

        for (MoodNoteDBHelper.Task task : tasksToday) {
            View taskView = createTaskView(task);
            homeTaskList.addView(taskView);
        }
    }



    private View createTaskView(MoodNoteDBHelper.Task task) {
        TextView taskTextView = new TextView(getContext());
        taskTextView.setText(task.getTime() + " - " + task.getText() + " - " + task.getDate());
        taskTextView.setTextSize(16);
        taskTextView.setPadding(16, 16, 16, 16);
        return taskTextView;
    }
}