package com.accertify.art;

/**
 * QUERY USED TO GENERATE

 select
 transaction_id,
 to_char(import_ts, 'YYYY-MM-DD HH24:MI:SS.FF6'),
 scoring7_score as existing_score,
 numeric_0002 as model_score,
 transaction_amount,
 (select username from users where id=LAST_VIEWER_ID) as u,
 to_char(last_viewed_ts, 'YYYY-MM-DD HH24:MI:SS.FF6'),
 (select resolution_name from resolutions where id = RESOLUTION1_ID) as r,
 to_char(resolution1_ts, 'YYYY-MM-DD HH24:MI:SS.FF6'),
 resolution1_fraud,
 resolution1_missed,
 '',
 scoring7_rules_tripped
 from data_headers1
 where virtual_table_id=5237260000000001641 and
 import_ts between
 to_date('2014-03-07', 'YYYY-MM-DD') and
 to_date('2014-03-14', 'YYYY-MM-DD')
 union all
 select
 transaction_id,
 to_char(import_ts, 'YYYY-MM-DD HH24:MI:SS.FF6'),
 scoring7_score as existing_score,
 numeric_0002 as model_score,
 transaction_amount,
 (select username from users where id=LAST_VIEWER_ID) as u,
 to_char(last_viewed_ts, 'YYYY-MM-DD HH24:MI:SS.FF6'),
 (select resolution_name from resolutions where id = RESOLUTION1_ID) as r,
 to_char(resolution1_ts, 'YYYY-MM-DD HH24:MI:SS.FF6'),
 resolution1_fraud,
 resolution1_missed,
 '',
 scoring7_rules_tripped
 from data_headers1_hist
 where virtual_table_id=5237260000000001641 and
 import_ts between
 to_date('2014-03-07', 'YYYY-MM-DD') and
 to_date('2014-03-14', 'YYYY-MM-DD')

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
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.*;

/**
 Table we will use to store this

 create table art_model (
   tid text,
   import timestamp(1),
   escore int,
   mscore int,
   cost numeric(12,2),
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

    private static final String INSERT_SQL = "insert into art_model(tid, import, escore, mscore, cost, agent, agent_ts, r, r_ts, r_fraud, r_missed, tripped1, tripped2) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
    // 2014-02-22 16:25:46.588807
    private static final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    public static void main(String[] args) {

        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(INSERT_SQL);

            FileInputStream fis = new FileInputStream(new File("/home/mrose/art/marty.dat"));
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(fis, Charset.defaultCharset()));

            int counter=0;
            String line;
            while ((line = bufReader.readLine()) != null) {
                String[] parts = StringUtils.splitPreserveAllTokens(line, (char) 27);

                if (parts.length != 13) {
                    log.warn("Invalid line: " + line);
                    continue;
                }

                int idx=1;
                ps.setString(idx++, toString(parts[0])); // tid
                ps.setTimestamp(idx++, toTimestamp(parts[1])); // import
                ps.setInt(idx++, (int)Double.parseDouble(parts[2])); // escore
                if (StringUtils.isBlank(parts[3])) { // mscore
                    ps.setNull(idx++, Types.INTEGER);
                } else {
                    ps.setInt(idx++, (int)Double.parseDouble(parts[3]));
                }
                ps.setBigDecimal(idx++, new BigDecimal(parts[4])); // cost
                ps.setString(idx++, toString(parts[5])); // agent

                ps.setTimestamp(idx++, toTimestamp(parts[6])); // agent_ts
                ps.setString(idx++, toString(parts[7])); // resolution
                ps.setTimestamp(idx++, toTimestamp(parts[8])); // resolution_ts

                ps.setBoolean(idx++, "1".equals(parts[9]));// r_fraud
                ps.setBoolean(idx++, "1".equals(parts[10]));// r_missed

                ps.setString(idx++, toString(parts[11])); // tripped1
                ps.setString(idx++, toString(parts[12])); // tripped2
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

    private static Timestamp toTimestamp(String s) {
        return StringUtils.isBlank(s) ? null : new Timestamp(fmt.parseMillis(s));
    }

    private static String toString(String s) {
        return StringUtils.isBlank(s) ? null : s;
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
