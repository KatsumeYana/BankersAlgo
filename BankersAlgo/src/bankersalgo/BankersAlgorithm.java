package bankersalgo;

import java.sql.*;
import java.util.ArrayList;
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
            rs = stmt.executeQuery("SELECT * FROM guests WHERE check_out_time IS NULL");
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
    public void handleBankerCheckIn(String guestName, int qtyRegular, int qtyDeluxe,
            int maxRegular, int maxDeluxe, int maxStaff) {
        // Verify safety using Banker's algorithm
        if (!isSafeAllocation(qtyRegular, qtyDeluxe, qtyRegular + qtyDeluxe)) {
            JOptionPane.showMessageDialog(null,
                    "✗ Unsafe allocation - would lead to potential deadlock\n"
                    + "Please reduce your resource requests or try again later",
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
                    "✓ Safe allocation confirmed for " + guestName + "\n"
                    + "Regular Rooms: " + qtyRegular + "\n"
                    + "Deluxe Rooms: " + qtyDeluxe + "\n"
                    + "Total Staff Needed: " + (qtyRegular + qtyDeluxe),
                    "Allocation Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error during check-in: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Check Out for the Guests
    public void handleGuestCheckOut(String searchName) {
        try {
            String query = "SELECT * FROM guests WHERE guest_name LIKE ? AND check_out_time IS NULL";
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
            Timestamp checkInTime = rs.getTimestamp("check_in_time");

            // Show confirmation dialog with guest info
            StringBuilder guestInfo = new StringBuilder("✓ Guest Found:\n\n");
            guestInfo.append("Name: ").append(guestName).append("\n");
            guestInfo.append("Regular Suites: ").append(allocRegular).append("\n");
            guestInfo.append("Deluxe Suites: ").append(allocDeluxe).append("\n");
            guestInfo.append("House Staff Assigned: ").append(allocStaff).append("\n");
            guestInfo.append("Check-In Time: ").append(checkInTime).append("\n\n");
            guestInfo.append("Would you like to complete the check-out?");

            int confirm = JOptionPane.showConfirmDialog(null, guestInfo.toString(),
                    "Confirm Check-Out", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Perform the check-out
                String updateSQL = "UPDATE guests SET check_out_time = ? WHERE guest_name = ? AND check_out_time IS NULL";
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
    private void updateResourceAllocation(int qtyRegular, int qtyDeluxe, int qtyStaff) throws SQLException {
        // Update regular suites
        PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? "
                + "WHERE resource_type = 'regular_suite'");
        pstmt.setInt(1, qtyRegular);
        pstmt.setInt(2, qtyRegular);
        pstmt.executeUpdate();

        // Update deluxe suites
        pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? "
                + "WHERE resource_type = 'deluxe_suite'");
        pstmt.setInt(1, qtyDeluxe);
        pstmt.setInt(2, qtyDeluxe);
        pstmt.executeUpdate();

        // Update staff
        pstmt = conn.prepareStatement(
                "UPDATE resource_allocation SET allocated = allocated + ?, available = available - ? "
                + "WHERE resource_type = 'house_staff'");
        pstmt.setInt(1, qtyStaff);
        pstmt.setInt(2, qtyStaff);
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

    
    //Table 2
    public void updateNeedsRequestsTable(JTable table, int[] request, String requestingGuest) {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0); // Clear existing data

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guests WHERE check_out_time IS NULL");

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
                        status = "✗ Request > Need";
                    } else if (!reqWithinAvail) {
                        status = "✗ Request > Available";
                    } else {
                        // Simulate with Banker's Algorithm
                        boolean safe = isSafeAllocation(req[0], req[1], req[2]);
                        status = safe ? "✓ Safe" : "✗ Unsafe";
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
            case "regular_suite" -> available[0] = rs.getInt("available");
            case "deluxe_suite" -> available[1] = rs.getInt("available");
            case "house_staff" -> available[2] = rs.getInt("available");
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
}
