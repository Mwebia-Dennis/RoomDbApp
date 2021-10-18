package com.penguinstech.roomdbapp.utils;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.penguinstech.roomdbapp.AddTaskActivity;
import com.penguinstech.roomdbapp.R;
import com.penguinstech.roomdbapp.room_db.AppDatabase;
import com.penguinstech.roomdbapp.room_db.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder>{


    private final List<Task> taskList;
    private final Context context;
    private final AppDatabase localDatabase;

    public NotesAdapter (Context context, List<Task> taskList) {
        this.taskList = taskList;
        this.context = context;
        this.localDatabase = Room.databaseBuilder(context,
                AppDatabase.class, Configs.DatabaseName).build();
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
        holder.editBtn.setOnClickListener(v->{

            Intent intent = new Intent(holder.itemView.getContext(), AddTaskActivity.class);
            intent.putExtra("id", task.id);
            intent.putExtra("title", task.title);
            intent.putExtra("description", task.description);
            intent.putExtra("isDeleted", task.isDeleted);
            intent.putExtra("updatedAt", task.updatedAt);
            context.startActivity(intent);

        });

        holder.deleteBtn.setOnClickListener(v->{
            deleteTask(task.id);
        });
    }


    @Override
    public int getItemCount() {
        return taskList.size();
    }


    private void deleteTask(int id) {
        new Thread(()->{
            localDatabase.taskDao().delete(id, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date()));
        }).start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTv, descriptionTv, dateTv;
        Button editBtn, deleteBtn;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.titleTV);
            descriptionTv = itemView.findViewById(R.id.descriptionTV);
            dateTv = itemView.findViewById(R.id.dateTV);
            editBtn = itemView.findViewById(R.id.editBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}
