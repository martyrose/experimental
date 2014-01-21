package com.accertify.dumb;

import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
public class DeleteHierarchyNode {
    private static final String JDBC_URL = "jdbc:oracle:thin:@10.12.17.123:1521:XE";
    private static final String JDBC_USER = "core";
    private static final String JDBC_PASS = "core";
//    private static final String JDBC_URL = "jdbc:oracle:thin:@corep.db.vip:44221/corep";
//    private static final String JDBC_USER = "core";
//    private static final String JDBC_PASS = "XXXXXX";

    private static Connection c;

    private static final String FIND_CONSTRAINTS =
            "SELECT UC.TABLE_NAME, UCC.COLUMN_NAME, UC.CONSTRAINT_NAME \n" +
                    "FROM USER_CONSTRAINTS UC, USER_CONS_COLUMNS UCC \n" +
                    "WHERE UC.CONSTRAINT_NAME = UCC.CONSTRAINT_NAME AND \n" +
                    "UC.R_CONSTRAINT_NAME = 'HIERARCHIES_PK' AND \n" +
                    "UC.TABLE_NAME != 'HIERARCHIES'";

    private static final NumberFormat nf = NumberFormat.getIntegerInstance();

    public static void main(String[] args) {
        try {
            c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
            c.setAutoCommit(false);

            Set<Constraint> constraints = constraints(c);
            log("Constraints: " + constraints.size());

            Set<Table> tables = tables(constraints, c);
            log("Tables: " + tables.size());


            Map<String, Table> tablesMap = new HashMap<>();
            for(Table t: tables) {
                tablesMap.put(t.table, t);
            }

            for (Constraint csr : constraints) {
                doFastCheck(csr);
            }

            SortedSet<Table> bySize = new TreeSet<>(tables);
            for(Table t: bySize) {
                log("\tTable: " + t.table + " Size: " + nf.format(t.estRows));
            }
            for(Constraint csr: constraints ) {
                if( !csr.fastCheckPassed ) {
                    Table t = tablesMap.get(csr.table);
                    log("\tConstraint: " + csr.constraint + " Table: " + csr.table + " Column: " + csr.column + " Rows: " + nf.format(t.estRows));
                } else {
                    // log("Fast Check Passed: " + csr.constraint);
                }
            }
        } catch (Throwable t) {
            log(t);
        }
    }

    private static Connection getConnection() throws SQLException {
        c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        c.setAutoCommit(false);
        return c;
    }

    private static Set<Constraint> constraints(Connection c) throws SQLException {
        Set<Constraint> results = new HashSet<>();

        try(Statement st = c.createStatement()) {
            try(ResultSet rs = st.executeQuery(FIND_CONSTRAINTS)) {
                while(rs.next()) {
                    Constraint cs = new Constraint(rs.getString(3), rs.getString(1), rs.getString(2));
                    results.add(cs);
                }
            }
        }
        return results;
    }

    private static Set<Table> tables(Set<Constraint> constraints, Connection c) throws SQLException {
        Set<Table> results = new HashSet<>();

        for(Constraint cs: constraints) {
            Table t = new Table(cs.table);
            results.add(t);
        }

        try (PreparedStatement ps = c.prepareStatement("select num_rows from user_tables where table_name=?")) {
            for (Table t : results) {
                ps.setString(1, t.table);
                try(ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    t.estRows = rs.getLong(1);
                }
            }
        }
        return results;
    }

    private static void doFastCheck(Constraint csr) throws SQLException {
        try(Connection c = getConnection()) {
            try(Statement st = c.createStatement()) {
                log("Checking: " + csr.constraint);
                st.setQueryTimeout(5);
                try(ResultSet rs = st.executeQuery("select 1 from " + csr.table + " where " + csr.column + " = 1234 and rownum = 1")) {
                    csr.fastCheckPassed = true;
                } catch( SQLException ex ) {
                    csr.fastCheckPassed = false;
                }
                log("DONE Checking: " + csr.constraint);
                flush();
            }
        }
    }

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");

    public static final void log(Object o) {
        log(String.valueOf(o));
    }

    public static final void log(String s) {
        System.out.println(sdf.format(System.currentTimeMillis()) + " (" + formatDurationHMS(System.currentTimeMillis() - init) + ") --- " + s);
    }

    public static final void log(Throwable e) {
        System.out.print(sdf.format(System.currentTimeMillis()) + ": ex : ");
        e.printStackTrace(System.err);
    }

    public static final void flush() {
        System.out.flush();
    }

    public static final String lpad(long l, int pad, char c) {
        String s = "" + l;
        while (s.length() < pad) {
            s = c + s;
        }
        return s;
    }

    public static final long init = System.currentTimeMillis();
    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    public static final String formatDurationHMS(long l) {
        long balance = l;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long millis = 0;
        if (balance / MILLIS_PER_HOUR > 0) {
            hours = balance / MILLIS_PER_HOUR;
            balance = balance - hours * MILLIS_PER_HOUR;
        }

        if (balance / MILLIS_PER_MINUTE > 0) {
            minutes = balance / MILLIS_PER_MINUTE;
            balance = balance - minutes * MILLIS_PER_MINUTE;
        }

        if (balance / MILLIS_PER_SECOND > 0) {
            seconds = balance / MILLIS_PER_SECOND;
            balance = balance - seconds * MILLIS_PER_SECOND;
        }

        millis = balance;

        return lpad(hours, 2, '0') + ':' + lpad(minutes, 2, '0') + ':' + lpad(seconds, 2, '0') + '.' + lpad(millis, 3, '0');
    }

    static class Constraint {
        final String constraint;
        final String table;
        final String column;
        Boolean fastCheckPassed;

        Constraint(String constraint, String table, String column) {
            this.constraint = constraint;
            this.table = table;
            this.column = column;
        }
        @Override
        public boolean equals(Object o) {
            return this.constraint.equals(((Constraint)o).constraint);
        }

        @Override
        public int hashCode() {
            return constraint.hashCode();
        }
    }

    static class Table implements Comparable<Table> {
        final String table;
        Long estRows;

        Table(String table) {
            this.table = table;
        }

        @Override
        public boolean equals(Object o) {
            return this.table.equals(((Table)o).table);
        }

        @Override
        public int hashCode() {
            return table.hashCode();
        }

        @Override
        public int compareTo(Table t) {
            return new Long(estRows).compareTo(t.estRows);
        }
    }
}
