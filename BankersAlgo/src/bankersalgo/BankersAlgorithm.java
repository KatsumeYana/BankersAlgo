package bankersalgo;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class BankersAlgorithm {

    private Connection conn;

    //Connect DB
    public BankersAlgorithm() {
        conn = DatabaseConnection.getConnection();
    }
    

    // Banker's Algorithm implementation
    public boolean isSafeAllocation(int requestedRegular, int requestedDeluxe, int requestedStaff) {

        try {
            Statement stmt = conn.createStatement();

            // 1. Get available resources
            ResultSet rs = stmt.executeQuery("SELECT * FROM resource_allocation");
            int[] available = new int[3];
            while (rs.next()) {
                switch (rs.getString("resource_type")) {
                    case "regular_suite" ->
                        available[0] = rs.getInt("available");
                    case "deluxe_suite" ->
                        available[1] = rs.getInt("available");
                    case "house_staff" ->
                        available[2] = rs.getInt("available");
                }
            }

            // Check basic availability
            if (requestedRegular > available[0]
                    || requestedDeluxe > available[1]
                    || requestedStaff > available[2]) {

                StringBuilder message = new StringBuilder("✗ Not enough available resources:\n");

                if (requestedRegular > available[0]) {
                    message.append("• Requested Regular Suites: ").append(requestedRegular)
                            .append(" > Available: ").append(available[0]).append("\n");
                }
                if (requestedDeluxe > available[1]) {
                    message.append("• Requested Deluxe Suites: ").append(requestedDeluxe)
                            .append(" > Available: ").append(available[1]).append("\n");
                }
                if (requestedStaff > available[2]) {
                    message.append("• Requested House Staff: ").append(requestedStaff)
                            .append(" > Available: ").append(available[2]).append("\n");
                }

                message.append("Please adjust your request and try again.");

                JOptionPane.showMessageDialog(null, message.toString(),
                        "Resource Request Too High", JOptionPane.WARNING_MESSAGE);

                return false;
            }

            Resources res = new Resources(available[0], available[1], available[2]);
            res.allocate(new int[]{requestedRegular, requestedDeluxe, requestedStaff});

            // 2. Get guest allocations and needs
            rs = stmt.executeQuery("SELECT * FROM guests WHERE status = 'Check-In'");
            List<Guest> guests = new ArrayList<>();
            while (rs.next()) {
                int[] alloc = {
                    rs.getInt("allocated_regular"),
                    rs.getInt("allocated_deluxe"),
                    rs.getInt("allocated_staff")
                };
                int[] max = {
                    rs.getInt("max_regular"),
                    rs.getInt("max_deluxe"),
                    rs.getInt("max_staff")
                };
                guests.add(new Guest(alloc, max));
            }

            // Add the new guest
            guests.add(new Guest(
                    new int[]{requestedRegular, requestedDeluxe, requestedStaff},
                    new int[]{requestedRegular, requestedDeluxe, requestedStaff} // assume no future requests
            ));

            return isSafeState(guests, res);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Check In of the Guests
    public boolean handleBankerCheckIn(String guestName, 
                                 int reqRegular, int reqDeluxe, 
                                 int maxRegular, int maxDeluxe, int maxStaff) {
    // Calculate required staff (1 staff per room)
    int reqStaff = reqRegular + reqDeluxe;
    
    // 1. Check basic availability first
    int[] available = getAvailableResources();
    if (available == null) {
        JOptionPane.showMessageDialog(null, 
            "Error checking resource availability. Please try again.",
            "Database Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }
    
    // Check if requested resources exceed available
    if (reqRegular > available[0] || reqDeluxe > available[1] || reqStaff > available[2]) {
        StringBuilder errorMsg = new StringBuilder("✗ Not enough available resources:\n");
        
        if (reqRegular > available[0]) {
            errorMsg.append("• Regular Suites: Requested ").append(reqRegular)
                   .append(", Available ").append(available[0]).append("\n");
        }
        if (reqDeluxe > available[1]) {
            errorMsg.append("• Deluxe Suites: Requested ").append(reqDeluxe)
                   .append(", Available ").append(available[1]).append("\n");
        }
        if (reqStaff > available[2]) {
            errorMsg.append("• House Staff: Requested ").append(reqStaff)
                   .append(", Available ").append(available[2]).append("\n");
        }
        
        errorMsg.append("\nPlease adjust your request.");
        
        JOptionPane.showMessageDialog(null, errorMsg.toString(),
            "Insufficient Resources", JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // 2. Check if allocation is safe using Banker's algorithm
    boolean isSafe = isSafeAllocation(guestName, reqRegular, reqDeluxe, maxRegular, maxDeluxe, maxStaff);
    
    if (!isSafe) {
        JOptionPane.showMessageDialog(null,
            "✗ This allocation would make the system unsafe.\n" +
            "Please reduce your resource requests.",
            "Unsafe Allocation", JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // 3. Show confirmation dialog with allocation details
    StringBuilder confirmMsg = new StringBuilder("✓ Allocation is safe!\n\n");
    confirmMsg.append("Guest: ").append(guestName).append("\n");
    confirmMsg.append("Allocating:\n");
    confirmMsg.append("• Regular Suites: ").append(reqRegular).append("\n");
    confirmMsg.append("• Deluxe Suites: ").append(reqDeluxe).append("\n");
    confirmMsg.append("• House Staff: ").append(reqStaff).append("\n\n");
    confirmMsg.append("Maximum Needs:\n");
    confirmMsg.append("• Regular Suites: ").append(maxRegular).append("\n");
    confirmMsg.append("• Deluxe Suites: ").append(maxDeluxe).append("\n");
    confirmMsg.append("• House Staff: ").append(maxStaff).append("\n\n");
    confirmMsg.append("Confirm check-in?");
    
    int confirm = JOptionPane.showConfirmDialog(null, confirmMsg.toString(),
        "Confirm Check-In", JOptionPane.YES_NO_OPTION);
    
    if (confirm != JOptionPane.YES_OPTION) {
        return false;
    }
    
    // 4. Insert into database
    try {
        // Start transaction
        conn.setAutoCommit(false);
        
        // Insert guest record
        String insertGuest = "INSERT INTO guests (guest_name, allocated_regular, allocated_deluxe, " +
                           "allocated_staff, max_regular, max_deluxe, max_staff, status) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, 'Check-In')";
        PreparedStatement pstmt = conn.prepareStatement(insertGuest);
        pstmt.setString(1, guestName);
        pstmt.setInt(2, reqRegular);
        pstmt.setInt(3, reqDeluxe);
        pstmt.setInt(4, reqStaff);
        pstmt.setInt(5, maxRegular);
        pstmt.setInt(6, maxDeluxe);
        pstmt.setInt(7, maxStaff);
        pstmt.executeUpdate();
        
        // Update resource allocations
        updateResourceAllocation(reqRegular, reqDeluxe, reqStaff);
        
        // Commit transaction
        conn.commit();
        
        JOptionPane.showMessageDialog(null,
            "✓ Check-in successful for " + guestName,
            "Check-In Complete", JOptionPane.INFORMATION_MESSAGE);
        
        return true;
        
    } catch (SQLException e) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        JOptionPane.showMessageDialog(null,
            "Error during check-in: " + e.getMessage(),
            "Database Error", JOptionPane.ERROR_MESSAGE);
        return false;
    } finally {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
    //Check Out for the Guests
    public void handleGuestCheckOut(String searchName) {
        try {
            String query = "SELECT * FROM guests WHERE guest_name LIKE ? AND status = 'Check-In'";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, "%" + searchName + "%");
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null,
                        "✗ No active reservation found for: " + searchName,
                        "Guest Not Found", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Retrieve guest data
            String guestName = rs.getString("guest_name");
            int allocRegular = rs.getInt("allocated_regular");
            int allocDeluxe = rs.getInt("allocated_deluxe");
            int allocStaff = rs.getInt("allocated_staff");

            // Show confirmation dialog with guest info
            StringBuilder guestInfo = new StringBuilder("✓ Guest Found:\n\n");
            guestInfo.append("Name: ").append(guestName).append("\n");
            guestInfo.append("Regular Suites: ").append(allocRegular).append("\n");
            guestInfo.append("Deluxe Suites: ").append(allocDeluxe).append("\n");
            guestInfo.append("House Staff Assigned: ").append(allocStaff).append("\n");
            guestInfo.append("Would you like to complete the check-out?");

            int confirm = JOptionPane.showConfirmDialog(null, guestInfo.toString(),
                    "Confirm Check-Out", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Perform the check-out
                String updateSQL = "UPDATE guests WHERE guest_name = ? AND status = 'Check-In'";
                PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                updateStmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                updateStmt.setString(2, guestName);
                updateStmt.executeUpdate();

                // Restore the resources
                updateResourceAllocation(-allocRegular, -allocDeluxe, -allocStaff);

                JOptionPane.showMessageDialog(null,
                        "✓ Check-out completed for guest: " + guestName,
                        "Check-Out Success", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error during check-out: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    



    //Updates data in Table 1
    public void updateResourceAllocation(int allocRegular, int allocDeluxe, int allocStaff) throws SQLException {
        // Update regular suites
        PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? "
                + "WHERE resource_type = 'regular_suite'");
        pstmt.setInt(1, allocRegular);
        pstmt.setInt(2, allocRegular);
        pstmt.executeUpdate();

        // Update deluxe suites
        pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? "
                + "WHERE resource_type = 'deluxe_suite'");
        pstmt.setInt(1, allocDeluxe);
        pstmt.setInt(2, allocDeluxe);
        pstmt.executeUpdate();

        // Update staff
        pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? "
                + "WHERE resource_type = 'house_staff'");
        pstmt.setInt(1, allocStaff);
        pstmt.setInt(2, allocStaff);
        pstmt.executeUpdate();

        // Update status based on availability
        updateResourceStatus();
    }

    //3rd Table, Resource Status
    private void updateResourceStatus() throws SQLException {
        // Update regular suites status
        PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET status = CASE "
                + "WHEN available > 5 THEN 'safe' "
                + "WHEN available > 0 THEN 'warning' "
                + "ELSE 'danger' END "
                + "WHERE resource_type = 'regular_suite'");
        pstmt.executeUpdate();

        // Update deluxe suites status
        pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET status = CASE "
                + "WHEN available > 3 THEN 'safe' "
                + "WHEN available > 0 THEN 'warning' "
                + "ELSE 'danger' END "
                + "WHERE resource_type = 'deluxe_suite'");
        pstmt.executeUpdate();

        // Update staff status
        pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET status = CASE "
                + "WHEN available > 5 THEN 'safe' "
                + "WHEN available > 0 THEN 'warning' "
                + "ELSE 'danger' END "
                + "WHERE resource_type = 'house_staff'");
        pstmt.executeUpdate();
    }

    //Table 3
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

    //Table 1
    public void updateGuestTable(JTable guestTable) {
        try {
            DefaultTableModel model = (DefaultTableModel) guestTable.getModel();
            model.setRowCount(0); // Clear existing data

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guests WHERE status = 'Check-in'");

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

    //Table 2
    public void updateNeedsRequestsTable(JTable table, int[] request, String requestingGuest) {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0); // Clear existing data

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guests WHERE status = 'Check-In'");

            int[] available = getCurrentAvailableResources();

            while (rs.next()) {
                String name = rs.getString("guest_name");
                int[] alloc = {
                    rs.getInt("allocated_regular"),
                    rs.getInt("allocated_deluxe"),
                    rs.getInt("allocated_staff")
                };
                int[] max = {
                    rs.getInt("max_regular"),
                    rs.getInt("max_deluxe"),
                    rs.getInt("max_staff")
                };

                int[] need = new int[3];
                for (int i = 0; i < 3; i++) {
                    need[i] = max[i] - alloc[i];
                }

                int[] req = {0, 0, 0};
                String status = "—";

                if (name.equalsIgnoreCase(requestingGuest)) {
                    req = request;

                    boolean reqWithinNeed = req[0] <= need[0] && req[1] <= need[1] && req[2] <= need[2];
                    boolean reqWithinAvail = req[0] <= available[0] && req[1] <= available[1] && req[2] <= available[2];

                    if (!reqWithinNeed) {
                        status = "Invalid";
                    } else if (!reqWithinAvail) {
                        status = "Pending";
                    } else {
                        boolean safe = isSafeAllocation(req[0], req[1], req[2]);
                        status = safe ? "Approved" : "Denied";
                    }

                }

                model.addRow(new Object[]{
                    name,
                    need[0], need[1], need[2],
                    req[0], req[1], req[2],
                    status
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Current Resources
    private int[] getCurrentAvailableResources() throws SQLException {
        int[] available = new int[3];
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM resource_allocation");
        while (rs.next()) {
            switch (rs.getString("resource_type")) {
                case "regular_suite" ->
                    available[0] = rs.getInt("available");
                case "deluxe_suite" ->
                    available[1] = rs.getInt("available");
                case "house_staff" ->
                    available[2] = rs.getInt("available");
            }
        }
        return available;
    }

    //Determines if the available resources are still safe when a guest requests
    private boolean isSafeState(List<Guest> guests, Resources resources) {
        int[] work = resources.getAvailable().clone();
        boolean[] finish = new boolean[guests.size()];
        int finishedCount = 0;

        while (finishedCount < guests.size()) {
            boolean progress = false;
            for (int i = 0; i < guests.size(); i++) {
                if (!finish[i]) {
                    int[] need = guests.get(i).getNeed();
                    if (need[0] <= work[0] && need[1] <= work[1] && need[2] <= work[2]) {
                        int[] alloc = guests.get(i).allocated;
                        for (int j = 0; j < 3; j++) {
                            work[j] += alloc[j];
                        }
                        finish[i] = true;
                        finishedCount++;
                        progress = true;
                    }
                }
            }

            if (!progress) {
                return false;
            }
        }

        return true;
    }
    
    public int[] getAvailableResources() {
    String query = "SELECT available FROM resource_allocation ORDER BY resource_type";
    int[] available = new int[3]; // regular, deluxe, staff
    
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query);
         ResultSet rs = stmt.executeQuery()) {
        
        if (rs.next()) available[0] = rs.getInt(1); // regular
        if (rs.next()) available[1] = rs.getInt(1); // deluxe
        if (rs.next()) available[2] = rs.getInt(1); // staff
        
        return available;
    } catch (SQLException e) {
        e.printStackTrace();
        return null;
    }
}

    public boolean isSafeAllocation(String guestName, 
                              int reqRegular, int reqDeluxe,
                              int maxRegular, int maxDeluxe, int maxStaff) {
    // Calculate staff required (1 staff per room)
    int reqStaff = reqRegular + reqDeluxe;
    
    // 1. Get current state from database
    int[] available = getAvailableResources();
    if (available == null) return false;
    
    // 2. Check if requested resources exceed available
    if (reqRegular > available[0] || reqDeluxe > available[1] || reqStaff > available[2]) {
        return false;
    }

    // 3. Temporarily allocate resources for safety check
    available[0] -= reqRegular;
    available[1] -= reqDeluxe;
    available[2] -= reqStaff;

    // 4. Get all current allocations and max needs
    List<Guest> guests = getAllGuests();
    
    // Add the new potential guest to the list
    guests.add(new Guest(
        new int[]{reqRegular, reqDeluxe, reqStaff},
        new int[]{maxRegular, maxDeluxe, maxStaff}
    ));

    // 5. Implement Banker's safety algorithm
    int[] work = Arrays.copyOf(available, available.length);
    boolean[] finish = new boolean[guests.size()];
    
    // Initialize finish array
    Arrays.fill(finish, false);
    
    // Find a guest that can finish with current work
    boolean found;
    do {
        found = false;
        for (int i = 0; i < guests.size(); i++) {
            if (!finish[i]) {
                Guest g = guests.get(i);
                int[] need = g.getNeed();
                
                if (need[0] <= work[0] && need[1] <= work[1] && need[2] <= work[2]) {
                    // This guest can finish - pretend to release their resources
                    work[0] += g.allocated[0];
                    work[1] += g.allocated[1];
                    work[2] += g.allocated[2];
                    finish[i] = true;
                    found = true;
                }
            }
        }
    } while (found);
    
    // If all guests can finish, the state is safe
    for (boolean f : finish) {
        if (!f) return false;
    }
    
    return true;
}

private List<Guest> getAllGuests() {
    List<Guest> guests = new ArrayList<>();
    String query = "SELECT guest_name, allocated_regular, allocated_deluxe, allocated_staff, " +
                  "max_regular, max_deluxe, max_staff FROM guests";
    
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query);
         ResultSet rs = stmt.executeQuery()) {
        
        while (rs.next()) {
            guests.add(new Guest(
                rs.getString("guest_name"),
                rs.getInt("allocated_regular"),
                rs.getInt("allocated_deluxe"),
                rs.getInt("allocated_staff"),
                rs.getInt("max_regular"),
                rs.getInt("max_deluxe"),
                rs.getInt("max_staff")
            ));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return guests;
}
}
