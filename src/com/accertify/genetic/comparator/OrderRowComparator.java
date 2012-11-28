package com.accertify.genetic.comparator;

import com.accertify.genetic.model.OrderRow;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/27/11
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class OrderRowComparator implements Comparator<OrderRow> {
    private int comparisonsMade = 0;

    public final int compare(OrderRow r1, OrderRow r2) {
        comparisonsMade++;
        int score1 = calculateScore(r1);
        int score2 = calculateScore(r2);

        if( score1 != score2 ) {
            return score1 - score2;
        }
        // So if the scores are the same, we still want to have a stable fully defined sort, so sort it by their transaction id
        return r1.tid.compareTo(r2.tid);
    }

    public abstract int calculateScore(OrderRow or);
    public final int getComparisonsMade() {
        return comparisonsMade;
    }
}
