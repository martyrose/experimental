package com.accertify.genetic;

import com.accertify.genetic.model.OrderRow;
import com.accertify.genetic.model.OrdersCollection;
import com.accertify.genetic.model.RuleIdTranslator;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.google.common.base.Splitter;
import com.google.common.io.LineProcessor;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 4/26/11
 * Time: 2:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class OrdersLineProcessor implements LineProcessor<OrdersCollection> {
    protected static transient Log log = LogFactory.getLog(OrdersLineProcessor.class);

    private final OrdersCollection orders;
    private final Splitter lineSplitter = Splitter.on('|').trimResults();
    private final Splitter ruleEntrySplitter = Splitter.on(';').trimResults().omitEmptyStrings();
    private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private final RuleIdTranslator ruleIdTran;

    public OrdersLineProcessor(int expectedOrders, RuleIdTranslator ridTranslator) {
        ruleIdTran = ridTranslator;
        orders = new OrdersCollection(expectedOrders, ruleIdTran);
    }
    public OrdersCollection getResult() {
        return orders;
    }

    @Override
    public boolean processLine(String line) {
        if(StringUtils.isBlank(line)) {
            return true;
        }
        OrderRow or = OrderRow.populate(lineSplitter, ruleEntrySplitter, ruleIdTran, line);
        orders.add(or);
        if( orders.size() % 100000 == 0) {
            log.warn("@ {0}", orders.size());
        }
        return true;
    }
}
