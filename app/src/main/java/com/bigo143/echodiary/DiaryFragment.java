package com.bigo143.echodiary;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class DiaryFragment extends Fragment {

    private DiaryAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.diaryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        DiaryAdapter adapter = new DiaryAdapter();
        recyclerView.setAdapter(adapter);

        DiaryDatabase.getInstance(requireContext())
                .diaryDao()
                .getAllEntries()
                .observe(getViewLifecycleOwner(), entries -> {
                    adapter.setEntries(entries);
                });

        return view;
    }
}

