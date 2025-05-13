/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bankersalgo;

/**
 *
 * @author Chill
 */
class Resources {
    private int[] available;

    public Resources(int regular, int deluxe, int staff) {
        this.available = new int[]{regular, deluxe, staff};
    }

    public int[] getAvailable() {
        return available;
    }

    public void allocate(int[] request) {
        for (int i = 0; i < 3; i++) {
            available[i] -= request[i];
        }
    }
}

