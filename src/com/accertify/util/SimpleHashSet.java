package com.accertify.util;

import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.HashSet;

public class SimpleHashSet<T> extends HashSet<T> {
    protected static transient org.apache.commons.logging.Log log = LogFactory.getLog(SimpleHashSet.class);

    public SimpleHashSet(Collection<T> args) {
        super(args);
    }

    public SimpleHashSet(T... arguments) {
        super();
        for(T o:arguments) {
            this.add(o);
        }
    }
}
