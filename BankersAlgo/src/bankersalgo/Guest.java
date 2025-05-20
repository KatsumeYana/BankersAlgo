/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bankersalgo;

/**
 *
 * @author Chill
*/

public class Guest {
    private String name;
    private int allocatedRegular;
    private int allocatedDeluxe;
    private int allocatedStaff;
    private int maxRegular;
    private int maxDeluxe;
    private int maxStaff;
    int[] allocated; 
    int[] maxNeed;  

    public Guest(String name, int allocatedRegular, int allocatedDeluxe, int allocatedStaff,
                int maxRegular, int maxDeluxe, int maxStaff) {
        this.name = name;
        this.allocatedRegular = allocatedRegular;
        this.allocatedDeluxe = allocatedDeluxe;
        this.allocatedStaff = allocatedStaff;
        this.maxRegular = maxRegular;
        this.maxDeluxe = maxDeluxe;
        this.maxStaff = maxStaff;
        this.allocated = new int[]{allocatedRegular, allocatedDeluxe, allocatedStaff};
        this.maxNeed = new int[]{maxRegular, maxDeluxe, maxStaff}; // Ensure this is initialized
    }
    public Guest(int[] allocated, int[] maxNeed) {
        this.allocated = allocated;
        this.maxNeed = maxNeed;
    }
        public int[] getNeed() {
        int[] need = new int[maxNeed.length];
        for (int i = 0; i < maxNeed.length; i++) {
            need[i] = maxNeed[i] - allocated[i];
        }
        return need;
    }
    
}
