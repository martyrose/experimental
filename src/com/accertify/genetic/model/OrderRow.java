package com.accertify.genetic.model;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/26/11
 * Time: 2:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class OrderRow {
    protected static transient Log log = LogFactory.getLog(OrderRow.class);

    public final long id;
    public final int score;
    public final int monetary;
    public final String tid;
    public final RulesTripped rulesTripped;
    public final boolean isFraud;

    private OrderRow(long id, int score, int monetary, String tid, RulesTripped rt, boolean isFraud) {
        this.id = id;
        this.score = score;
        this.monetary = monetary;
        this.tid = tid;
        this.rulesTripped = rt;
        this.isFraud = isFraud;
    }

    public boolean equals(Object o) {
        OrderRow or = (OrderRow)o;
        if( or == null ) {
            return false;
        }
        return id == or.id;
    }

    public int hashCode() {
        return (int)(id % Integer.MAX_VALUE);
    }

    public static final OrderRow populate(Splitter lineSplitter, Splitter ruleEntrySplitter, RuleIdTranslator ruleIdTranslator, String line) {
        Iterable<String> iterable = lineSplitter.split(line);

        Iterator<String> iter = iterable.iterator();
        long id = Long.parseLong(iter.next());
        int score = Integer.parseInt(iter.next());
        int monetary = new BigDecimal(iter.next()).intValue();
        String tid = iter.next();
        RulesTripped rulesTripped = new RulesTripped(ruleEntrySplitter, ruleIdTranslator);
        rulesTripped.add(iter.next());
        rulesTripped.add(iter.next());
        rulesTripped.add(iter.next());
        rulesTripped.freeze();

        boolean isFraud = StringUtils.equals(iter.next(), "1") ? Boolean.TRUE : Boolean.FALSE;
        return new OrderRow(id, score, monetary, tid, rulesTripped, isFraud);
    }
}
