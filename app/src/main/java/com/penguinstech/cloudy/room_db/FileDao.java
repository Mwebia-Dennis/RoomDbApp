package com.penguinstech.cloudy.room_db;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.penguinstech.cloudy.utils.Configs;

import java.util.List;

@Dao
public interface FileDao {

    @Query("SELECT file_id, device_id, local_path, firestore_path, updated_at, is_deleted  FROM "+ Configs.filesTableName)
    List<Files> getAll();


    @Query("SELECT * FROM "+ Configs.filesTableName+" where is_deleted != 1 order by updated_at desc")
    List<Files> getNonDeletedFiles();


    @Query("SELECT * FROM "+ Configs.filesTableName+" order by updated_at asc limit :limit offset :offset")
    List<Files> getAll(int limit, int offset);


    @Query("SELECT * FROM "+Configs.filesTableName+" order by updated_at desc limit 1")
    List<Files> getLatestFile();

    @Query("SELECT file_id, device_id, local_path, firestore_path, updated_at, is_deleted FROM "+Configs.filesTableName+" where :colName = :value")
    List<Files> filterByCol(String colName, String value);

    @Query("SELECT file_id, device_id, local_path, firestore_path, updated_at, is_deleted FROM "+Configs.filesTableName+
            " where updated_at >= :date order by updated_at asc limit :limit offset :offset")
    List<Files> filterByDate(String date, int limit, int offset);

    @Query("SELECT count(id) FROM "+Configs.filesTableName+" where updated_at > :date")
    int filterByDateCount(String date);


    @Query("SELECT count(id) FROM "+Configs.filesTableName)
    int getCount();


    @Query("SELECT * FROM "+Configs.filesTableName+" WHERE id = :id")
    Files loadFileById(int id);

    @Query("SELECT count(id) FROM "+Configs.filesTableName+" WHERE file_id = :fileId")
    int exists(String fileId);

    @Query("SELECT * FROM "+Configs.filesTableName+" order by updated_at asc limit 1")
    Files getFirstFile();

    @Insert
    void insertAll(List<Files> files);
    @Update
    void update(Files files);

    @Query("UPDATE "+Configs.filesTableName+" SET is_deleted = 1,updated_at = :updatedAt WHERE id = :fileId")
    void delete(int fileId, String updatedAt);
}
