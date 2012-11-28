package com.accertify.genetic.model;

import com.accertify.genetic.OptimizerConstants;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/26/11
 * Time: 2:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class RulesTripped {
    protected static transient Log log = LogFactory.getLog(RulesTripped.class);

    private volatile Map<Integer, Integer> tripped = new HashMap<Integer, Integer>();
    private int[] ruleIds = null;
    private final Splitter ruleEntrySplitter;
    private final RuleIdTranslator ruleIdTranslator;

    public RulesTripped(Splitter s, RuleIdTranslator ruleIdTranslator) {
        ruleEntrySplitter = s;
        this.ruleIdTranslator = ruleIdTranslator;
    }

    public void add(String rulesTripped) {
        if(StringUtils.isBlank(rulesTripped)) {
            return;
        }
        for(String s: ruleEntrySplitter.split(rulesTripped)) {
            String[] pair = StringUtils.split(s, ':');
            long realRuleId = Long.parseLong(pair[0]);
            Integer score = Integer.parseInt(pair[1]);
            if( score > OptimizerConstants.MAX_RULE_SCORE ) {
                score = OptimizerConstants.MAX_RULE_SCORE;
            }
            if( score < OptimizerConstants.MIN_RULE_SCORE ) {
                score = OptimizerConstants.MIN_RULE_SCORE;
            }
            Integer fakeRuleId = ruleIdTranslator.getShortRuleId(realRuleId);
            tripped.put(fakeRuleId, score);
        }
    }

    public void freeze() {
        tripped = Collections.unmodifiableMap(tripped);
        ruleIds = new int[tripped.size()];
        int x = 0;
        for( Integer ruleId: tripped.keySet() ) {
            ruleIds[x++] = ruleId;
        }
    }

    public int[] getRuleIds() {
        return ruleIds;
    }

    public Integer getScore(Integer ruleId) {
        return tripped.get(ruleId);
    }

    public String toString() {
        return tripped.toString();
    }

    public static Map<Long, Integer> parse(String rulesTripped) {
        Map<Long, Integer> scores = new HashMap<Long, Integer>();
        String[] pairs = StringUtils.split(rulesTripped, ";");
        for(String pair:pairs) {
            if(StringUtils.isBlank(pair)) {
                continue;
            }
            String[] entry = StringUtils.split(pair, ":");
            if(StringUtils.isBlank(entry[0]) || StringUtils.isBlank(entry[1])) {
                continue;
            }
            Long rid = Long.parseLong(entry[0]);
            Integer score = Integer.parseInt(entry[1]);
            scores.put(rid, score);
        }
        return scores;
    }

    public static String format(Map<Long, Integer> scoreProfile) {
        StringBuilder sb = new StringBuilder();
        for(Long rid: scoreProfile.keySet()) {
            Integer score = scoreProfile.get(rid);
            sb.append(rid);
            sb.append(":");
            sb.append(score);
            sb.append(";");
        }
        return sb.toString();
    }
}
/*
5237260000000066461:-101;5237260000000025421:250;5237260000000024581:1000;5237260000000025662:200;5237260000000032381:-300;
*/
