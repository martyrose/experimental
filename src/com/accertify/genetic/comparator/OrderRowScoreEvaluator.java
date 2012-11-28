package com.accertify.genetic.comparator;

import com.accertify.genetic.model.OrderRow;
import com.accertify.genetic.model.RulesTripped;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 6/13/11
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public final class OrderRowScoreEvaluator {
    protected static transient Log log = LogFactory.getLog(OrderRowScoreEvaluator.class);

    private OrderRowScoreEvaluator() {
        ;
    }

    public static int calculateScore(OrderRow or, int[] scores) {
        int ruleIds[] = or.rulesTripped.getRuleIds();
        int score = 0;
        for(int x=0; x<ruleIds.length; x++) {
            int ruleScore = scores[ruleIds[x]];
            score = score + ruleScore;
        }
        return score;
    }

    public static int calculateScore(OrderRow or, Map<Integer, Integer> scoreMapping ) {
        RulesTripped rt = or.rulesTripped;
        int score = 0;
        for(Integer ruleId:rt.getRuleIds()) {
            Integer ruleScore = scoreMapping.get(ruleId);
            if( ruleScore == null ) {
                log.warn("No score for rule " + ruleId);
            } else {
                score = score + ruleScore;
            }
        }
        return score;
    }
}
