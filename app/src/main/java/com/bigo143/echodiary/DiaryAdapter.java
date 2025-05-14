package com.bigo143.echodiary;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.List;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    private List<DiaryEntry> entries;
    private Context context;
    private DiaryEntry currentEntry;

    public DiaryAdapter(Context context) {
        this.context = context;
    }

    public void setEntries(List<DiaryEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @Override
    public DiaryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diary_entry, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DiaryViewHolder holder, int position) {
        DiaryEntry entry = entries.get(position);
        holder.title.setText(entry.title);
        holder.date.setText(DateFormat.getDateTimeInstance().format(entry.timestamp));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DiaryDetailActivity.class);
            intent.putExtra("title", entry.title);
            intent.putExtra("content", entry.content);
            intent.putExtra("timestamp", entry.timestamp);
            context.startActivity(intent);
        });


    }

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;

        DiaryViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.entryTitle);
            date = itemView.findViewById(R.id.entryDate);
        }
    }

}
