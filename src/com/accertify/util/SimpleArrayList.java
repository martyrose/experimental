package com.accertify.util;

import java.util.ArrayList;
import java.util.Collection;

public class SimpleArrayList<T> extends ArrayList<T> {

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
