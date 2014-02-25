package com.accertify.art;

/**
 * QUERY USED TO GENERATE
 *
 select transaction_id, import_ts, scoring7_score as existing_score, numeric_0002 as model_score, (select username from users where id=LAST_VIEWER_ID) as u, (select resolution_name from resolutions where id = RESOLUTION1_ID) as r, resolution1_fraud
 from data_headers1 dh
 where virtual_table_id = 5237260000000001641 and
 import_ts between to_date('2014-02-22', 'YYYY-MM-DD') and to_date('2014-02-24', 'YYYY-MM-DD')
 and numeric_0002 is not null
 and trim(text_0439) is not null

 union all

 select transaction_id, import_ts, scoring7_score as existing_score, numeric_0002 as model_score, (select username from users where id=LAST_VIEWER_ID) as u, (select resolution_name from resolutions where id = RESOLUTION1_ID) as r, resolution1_fraud
 from data_headers1_hist dh
 where virtual_table_id = 5237260000000001641 and
 import_ts between to_date('2014-02-22', 'YYYY-MM-DD') and to_date('2014-02-24', 'YYYY-MM-DD')
 and numeric_0002 is not null
 and trim(text_0439) is not null
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
   resolution text,
   fraud boolean,
   tripped1 text,
   tripped2 text
 );
 */
public class LoadModelRawData {
    private static final Logger log = LoggerFactory.getLogger(LoadModelRawData.class);

    private static final String INSERT_SQL = "insert into art_model(tid, import, escore, mscore, agent, resolution, fraud, tripped1, tripped2) values(?,?,?,?,?,?,?,?,?)";

    public static void main(String[] args) {
        // 2014-02-22 16:25:46.588807
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(INSERT_SQL);

            FileInputStream fis = new FileInputStream(new File("/home/mrose/art.data.dat"));
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(fis, Charset.defaultCharset()));

            String line;
            while ((line = bufReader.readLine()) != null) {
                String[] parts = StringUtils.splitPreserveAllTokens(line, (char) 27);

                if (parts.length != 9) {
                    log.warn("Invalid line: " + line);
                    continue;
                }

                ps.setString(1, parts[0]);
                ps.setTimestamp(2, new Timestamp(fmt.parseMillis(parts[1])));
                ps.setInt(3, (int) Double.parseDouble(parts[2]));
                ps.setInt(4, (int) Double.parseDouble(parts[3]));
                ps.setString(5, StringUtils.isBlank(parts[4]) ? null : parts[4]);
                ps.setString(6, StringUtils.isBlank(parts[5]) ? null : parts[5]);
                if (StringUtils.isBlank(parts[6])) {
                    ps.setNull(7, Types.BOOLEAN);
                } else {
                    ps.setBoolean(7, "1".equals(parts[6]));
                }
                ps.setString(8, StringUtils.isBlank(parts[7]) ? null : parts[7]);
                ps.setString(9, StringUtils.isBlank(parts[8]) ? null : parts[8]);
                ps.executeUpdate();
            }
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
