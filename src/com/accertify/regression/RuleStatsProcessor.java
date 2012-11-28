package com.accertify.regression;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.accertify.util.SimpleHashSet;
import com.google.common.base.Splitter;
import com.google.common.io.LineProcessor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * TODO This parsing logic needs to be in a common place
 */
public class RuleStatsProcessor implements LineProcessor<RuleStatsResult> {
    protected static transient Log log = LogFactory.getLog(RuleStatsProcessor.class);

    private final RuleStatsResult results;
    private final Splitter lineSplitter = Splitter.on('|').trimResults();

    public RuleStatsProcessor(Set<Long> rulesToTrack) {
        results = new RuleStatsResult();
        results.addKnownRules(rulesToTrack);
    }

    @Override
    public RuleStatsResult getResult() {
        return results;
    }

    @Override
    public boolean processLine(String line) {
        if(StringUtils.isBlank(line)) {
            return true;
        }
        Iterable<String> it = lineSplitter.split(line);

        Iterator<String> iter = it.iterator();
        Long id = Long.parseLong(iter.next());
        Integer score = Integer.parseInt(iter.next());
        Long monetary = new BigDecimal(iter.next()).longValue();
        String tid = iter.next();

        Set<Long> tripped = parse(iter.next() + ";" + iter.next() + ";" + iter.next());
        Boolean isFraud = StringUtils.equals(iter.next(), "1") ? Boolean.TRUE : Boolean.FALSE;

        if( Math.abs(score) > 100000 ) {
            log.warn("Score is " + score + " ?!?!?!? tid=" + tid);
        }
        if( Math.abs(monetary) > 100000 ) {
            log.warn("Monetary is " + score + " ?!?!?!? tid=" + tid);
        }

        for (Long rid : tripped) {
            results.trippedRules.add(rid);
        }
        if( isFraud ) {
            // log.warn("id=" + id + ";score="+score + ";monetary=" + monetary + "tid=" + tid + ";isFraud=" + isFraud);
            results.fraudCount++;
            results.fraudValue += monetary;

            for(Long rid:tripped) {
                putAddIfAbsent(results.trippedFraudCount, rid, 1l);
                putAddIfAbsent(results.trippedFraudValue, rid, monetary);
            }

            for(Long rid:results.getKnownRules()) {
                if(!tripped.contains(rid) ) {
                    putAddIfAbsent(results.notTrippedFraudCount, rid, 1l);
                    putAddIfAbsent(results.notTrippedFraudValue, rid, monetary);
                }
            }
        } else {
            results.notFraudCount++;
            results.notFraudValue += monetary;

            for(Long rid:tripped) {
                putAddIfAbsent(results.trippedNotFraudCount, rid, 1l);
                putAddIfAbsent(results.trippedNotFraudValue, rid, monetary);
            }

            for(Long rid:results.getKnownRules()) {
                if(!tripped.contains(rid) ) {
                    putAddIfAbsent(results.notTrippedNotFraudCount, rid, 1l);
                    putAddIfAbsent(results.notTrippedNotFraudValue, rid, monetary);
                }
            }
        }

        return true;
    }

    private void putAddIfAbsent(Map<Long, Long> map, Long key, Long valueAdd) {
        if( map.containsKey(key) ) {
            Long value = map.get(key);
            if( value == null ) {
                value = 0l;
            }
            map.put(key, value + valueAdd);
        } else {
            map.put(key, valueAdd);
        }
    }

    private Set<Long> parse(String s) {
        Set<String> tripped = new SimpleHashSet<String>(StringUtils.split(s, "; "));
        Set<Long> r = new HashSet<Long>(tripped.size());
        for(String x:tripped) {
            r.add(Long.parseLong(StringUtils.substringBefore(x, ":")));
        }
        return r;
    }
}
