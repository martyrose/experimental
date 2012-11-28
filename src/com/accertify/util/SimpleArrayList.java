package com.accertify.util;

import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;

public class SimpleArrayList<T> extends ArrayList<T> {
    protected static transient org.apache.commons.logging.Log log = LogFactory.getLog(SimpleArrayList.class);

    public SimpleArrayList(Collection<T> args) {
        super(args);
    }

    public SimpleArrayList(T... arguments) {
        super();
        for(T o:arguments) {
            this.add(o);
        }
    }
}
