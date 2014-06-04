package com.mrose.util;

public abstract class LogFactory {

    public static Log getLog(Class<?> clazz) {
        return new Log(org.slf4j.LoggerFactory.getLogger(clazz));
    }

    public static Log getLog(String name) {
        return new Log(org.slf4j.LoggerFactory.getLogger(name));
    }
}