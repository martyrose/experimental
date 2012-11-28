
package com.accertify.genetic;

import com.accertify.genetic.comparator.OrderRowComparator;
import com.accertify.genetic.comparator.OrderRowComparator2;
import com.accertify.genetic.model.OrderRow;
import com.accertify.genetic.model.OrdersCollection;
import com.accertify.genetic.model.RuleIdTranslator;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.google.common.io.Files;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/26/11
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class OrdersManager {
    protected static transient Log log = LogFactory.getLog(OrdersManager.class);

    public static OrdersCollection getOrdersFromFile(String ordersLocation, RuleIdTranslator ridTranslator) {
        OrdersCollection oc = null;

        // TODO Better code structure/inheritance/design
        // TODO Do better than hardcoded values
        OrdersLineProcessor lp = new OrdersLineProcessor(500000, ridTranslator);
        try {
            log.warn("Reading Orders File: " + ordersLocation);
            Files.readLines(new File(ordersLocation), Charset.defaultCharset(), lp);

            oc = lp.getResult();

            log.warn("Linez: {0}", oc.size());
            log.warn("Fraud/Total: {0} {1}", oc.getFraudCount(), oc.size());
            log.warn("Fraud$: {0} {1}", oc.getFraudValue(), (oc.getNonFraudValue() + oc.getFraudValue()));

            OrdersCollectionRuleSetConfiguration rc = OrdersCollectionRuleSetConfiguration.calculateInPlaceRuleSetConfiguration(oc);
            log.warn("RuleConfigs: " + rc.getRuleConfigs().size());
            reSortAndStore(oc, rc.getSampleScoreProfile());
        } catch (IOException e) {
            log.info(e, e.getMessage());
        }
        return oc;
    }

    public static void reSortAndStore(OrdersCollection oc, Map<Integer, Integer> scoreMapping) {// Now build up a list of scores from what we found
        OrderRowComparator orc = new OrderRowComparator2(scoreMapping);
        OrderRow[] arr = oc.getOrdersArray();
        long i1 = System.currentTimeMillis();
        Arrays.sort(arr, Collections.reverseOrder(orc));
        long i2 = System.currentTimeMillis();

        oc.replaceWithNearlySorted(arr);

        log.debug("Sorting: " + DurationFormatUtils.formatDurationHMS(i2 - i1));
        log.debug("Comparisons: {0}", orc.getComparisonsMade());
    }

    public static void scoreHistorgram(OrdersCollection oc, Map<Integer, Integer> scoreMapping) {// Now build up a list of scores from what we found
        OrderRowComparator orc = new OrderRowComparator2(scoreMapping);
        OrderRow[] arr = oc.getOrdersArray();

        SortedMap<Integer, AtomicInteger> counters = new TreeMap<Integer, AtomicInteger>();
        int DIVISOR=100;

        for(OrderRow or: arr) {
            Integer score = orc.calculateScore(or);
            Integer bucket = (score/DIVISOR)*DIVISOR;
            if(!counters.containsKey(bucket)) {
                counters.put(bucket, new AtomicInteger(0));
            }
            counters.get(bucket).incrementAndGet();
        }

        log.warn("Score Histogram: " + counters);
    }
}
