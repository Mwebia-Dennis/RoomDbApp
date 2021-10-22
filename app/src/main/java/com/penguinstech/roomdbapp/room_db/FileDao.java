package com.penguinstech.roomdbapp.room_db;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.penguinstech.roomdbapp.utils.Configs;

import java.util.List;

@Dao
public interface FileDao {

    @Query("SELECT * FROM "+ Configs.filesTableName)
    List<Files> getAll();


    @Query("SELECT * FROM "+ Configs.filesTableName+" where is_deleted != 1 order by updated_at desc")
    List<Files> getNonDeletedFiles();


    @Query("SELECT * FROM "+ Configs.filesTableName+" order by updated_at asc limit :limit offset :offset")
    List<Files> getAll(int limit, int offset);


    @Query("SELECT * FROM "+Configs.filesTableName+" order by updated_at desc limit 1")
    List<Files> getLatestFile();

    @Query("SELECT * FROM "+Configs.filesTableName+" where :colName = :value")
    List<Files> filterByCol(String colName, String value);

    @Query("SELECT * FROM "+Configs.filesTableName+" where updated_at > :date order by updated_at asc limit :limit offset :offset")
    List<Files> filterByDate(String date, int limit, int offset);

    @Query("SELECT count(id) FROM "+Configs.filesTableName+" where updated_at > :date")
    int filterByDateCount(String date);


    @Query("SELECT count(id) FROM "+Configs.filesTableName)
    int getCount();


    @Query("SELECT count(id) FROM "+Configs.filesTableName+" WHERE id = :fileId")
    int exists(int fileId);

    @Query("SELECT * FROM "+Configs.filesTableName+" WHERE id = :fileId")
    Files loadFileById(int fileId);

    @Insert
    void insertAll(List<Files> files);
    @Update
    void update(Files files);

    @Query("UPDATE "+Configs.filesTableName+" SET is_deleted = 1,updated_at = :updatedAt WHERE id = :fileId")
    void delete(int fileId, String updatedAt);
}
