package com.accertify.regression;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * stuff
 */
public class RuleStatsResult {
    protected static transient Log log = LogFactory.getLog(RuleStatsResult.class);


    public void addKnownRules(Iterable<Long> rid) {
        for(Long l: rid) {
            addKnownRule(l);
        }
    }

    public void addKnownRule(Long rid) {
        knownRules.add(rid);
        trippedFraudCount.put(rid, 0l);
        trippedFraudValue.put(rid, 0l);

        notTrippedFraudCount.put(rid, 0l);
        notTrippedFraudValue.put(rid, 0l);

        notTrippedNotFraudCount.put(rid, 0l);
        notTrippedNotFraudValue.put(rid, 0l);

        trippedNotFraudCount.put(rid, 0l);
        trippedNotFraudValue.put(rid, 0l);
    }

    public Set<Long> getKnownRules() {
        Set<Long> ss = new HashSet<Long>(knownRules.size());
        ss.addAll(knownRules);
        return ss;
    }

    Long fraudCount = 0l;
    Long notFraudCount = 0l;
    Long fraudValue = 0l;
    Long notFraudValue = 0l;

    private final Set<Long> knownRules = new HashSet<Long>();
    final Set<Long> trippedRules = new HashSet<Long>();

    final Map<Long, Long> trippedFraudCount = new HashMap<Long,Long>();
    final Map<Long, Long> trippedFraudValue = new HashMap<Long,Long>();

    final Map<Long, Long> notTrippedFraudCount = new HashMap<Long,Long>();
    final Map<Long, Long> notTrippedFraudValue = new HashMap<Long,Long>();

    final Map<Long, Long> notTrippedNotFraudCount = new HashMap<Long,Long>();
    final Map<Long, Long> notTrippedNotFraudValue = new HashMap<Long,Long>();

    final Map<Long, Long> trippedNotFraudCount = new HashMap<Long,Long>();
    final Map<Long, Long> trippedNotFraudValue = new HashMap<Long,Long>();
}
