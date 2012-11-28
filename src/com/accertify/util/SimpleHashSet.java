package com.accertify.util;

import java.util.Collection;
import java.util.HashSet;

public class SimpleHashSet<T> extends HashSet<T> {

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
