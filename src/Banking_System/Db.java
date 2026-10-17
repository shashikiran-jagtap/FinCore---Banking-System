package Banking_System;

import java.sql.*;


public final class Db {
    private static final String URL  = env("BANK_DB_URL", "jdbc:mysql://localhost:3306/bank_db");
    private static final String USER = env("BANK_DB_USER", "root");
    private static final String PASS = env("BANK_DB_PASS", "root");

    private Db() {}

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null ? d : v;
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}


