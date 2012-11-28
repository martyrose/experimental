package com.accertify.genetic.comparator;

import com.accertify.genetic.model.OrderRow;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/12/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class OrderRowComparator3 extends OrderRowComparator {
    private int[] scores = null;

    public OrderRowComparator3(int[] scores) {
        this.scores = scores;
    }

    @Override
    public int calculateScore(OrderRow or) {
        return OrderRowScoreEvaluator.calculateScore(or, scores);
    }
}
