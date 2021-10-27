package com.penguinstech.cloudy.controller;

public interface MainController {

    void syncAllDataFromFirestore();
    void saveDataToFirestore( boolean isAllData, String updatedAt);
    void compareRoomToFirestoreData(String tokenLastSync);
    void compareFirestoreToRoomData(String tokenLastSync);
    void updateServer(Object item, String docId);

}
