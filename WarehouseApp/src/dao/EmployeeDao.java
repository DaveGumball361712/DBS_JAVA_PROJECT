package dao;

import model.Cashier;
import model.Employee;
import model.Manager;
import model.WarehouseStaff;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO extends BaseDAO<Employee, Integer> {

    private static final String BASE_SELECT =
            "SELECT e.EMPLOYEE_ID, e.USERNAME, e.FIRST_NAME, e.LAST_NAME, " +
            "       e.START_DATE, e.DATE_OF_BIRTH, e.ADDRESS, " +
            "       m.EMPLOYEE_ID AS MGR_ROLE_ID, m.BONUS, " +
            "       c.EMPLOYEE_ID AS CSH_ROLE_ID, c.MANAGER_ID AS CSH_MGR_ID, c.ASSIGNED_COUNTER, " +
            "       w.EMPLOYEE_ID AS WHS_ROLE_ID, w.MANAGER_ID AS WHS_MGR_ID, w.ASSIGNED_ZONE " +
            "FROM HR.EMPLOYEE e " +
            "LEFT JOIN HR.MANAGER m ON e.EMPLOYEE_ID = m.EMPLOYEE_ID " +
            "LEFT JOIN HR.CASHIER c ON e.EMPLOYEE_ID = c.EMPLOYEE_ID " +
            "LEFT JOIN HR.WAREHOUSE_STAFF w ON e.EMPLOYEE_ID = w.EMPLOYEE_ID ";

    public Employee findByUsername(String username) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.USERNAME = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Employee emp = map(rs);
                    emp.setPhoneNumbers(findPhonesByEmployeeId(conn, emp.getEmployeeId()));
                    return emp;
                }
            }
        }
        return null;
    }

    @Override
    protected Employee map(ResultSet rs) throws SQLException {
        int id = rs.getInt("EMPLOYEE_ID");
        String username = rs.getString("USERNAME");
        String firstName = rs.getString("FIRST_NAME");
        String lastName = rs.getString("LAST_NAME");
        LocalDate startDate = rs.getDate("START_DATE").toLocalDate();
        LocalDate dob = rs.getDate("DATE_OF_BIRTH").toLocalDate();
        String address = rs.getString("ADDRESS");

        if (rs.getObject("MGR_ROLE_ID") != null) {
            return new Manager(id, username, firstName, lastName, startDate, dob, address,
                    rs.getBigDecimal("BONUS"));
        } else if (rs.getObject("CSH_ROLE_ID") != null) {
            return new Cashier(id, username, firstName, lastName, startDate, dob, address,
                    rs.getInt("CSH_MGR_ID"), rs.getInt("ASSIGNED_COUNTER"));
        } else if (rs.getObject("WHS_ROLE_ID") != null) {
            return new WarehouseStaff(id, username, firstName, lastName, startDate, dob, address,
                    rs.getInt("WHS_MGR_ID"), rs.getString("ASSIGNED_ZONE"));
        }
        throw new SQLException("Nhân viên ID=" + id + " chưa được gán vào bảng vai trò (Vi phạm R4).");
    }

    private List<String> findPhonesByEmployeeId(Connection conn, int employeeId) throws SQLException {
        List<String> phones = new ArrayList<>();
        String sql = "SELECT PHONE_NUMBER FROM HR.EMPLOYEE_PHONE_NUMBER WHERE EMPLOYEE_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    phones.add(rs.getString("PHONE_NUMBER"));
                }
            }
        }
        return phones;
    }

    @Override
    public List<Employee> findAll() throws SQLException {
        List<Employee> list = new ArrayList<>();
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(BASE_SELECT);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Employee emp = map(rs);
                emp.setPhoneNumbers(findPhonesByEmployeeId(conn, emp.getEmployeeId()));
                list.add(emp);
            }
        }
        return list;
    }

    @Override
    public Employee findById(Integer id) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.EMPLOYEE_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Employee emp = map(rs);
                    emp.setPhoneNumbers(findPhonesByEmployeeId(conn, emp.getEmployeeId()));
                    return emp;
                }
            }
        }
        return null;
    }

    @Override
    public Integer insert(Employee obj) throws SQLException {
        String sqlEmp = "INSERT INTO HR.EMPLOYEE (USERNAME, FIRST_NAME, LAST_NAME, START_DATE, DATE_OF_BIRTH, ADDRESS) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sqlEmp, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, obj.getUsername());
                ps.setString(2, obj.getFirstName());
                ps.setString(3, obj.getLastName());
                ps.setDate(4, Date.valueOf(obj.getStartDate()));
                ps.setDate(5, Date.valueOf(obj.getDateOfBirth()));
                ps.setString(6, obj.getAddress());
                ps.executeUpdate();

                int newId;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newId = keys.getInt(1);
                        obj.setEmployeeId(newId);
                    } else {
                        throw new SQLException("Không lấy được EMPLOYEE_ID vừa sinh.");
                    }
                }

                // Chèn vào bảng vai trò tương ứng
                if (obj instanceof Manager m) {
                    try (PreparedStatement psRole = conn.prepareStatement(
                            "INSERT INTO HR.MANAGER (EMPLOYEE_ID, BONUS) VALUES (?, ?)")) {
                        psRole.setInt(1, newId);
                        psRole.setBigDecimal(2, m.getBonus());
                        psRole.executeUpdate();
                    }
                } else if (obj instanceof Cashier c) {
                    try (PreparedStatement psRole = conn.prepareStatement(
                            "INSERT INTO HR.CASHIER (EMPLOYEE_ID, MANAGER_ID, ASSIGNED_COUNTER) VALUES (?, ?, ?)")) {
                        psRole.setInt(1, newId);
                        psRole.setInt(2, c.getManagerId());
                        psRole.setInt(3, c.getAssignedCounter());
                        psRole.executeUpdate();
                    }
                } else if (obj instanceof WarehouseStaff w) {
                    try (PreparedStatement psRole = conn.prepareStatement(
                            "INSERT INTO HR.WAREHOUSE_STAFF (EMPLOYEE_ID, MANAGER_ID, ASSIGNED_ZONE) VALUES (?, ?, ?)")) {
                        psRole.setInt(1, newId);
                        psRole.setInt(2, w.getManagerId());
                        psRole.setString(3, w.getAssignedZone());
                        psRole.executeUpdate();
                    }
                }

                conn.commit();
                return newId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean update(Employee obj) throws SQLException {
        String sql = "UPDATE HR.EMPLOYEE SET FIRST_NAME = ?, LAST_NAME = ?, START_DATE = ?, " +
                    "DATE_OF_BIRTH = ?, ADDRESS = ? WHERE EMPLOYEE_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, obj.getFirstName());
            ps.setString(2, obj.getLastName());
            ps.setDate(3, Date.valueOf(obj.getStartDate()));
            ps.setDate(4, Date.valueOf(obj.getDateOfBirth()));
            ps.setString(5, obj.getAddress());
            ps.setInt(6, obj.getEmployeeId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM HR.EMPLOYEE WHERE EMPLOYEE_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}