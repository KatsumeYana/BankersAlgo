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
    }
    public Guest(int[] allocated, int[] maxNeed) {
        this.allocated = allocated;
        this.maxNeed = maxNeed;
    }
        public int[] getNeed() {
                return new int[] {
                    maxNeed[0] - allocated[0],
                    maxNeed[1] - allocated[1],
                    maxNeed[2] - allocated[2]
                };
            }
    // Getters
    public String getName() { return name; }
    public int getAllocatedRegular() { return allocatedRegular; }
    public int getAllocatedDeluxe() { return allocatedDeluxe; }
    public int getAllocatedStaff() { return allocatedStaff; }
    public int getMaxRegular() { return maxRegular; }
    public int getMaxDeluxe() { return maxDeluxe; }
    public int getMaxStaff() { return maxStaff; }
}

