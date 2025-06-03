package com.bigo143.echodiary;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import android.widget.ImageView;
import android.content.Intent;
import android.widget.Toast;


public class DiaryFragment extends Fragment {

    private DiaryAdapter adapter;
    private ActivityResultLauncher<Intent> detailLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.diaryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DiaryAdapter(getContext(), new ArrayList<>());

        recyclerView.setAdapter(adapter);


        DiaryDatabase.getInstance(requireContext())
                .diaryDao()
                .getAllEntries()
                .observe(getViewLifecycleOwner(), entries -> {
                    adapter.setEntries(entries);
                });

        // Handle Add Note button click
        ImageView addNoteButton = view.findViewById(R.id.image_AddNote);
        addNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NewJournalActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.diaryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DiaryAdapter(requireContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Now it's safe to observe LiveData
        DiaryDatabase.getInstance(requireContext())
                .diaryDao()
                .getAllEntries()
                .observe(getViewLifecycleOwner(), entries -> {
                    adapter.setEntries(entries);
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            DiaryDatabase.getInstance(requireContext())
                    .diaryDao()
                    .getAllEntries()
                    .observe(getViewLifecycleOwner(), entries -> {
                        adapter.setEntries(entries);
                    });
        }
    }
}

