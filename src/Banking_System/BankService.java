package Banking_System;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BankService {

    public record Session(int customerId, String name, long accountId, String accountNo, boolean admin) {}
    public record Txn(Timestamp at, String type, BigDecimal amount, BigDecimal balanceAfter) {}
    public record AdminRow(long accountId, String name, String email, String accountNo, BigDecimal balance, String status) {}
    public record Monthly(String month, BigDecimal income, BigDecimal spent) {}

    /** Registers a customer and opens an account in one transaction. */
    public String register(String name, String email, String password) throws BankException, SQLException {
        if (name.isBlank() || !email.contains("@") || password.length() < 6)
            throw new BankException("Invalid name, email, or password (min 6 chars).");
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                String salt = PasswordUtil.newSalt();
                int custId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO customers(name,email,password_hash,salt) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, email.toLowerCase());
                    ps.setString(3, PasswordUtil.hash(password, salt));
                    ps.setString(4, salt);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); custId = k.getInt(1); }
                }
                String accNo = String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L));
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO accounts(account_no,customer_id) VALUES(?,?)")) {
                    ps.setString(1, accNo);
                    ps.setInt(2, custId);
                    ps.executeUpdate();
                }
                c.commit();
                return accNo;
            } catch (SQLIntegrityConstraintViolationException e) {
                c.rollback();
                throw new BankException("Email already registered.");
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public Optional<Session> login(String email, String password) throws SQLException {
        String sql = "SELECT c.id, c.name, c.password_hash, c.salt, a.id, a.account_no, c.role " +
                "FROM customers c JOIN accounts a ON a.customer_id = c.id WHERE c.email = ?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password, rs.getString(4), rs.getString(3)))
                    return Optional.of(new Session(rs.getInt(1), rs.getString(2), rs.getLong(5), rs.getString(6), "ADMIN".equals(rs.getString(7))));
            }
        }
        return Optional.empty();
    }

    public BigDecimal balance(long accountId) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT balance FROM accounts WHERE id=?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    public BigDecimal deposit(long accountId, BigDecimal amt) throws BankException, SQLException {
        validate(amt);
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                BigDecimal nb = apply(c, accountId, amt, "DEPOSIT", null);
                c.commit();
                return nb;
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    public BigDecimal withdraw(long accountId, BigDecimal amt) throws BankException, SQLException {
        validate(amt);
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                BigDecimal nb = apply(c, accountId, amt.negate(), "WITHDRAW", null);
                c.commit();
                return nb;
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    /** Atomic transfer. Rows are locked in id order to prevent deadlocks. */
    public BigDecimal transfer(long fromId, String toAccountNo, BigDecimal amt) throws BankException, SQLException {
        validate(amt);
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                long toId;
                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM accounts WHERE account_no=?")) {
                    ps.setString(1, toAccountNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new BankException("Destination account not found.");
                        toId = rs.getLong(1);
                    }
                }
                if (toId == fromId) throw new BankException("Cannot transfer to the same account.");

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM accounts WHERE id IN (?,?) ORDER BY id FOR UPDATE")) {
                    ps.setLong(1, fromId);
                    ps.setLong(2, toId);
                    ps.executeQuery().close();
                }
                BigDecimal nb = apply(c, fromId, amt.negate(), "TRANSFER_OUT", toId);
                apply(c, toId, amt, "TRANSFER_IN", fromId);
                c.commit();
                return nb;
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    public List<Txn> history(long accountId, int limit) throws SQLException {
        List<Txn> out = new ArrayList<>();
        String sql = "SELECT created_at,type,amount,balance_after FROM transactions " +
                "WHERE account_id=? ORDER BY id DESC LIMIT ?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new Txn(rs.getTimestamp(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
            }
        }
        return out;
    }

    // ---------- Admin & reporting ----------
    public List<AdminRow> allAccounts() throws SQLException {
        List<AdminRow> out = new ArrayList<>();
        String sql = "SELECT a.id, c.name, c.email, a.account_no, a.balance, a.status " +
                "FROM accounts a JOIN customers c ON c.id = a.customer_id ORDER BY a.id";
        try (Connection c = Db.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                out.add(new AdminRow(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getBigDecimal(5), rs.getString(6)));
        }
        return out;
    }

    public void setFrozen(long accountId, boolean frozen) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("UPDATE accounts SET status=? WHERE id=?")) {
            ps.setString(1, frozen ? "FROZEN" : "ACTIVE");
            ps.setLong(2, accountId);
            ps.executeUpdate();
        }
    }

    public List<Monthly> monthlySummary(long accountId, int months) throws SQLException {
        List<Monthly> out = new ArrayList<>();
        String sql = "SELECT DATE_FORMAT(created_at,'%Y-%m') m, " +
                "SUM(CASE WHEN type IN ('DEPOSIT','TRANSFER_IN') THEN amount ELSE 0 END), " +
                "SUM(CASE WHEN type IN ('WITHDRAW','TRANSFER_OUT') THEN amount ELSE 0 END) " +
                "FROM transactions WHERE account_id=? AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) " +
                "GROUP BY m ORDER BY m";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setInt(2, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Monthly(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
            }
        }
        return out;
    }

    /** Locks the account row, applies delta, writes the ledger entry. Caller commits. */
    private BigDecimal apply(Connection c, long accId, BigDecimal delta, String type, Long related)
            throws SQLException, BankException {
        BigDecimal bal;
        try (PreparedStatement ps = c.prepareStatement("SELECT balance, status FROM accounts WHERE id=? FOR UPDATE")) {
            ps.setLong(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                bal = rs.getBigDecimal(1);
                if ("FROZEN".equals(rs.getString(2))) throw new BankException("Account is frozen. Please contact the bank.");
            }
        }
        BigDecimal nb = bal.add(delta);
        if (nb.signum() < 0) throw new BankException("Insufficient funds. Balance: " + bal);
        try (PreparedStatement ps = c.prepareStatement("UPDATE accounts SET balance=? WHERE id=?")) {
            ps.setBigDecimal(1, nb);
            ps.setLong(2, accId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO transactions(account_id,type,amount,balance_after,related_account_id) VALUES(?,?,?,?,?)")) {
            ps.setLong(1, accId);
            ps.setString(2, type);
            ps.setBigDecimal(3, delta.abs());
            ps.setBigDecimal(4, nb);
            if (related == null) ps.setNull(5, Types.BIGINT); else ps.setLong(5, related);
            ps.executeUpdate();
        }
        return nb;
    }

    private void validate(BigDecimal amt) throws BankException {
        if (amt == null || amt.signum() <= 0 || amt.scale() > 2)
            throw new BankException("Amount must be positive with at most 2 decimals.");
    }
}
