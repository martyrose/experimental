package com.accertify.genetic;

import com.accertify.genetic.model.OrdersCollection;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/10/11
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class RulesManager {
    protected static transient Log log = LogFactory.getLog(RulesManager.class);

    // TODO move to common area
    public static Integer findMinRuleId(Map<Integer, Integer> scoreMapping) {
        SortedSet<Integer> ss = new TreeSet<Integer>(scoreMapping.keySet());
        return ss.first();
    }

    public static Integer findMaxRuleId(Map<Integer, Integer> scoreMapping) {
        SortedSet<Integer> ss = new TreeSet<Integer>(scoreMapping.keySet());
        return ss.last();
    }

    public static Map<Long,String> getRuleDescriptions(String rulesFile) {
        Map<Long, String> rulez = new HashMap<Long, String>();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(rulesFile);

            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

            String s = null;

            while( (s=br.readLine()) != null ) {
                Long id = Long.parseLong(StringUtils.substringBefore(s, "|"));
                String name = StringUtils.substringAfter(s, "|");

                rulez.put(id, name);
            }
        } catch( IOException ex ) {
            log.warn(ex.getMessage());
        } finally {
            IOUtils.closeQuietly(fis);
        }
        return rulez;
    }
}
