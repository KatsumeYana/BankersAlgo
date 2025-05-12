package bankersalgo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class BankersAlgorithm {
    
    private Connection conn;
    
    public BankersAlgorithm() {
        conn = DatabaseConnection.getConnection();
    }
    
    // Banker's Algorithm implementation
    public boolean isSafeAllocation(int requestedRegular, int requestedDeluxe, int requestedStaff) {
        try {
            // Get current allocation and max needs
            Statement stmt = conn.createStatement();
            
            // Get available resources
            ResultSet rs = stmt.executeQuery("SELECT * FROM resource_allocation");
            int availableRegular = 0, availableDeluxe = 0, availableStaff = 0;
            
            while (rs.next()) {
                String type = rs.getString("resource_type");
                if (type.equals("regular_suite")) {
                    availableRegular = rs.getInt("available");
                } else if (type.equals("deluxe_suite")) {
                    availableDeluxe = rs.getInt("available");
                } else if (type.equals("house_staff")) {
                    availableStaff = rs.getInt("available");
                }
            }
            
            // Check if requested resources exceed available
            if (requestedRegular > availableRegular || 
                requestedDeluxe > availableDeluxe || 
                requestedStaff > availableStaff) {
                return false;
            }
            
            // Get all guests and their allocations
            rs = stmt.executeQuery("SELECT * FROM guests WHERE check_out_time IS NULL");
            List<int[]> allocations = new ArrayList<>();
            List<int[]> maxNeeds = new ArrayList<>();
            
            while (rs.next()) {
                int allocRegular = rs.getInt("allocated_regular");
                int allocDeluxe = rs.getInt("allocated_deluxe");
                int allocStaff = rs.getInt("allocated_staff");
                int maxRegular = rs.getInt("max_regular");
                int maxDeluxe = rs.getInt("max_deluxe");
                int maxStaff = rs.getInt("max_staff");
                
                allocations.add(new int[]{allocRegular, allocDeluxe, allocStaff});
                maxNeeds.add(new int[]{maxRegular - allocRegular, maxDeluxe - allocDeluxe, maxStaff - allocStaff});
            }
            
            // Simulate allocation to check safety
            availableRegular -= requestedRegular;
            availableDeluxe -= requestedDeluxe;
            availableStaff -= requestedStaff;
            
            // Add the new process to the lists
            allocations.add(new int[]{requestedRegular, requestedDeluxe, requestedStaff});
            maxNeeds.add(new int[]{0, 0, 0}); // Assuming the new process won't request more
            
            // Initialize work array
            int[] work = {availableRegular, availableDeluxe, availableStaff};
            
            // Initialize finish array
            boolean[] finish = new boolean[allocations.size()];
            for (int i = 0; i < finish.length; i++) {
                finish[i] = false;
            }
            
            // Safety algorithm
            boolean found;
            do {
                found = false;
                for (int i = 0; i < allocations.size(); i++) {
                    if (!finish[i] && 
                        maxNeeds.get(i)[0] <= work[0] && 
                        maxNeeds.get(i)[1] <= work[1] && 
                        maxNeeds.get(i)[2] <= work[2]) {
                        
                        // Release resources
                        work[0] += allocations.get(i)[0];
                        work[1] += allocations.get(i)[1];
                        work[2] += allocations.get(i)[2];
                        finish[i] = true;
                        found = true;
                    }
                }
            } while (found);
            
            // Check if all processes finished
            for (boolean f : finish) {
                if (!f) {
                    return false;
                }
            }
            
            return true;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void handleBankerCheckIn(String guestName, int qtyRegular, int qtyDeluxe, 
                                  int maxRegular, int maxDeluxe, int maxStaff) {
        // Verify safety using Banker's algorithm
        if (!isSafeAllocation(qtyRegular, qtyDeluxe, qtyRegular + qtyDeluxe)) {
            JOptionPane.showMessageDialog(null, 
                "✗ Unsafe allocation - would lead to potential deadlock\n" +
                "Please reduce your resource requests or try again later",
                "Unsafe Allocation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Insert into guests table
            String insertSQL = "INSERT INTO guests (guest_name, allocated_regular, allocated_deluxe, allocated_staff, "
                    + "max_regular, max_deluxe, max_staff, check_in_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, guestName);
            pstmt.setInt(2, qtyRegular);
            pstmt.setInt(3, qtyDeluxe);
            pstmt.setInt(4, qtyRegular + qtyDeluxe);
            pstmt.setInt(5, maxRegular);
            pstmt.setInt(6, maxDeluxe);
            pstmt.setInt(7, maxStaff);
            pstmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            pstmt.executeUpdate();

            // Update resource allocation
            updateResourceAllocation(qtyRegular, qtyDeluxe, qtyRegular + qtyDeluxe);

            // Show confirmation
            JOptionPane.showMessageDialog(null,
                "✓ Safe allocation confirmed for " + guestName + "\n" +
                "Regular Rooms: " + qtyRegular + "\n" +
                "Deluxe Rooms: " + qtyDeluxe + "\n" +
                "Total Staff Needed: " + (qtyRegular + qtyDeluxe),
                "Allocation Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error during check-in: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateResourceAllocation(int qtyRegular, int qtyDeluxe, int qtyStaff) throws SQLException {
        // Update regular suites
        PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? " +
            "WHERE resource_type = 'regular_suite'");
        pstmt.setInt(1, qtyRegular);
        pstmt.setInt(2, qtyRegular);
        pstmt.executeUpdate();
        
        // Update deluxe suites
        pstmt = conn.prepareStatement(
            "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? " +
            "WHERE resource_type = 'deluxe_suite'");
        pstmt.setInt(1, qtyDeluxe);
        pstmt.setInt(2, qtyDeluxe);
        pstmt.executeUpdate();
        
        // Update staff
        pstmt = conn.prepareStatement(
            "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? " +
            "WHERE resource_type = 'house_staff'");
        pstmt.setInt(1, qtyStaff);
        pstmt.setInt(2, qtyStaff);
        pstmt.executeUpdate();
        
        // Update status based on availability
        updateResourceStatus();
    }
    
    private void updateResourceStatus() throws SQLException {
        // Update regular suites status
        PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE resource_allocation SET status = CASE " +
            "WHEN available > 5 THEN 'safe' " +
            "WHEN available > 0 THEN 'warning' " +
            "ELSE 'danger' END " +
            "WHERE resource_type = 'regular_suite'");
        pstmt.executeUpdate();
        
        // Update deluxe suites status
        pstmt = conn.prepareStatement(
            "UPDATE resource_allocation SET status = CASE " +
            "WHEN available > 3 THEN 'safe' " +
            "WHEN available > 0 THEN 'warning' " +
            "ELSE 'danger' END " +
            "WHERE resource_type = 'deluxe_suite'");
        pstmt.executeUpdate();
        
        // Update staff status
        pstmt = conn.prepareStatement(
            "UPDATE resource_allocation SET status = CASE " +
            "WHEN available > 5 THEN 'safe' " +
            "WHEN available > 0 THEN 'warning' " +
            "ELSE 'danger' END " +
            "WHERE resource_type = 'house_staff'");
        pstmt.executeUpdate();
    }
    
    public void updateResourceDisplay(JTable resourceTable) {
        try {
            DefaultTableModel model = (DefaultTableModel) resourceTable.getModel();
            model.setRowCount(0); // Clear existing data
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM resource_allocation");
            
            while (rs.next()) {
                String type = rs.getString("resource_type");
                int max = rs.getInt("max_capacity");
                int allocated = rs.getInt("allocated");
                int available = rs.getInt("available");
                String status = rs.getString("status");
                
                // Format the resource type for display
                String displayType = "";
                if (type.equals("regular_suite")) {
                    displayType = "Regular Suite";
                } else if (type.equals("deluxe_suite")) {
                    displayType = "Deluxe Suite";
                } else if (type.equals("house_staff")) {
                    displayType = "House Staff";
                }
                
                model.addRow(new Object[]{displayType, max, allocated, available, status});
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateGuestTable(JTable guestTable) {
        try {
            DefaultTableModel model = (DefaultTableModel) guestTable.getModel();
            model.setRowCount(0); // Clear existing data
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guests WHERE check_out_time IS NULL");
            
            while (rs.next()) {
                String name = rs.getString("guest_name");
                int allocRegular = rs.getInt("allocated_regular");
                int allocDeluxe = rs.getInt("allocated_deluxe");
                int allocStaff = rs.getInt("allocated_staff");
                int maxRegular = rs.getInt("max_regular");
                int maxDeluxe = rs.getInt("max_deluxe");
                int maxStaff = rs.getInt("max_staff");
                
                model.addRow(new Object[]{
                    name, 
                    allocRegular, 
                    allocDeluxe, 
                    allocStaff, 
                    maxRegular, 
                    maxDeluxe, 
                    maxStaff
                });
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Other methods for checkout, archived records, etc.
    // ...
}