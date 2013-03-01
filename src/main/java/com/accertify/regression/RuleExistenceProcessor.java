package com.accertify.regression;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.accertify.util.SimpleHashSet;
import com.google.common.base.Splitter;
import com.google.common.io.LineProcessor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 1/3/12
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class RuleExistenceProcessor implements LineProcessor<Set<Long>> {
    protected static transient Log log = LogFactory.getLog(RuleStatsProcessor.class);

    private final Set<Long> trippedRules;
    private Splitter lineSplitter = Splitter.on('|').trimResults();

    public RuleExistenceProcessor() {
        trippedRules = new HashSet<Long>();
    }

    @Override
    public Set<Long> getResult() {
        return trippedRules;
    }

    @Override
    public boolean processLine(String line) {
        if(StringUtils.isBlank(line)) {
            return true;
        }
        Iterable<String> it = lineSplitter.split(line);

        Iterator<String> iter = it.iterator();
        Long id = Long.parseLong(iter.next());
        Integer score = Integer.parseInt(iter.next());
        Long monetary = new BigDecimal(iter.next()).longValue();
        String tid = iter.next();

        Set<Long> tripped = parse(iter.next() + ";" + iter.next() + ";" + iter.next());
        Boolean isFraud = StringUtils.equals(iter.next(), "1") ? Boolean.TRUE : Boolean.FALSE;
        trippedRules.addAll(tripped);

        return true;
    }



    private Set<Long> parse(String s) {
        Set<String> tripped = new SimpleHashSet<String>(StringUtils.split(s, "; "));
        Set<Long> r = new HashSet<Long>(tripped.size());
        for(String x:tripped) {
            r.add(Long.parseLong(StringUtils.substringBefore(x, ":")));
        }
        return r;
    }
}
