package com.accertify.genetic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/26/11
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class OrdersCollection {
    private int expectedSize = 0;
    private int fraudCount = 0;
    private int nonFraudCount = 0;
    private long fraudValue = 0;
    private long nonFraudValue = 0;

    private final RuleIdTranslator ridTranslator;
    private final ArrayList<OrderRow> orders;

    public OrdersCollection(int expectedSize, RuleIdTranslator ridTranslator) {
        this.ridTranslator = ridTranslator;
        this.expectedSize = expectedSize;
        orders = new ArrayList<OrderRow>(expectedSize);
    }

    public void clear() {
        orders.clear();
        orders.trimToSize();
    }

    public List<OrderRow> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public OrderRow[] getOrdersArray() {
        OrderRow[] arr = new OrderRow[orders.size()];
        orders.toArray(arr);
        return arr;
    }


    public boolean add(OrderRow or) {
        if(or.isFraud) {
            fraudCount++;
            fraudValue = fraudValue + or.monetary;
        } else {
            nonFraudCount++;
            nonFraudValue = nonFraudValue + or.monetary;
        }

        return orders.add(or);
    }

    public void replaceWithNearlySorted(OrderRow[] lor) {
        if( orders.size() != lor.length) {
            throw new RuntimeException("Not same size!!!");
        }
        orders.clear();
        orders.addAll(Arrays.asList(lor));
    }

    public int size() {
        return orders.size();
    }

    public int getFraudCount() {
        return fraudCount;
    }

    public int getNonFraudCount() {
        return nonFraudCount;
    }

    public int getExpectedSize() {
        return expectedSize;
    }

    public long getFraudValue() {
        return fraudValue;
    }

    public long getNonFraudValue() {
        return nonFraudValue;
    }

    public RuleIdTranslator getRuleIdTranslator() {
        return ridTranslator;
    }
}
