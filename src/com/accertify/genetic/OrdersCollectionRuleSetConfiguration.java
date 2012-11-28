package com.accertify.genetic;

import com.accertify.genetic.model.OrderRow;
import com.accertify.genetic.model.OrdersCollection;
import com.accertify.genetic.model.RulesTripped;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/26/11
 * Time: 3:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class OrdersCollectionRuleSetConfiguration {
    private Map<Integer, Set<Integer>> ruleConfigs = new HashMap<Integer, Set<Integer>>(1000);

    public static final OrdersCollectionRuleSetConfiguration calculateInPlaceRuleSetConfiguration(OrdersCollection oc) {
        OrdersCollectionRuleSetConfiguration rc = new OrdersCollectionRuleSetConfiguration();
        for(OrderRow or: oc.getOrders()) {
            RulesTripped rt = or.rulesTripped;

            for(Integer ruleId: rt.getRuleIds()) {
                Integer score = rt.getScore(ruleId);

                if( !rc.ruleConfigs.containsKey(ruleId)) {
                    rc.ruleConfigs.put(ruleId, new TreeSet<Integer>());
                }
                Set<Integer> scores = rc.ruleConfigs.get(ruleId);

                scores.add(score);
            }
        }
        return rc;
    }

    public Map<Integer, Set<Integer>> getRuleConfigs() {
        return ruleConfigs;
    }

    public Map<Integer, Integer> getSampleScoreProfile() {
        Map<Integer, Integer> defaultScoreProfile = new HashMap<Integer, Integer>(ruleConfigs.size());

        for(Map.Entry<Integer, Set<Integer>> entry: ruleConfigs.entrySet()) {
            Integer ruleId = entry.getKey();
            Integer score = entry.getValue().iterator().next();

            defaultScoreProfile.put(ruleId, score);
        }

        return defaultScoreProfile;
    }
}
