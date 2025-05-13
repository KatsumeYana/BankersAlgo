package bankersalgo;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Juliana
 */

public enum GuestStatus {
    CHECK_IN("Check-In"),
    CHECK_OUT("Check-Out");
    private final String status;
    
    GuestStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }
}