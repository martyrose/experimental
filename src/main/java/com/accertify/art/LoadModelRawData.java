package com.accertify.art;

/**
 * QUERY USED TO GENERATE

 select transaction_id, import_ts, scoring7_score as existing_score, numeric_0002 as model_score, (select username from users where id=LAST_VIEWER_ID) as u, (select resolution_name from resolutions where id = RESOLUTION1_ID) as r, resolution1_fraud, text_0439, scoring7_rules_tripped
 from data_headers1 dh
 where virtual_table_id = 5237260000000001641 and
 import_ts between to_date('2014-02-22', 'YYYY-MM-DD') and to_date('2014-03-01', 'YYYY-MM-DD') and
 transaction_id like '14%'

 union all

 select  transaction_id, import_ts, scoring7_score as existing_score, numeric_0002 as model_score, (select username from users where id=LAST_VIEWER_ID) as u, (select resolution_name from resolutions where id = RESOLUTION1_ID) as r, resolution1_fraud, text_0439, scoring7_rules_tripped
 from data_headers1_hist dh
 where virtual_table_id = 5237260000000001641 and
 import_ts between to_date('2014-02-22', 'YYYY-MM-DD') and to_date('2014-03-01', 'YYYY-MM-DD') and
 transaction_id like '14%'
 */

import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.*;

/**
 Table we will use to store this

 create table art_model (
   tid text,
   import timestamp(1),
   escore int,
   mscore int,
   agent text,
   agent_ts timestamp(1),
   r text,
   r_ts timestamp(1),
   r_fraud boolean,
   r_missed boolean,
   tripped1 text,
   tripped2 text
 );
 */
public class LoadModelRawData {
    private static final Logger log = LoggerFactory.getLogger(LoadModelRawData.class);

    private static final String INSERT_SQL = "insert into art_model(tid, import, escore, mscore, agent, agent_ts, r, r_ts, r_fraud, r_missed, tripped1, tripped2) values(?,?,?,?,?,?,?,?,?,?,?,?)";

    public static void main(String[] args) {
        // 2014-02-22 16:25:46.588807
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(INSERT_SQL);

            FileInputStream fis = new FileInputStream(new File("/home/mrose/art/art.data.dat"));
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(fis, Charset.defaultCharset()));

            int counter=0;
            String line;
            while ((line = bufReader.readLine()) != null) {
                String[] parts = StringUtils.splitPreserveAllTokens(line, (char) 27);

                if (parts.length != 12) {
                    log.warn("Invalid line: " + line);
                    continue;
                }

                ps.setString(1, parts[0]); // tid
                ps.setTimestamp(2, new Timestamp(fmt.parseMillis(parts[1]))); // import
                ps.setInt(3, (int) Double.parseDouble(parts[2])); // escore
                if (StringUtils.isBlank(parts[3])) { // mscore
                    ps.setNull(4, Types.INTEGER);
                } else {
                    ps.setInt(4, (int) Double.parseDouble(parts[3]));
                }
                ps.setString(5, StringUtils.isBlank(parts[4]) ? null : parts[4]); // agent

                ps.setTimestamp(6, StringUtils.isBlank(parts[5]) ? null : new Timestamp(fmt.parseMillis(parts[5]))); // agent_ts
                ps.setString(7, StringUtils.isBlank(parts[6]) ? null : parts[6]); // resolution
                ps.setTimestamp(8, StringUtils.isBlank(parts[7]) ? null : new Timestamp(fmt.parseMillis(parts[7]))); // resolution_ts

                ps.setBoolean(9, "1".equals(parts[8]));// r_fraud
                ps.setBoolean(10, "1".equals(parts[9]));// r_missed

                ps.setString(11, StringUtils.isBlank(parts[10]) ? null : parts[10]); // tripped1
                ps.setString(12, StringUtils.isBlank(parts[11]) ? null : parts[11]); // tripped2
                ps.addBatch(); counter++;

                if( counter % 1000 == 0 ) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static Connection getConnection() {
        try {
            Connection c = DriverManager.getConnection("jdbc:postgresql://autod.pg01/art", "art", "art");
            c.setAutoCommit(false);
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("EVIL-1", e);
        }
    }
}
