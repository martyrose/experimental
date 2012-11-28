package com.accertify.genetic.comparator;

import com.accertify.genetic.model.OrderRow;
import com.accertify.genetic.model.RulesTripped;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/27/11
 * Time: 2:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class OrderRowComparator2 extends OrderRowComparator {
    protected static transient Log log = LogFactory.getLog(OrderRowComparator2.class);

    private Map<Integer, Integer> scoreMapping = null;

    public OrderRowComparator2(Map<Integer, Integer> scoreMapping) {
        this.scoreMapping = scoreMapping;
    }

    @Override
    public int calculateScore(OrderRow or) {
        return OrderRowScoreEvaluator.calculateScore(or, scoreMapping);
    }
}
