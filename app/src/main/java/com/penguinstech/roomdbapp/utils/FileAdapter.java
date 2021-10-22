package com.penguinstech.roomdbapp.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.penguinstech.roomdbapp.R;
import com.penguinstech.roomdbapp.room_db.Files;

import java.io.File;
import java.util.List;

public class FileAdapter extends BaseAdapter {
    Context context;
    List<Files> listOfFiles;

    public FileAdapter(Context context, List<Files> listOfFiles) {
        this.context = context;
        this.listOfFiles = listOfFiles;
    }

    @Override
    public int getCount() {
        return listOfFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return listOfFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ImageHolder holder;
        if(convertView == null) {

            convertView = inflater.inflate(R.layout.image_container,parent, false);
            holder = new ImageHolder(convertView);
            convertView.setTag(holder);

        }else {

            holder = (ImageHolder) convertView.getTag();
        }

        Files files = listOfFiles.get(position);

        String deviceId= Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("imagePath", files.firestorePath);
        if(files.firestorePath.equals("") && files.deviceId.equals(deviceId)){
            String filePath = Util.getPath(context, Uri.parse(files.localPath));
            if(filePath != null) {
                Glide
                        .with(context)
                        .load(filePath)
                        .centerCrop()
//                            .placeholder(R.drawable.loading_spinner)
                        .into(holder.imageView);
            }
        }else {
            Glide
                    .with(context)
                    .load(Uri.parse(files.firestorePath))
                    .centerCrop()
//                            .placeholder(R.drawable.loading_spinner)
                    .into(holder.imageView);
        }

        return convertView;
    }

    public static class ImageHolder {

        ImageView imageView;
        public ImageHolder(View view) {
            imageView = view.findViewById(R.id.img);
        }
    }
}
