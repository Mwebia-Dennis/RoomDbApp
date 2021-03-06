package com.penguinstech.cloudy.room_db;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.penguinstech.cloudy.utils.Configs;

import java.util.List;

@Dao
public interface TokenDao {


    @Query("SELECT * FROM "+ Configs.tokensTableName+" WHERE id = :id")
    Token loadTokenById(int id);

    @Query("SELECT * FROM "+ Configs.tokensTableName+" WHERE table_name = :tableName  ORDER BY last_sync DESC LIMIT 1")
    Token loadLastSyncToken(String tableName);

    @Insert
    void insertAll(List<Token> tokenList);

    @Delete
    void delete(Token token);
}
