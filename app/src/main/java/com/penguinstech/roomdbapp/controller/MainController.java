package com.penguinstech.roomdbapp.controller;

import com.penguinstech.roomdbapp.room_db.Task;

public interface MainController {

    void syncAllDataFromFirestore();
    void saveDataToFirestore( boolean isAllData, String updatedAt);
    void compareRoomToFirestoreData(String tokenLastSync);
    void compareFirestoreToRoomData(String tokenLastSync);
    void updateServer(Object item, String docId);

}
