package com.penguinstech.roomdbapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder>{


    private final List<Task> taskList;
    private final Context context;

    public NotesAdapter (Context context, List<Task> taskList) {
        this.taskList = taskList;
        this.context = context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View myView = LayoutInflater.from(context).inflate(R.layout.note_layout, parent, false);
        return new ViewHolder(myView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleTv.setText(task.title);
        holder.descriptionTv.setText(task.description);
        holder.dateTv.setText(task.updatedAt);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTv, descriptionTv, dateTv;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.titleTV);
            descriptionTv = itemView.findViewById(R.id.descriptionTV);
            dateTv = itemView.findViewById(R.id.dateTV);
        }
    }
}
