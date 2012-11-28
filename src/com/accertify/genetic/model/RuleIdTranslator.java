package com.accertify.genetic.model;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/12/11
 * Time: 1:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class RuleIdTranslator {
    protected static transient Log log = LogFactory.getLog(RuleIdTranslator.class);

    private final AtomicInteger sequence = new AtomicInteger(0);
    private final BiMap<Long, Integer> lookups;
    private final BiMap<Integer, Long> inverse;


    public RuleIdTranslator(int expectedRulez) {
        this.lookups = HashBiMap.create(expectedRulez);
        this.inverse = this.lookups.inverse();
    }

    public synchronized Integer getShortRuleId(Long ruleId) {
        if( lookups.containsKey(ruleId)) {
            return lookups.get(ruleId);
        }
        else {
            Integer newId = sequence.getAndIncrement();
            if (log.isDebugEnabled()) {
                log.debug("Handed {0} out to {1}", String.valueOf(newId), String.valueOf(ruleId));
            }
            lookups.put(ruleId, newId);
            return newId;
        }
    }
    public Long getLongRuleId(Integer ruleId) {
        return inverse.get(ruleId);
    }

    public Map<Long, Integer> toLongIds(Map<Integer, Integer> shortIds) {
        Map<Long, Integer> longIds = new HashMap<Long, Integer>(shortIds.size());

        for(Map.Entry<Integer, Integer> entry: shortIds.entrySet()) {
            longIds.put(getLongRuleId(entry.getKey()), entry.getValue());
        }
        return longIds;
    }

    public Map<Integer, Integer> toIntIds(final Map<Long, Integer> longIds) {
        Map<Integer, Integer> intIds = new HashMap<Integer, Integer>(longIds.size());

        for (Map.Entry<Long, Integer> entry : longIds.entrySet()) {
            intIds.put(getShortRuleId(entry.getKey()), entry.getValue());
        }
        return intIds;
    }

    public int size() {
        return lookups.size();
    }

    public Set<Long> getRuleKeys() {
        return lookups.keySet();
    }
}
