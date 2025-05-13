/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bankersalgo;

/**
 *
 * @author Chill
 */
class Guest {
    int[] allocated; 
    int[] maxNeed;  

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
}
