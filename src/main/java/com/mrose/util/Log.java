package com.mrose.util;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps clogging with ability to do varargs messages.
 *
 * @author werges
 *
 */
public class Log {

    private final org.slf4j.Logger log;

    public Log(org.slf4j.Logger log) {
        this.log = log;
    }

    public void trace(Object msg) {
        if (this.isTraceEnabled()) {
            this.log.trace(String.valueOf(msg));
        }
    }

    public void trace(String msg) {
        if (this.isTraceEnabled()) {
            this.log.trace(msg);
        }
    }

    public void trace(Throwable t, Object msg) {
        if( this.isTraceEnabled() ) {
            this.log.trace(String.valueOf(msg), t);
        }
    }

    public void trace(Throwable t, String msg) {
        if( this.isTraceEnabled() ) {
            this.log.trace(msg, t);
        }
    }

    public void trace(String msg, Object... msgArgs) {
        if (this.isTraceEnabled()) {
            this.log.trace(MessageFormat.format(msg, msgArgs));
        }
    }

    public void trace(Throwable t, String msg, Object... msgArgs) {
        if (this.isTraceEnabled()) {
            this.log.trace(MessageFormat.format(msg, msgArgs), t);
        }
    }

    public void debug(Object msg) {
        if(this.isDebugEnabled()) {
            this.log.debug(String.valueOf(msg));
        }
    }

    public void debug(String msg) {
        if(this.isDebugEnabled()) {
            this.log.debug(msg);
        }
    }

    public void debug(Throwable t, Object msg) {
        if( this.isDebugEnabled() ) {
            this.log.debug(String.valueOf(msg), t);
        }
    }

    public void debug(Throwable t, String msg) {
        if( this.isDebugEnabled() ) {
            this.log.debug(msg, t);
        }
    }

    public void debug(String msg, Object... msgArgs) {
        if (this.isDebugEnabled()) {
            this.log.debug(MessageFormat.format(msg, msgArgs));
        }
    }

    public void debug(Throwable t, String msg, Object... msgArgs) {
        if (this.isDebugEnabled()) {
            this.log.debug(MessageFormat.format(msg, msgArgs), t);
        }
    }


    public void info(Object msg) {
        if(log.isInfoEnabled()) {
            this.log.info(String.valueOf(msg));
        }
    }

    public void info(String msg) {
        if(log.isInfoEnabled()) {
            this.log.info(msg);
        }
    }

    public void info(Throwable t, Object msg) {
        if( this.isInfoEnabled() ) {
            this.log.info(String.valueOf(msg), t);
        }
    }

    public void info(Throwable t, String msg) {
        if( this.isInfoEnabled() ) {
            this.log.info(msg, t);
        }
    }

    public void info(String msg, Object... msgArgs) {
        if (this.isInfoEnabled()) {
            this.log.info(MessageFormat.format(msg, msgArgs));
        }
    }

    public void info(Throwable t, String msg, Object... msgArgs) {
        if (this.isInfoEnabled()) {
            this.log.info(MessageFormat.format(msg, msgArgs), t);
        }
    }

    public void warn(Object msg) {
        if(log.isWarnEnabled()) {
            this.log.warn(String.valueOf(msg));
        }
    }

    public void warn(String msg) {
        if(log.isWarnEnabled()) {
            this.log.warn(msg);
        }
    }

    public void warn(Throwable t, Object msg) {
        if( this.isWarnEnabled() ) {
            this.log.warn(String.valueOf(msg), t);
        }
    }

    public void warn(Throwable t, String msg) {
        if( this.isWarnEnabled() ) {
            this.log.warn(msg, t);
        }
    }

    public void warn(String msg, Object... msgArgs) {
        if (this.isWarnEnabled()) {
            this.log.warn(MessageFormat.format(msg, msgArgs));
        }
    }

    public void warn(Throwable t, String msg, Object... msgArgs) {
        if (this.isWarnEnabled()) {
            this.log.warn(MessageFormat.format(msg, msgArgs), t);
        }
    }


    public void error(Object msg) {
        if(this.isErrorEnabled()) {
            this.log.error(String.valueOf(msg));
        }
    }

    public void error(String msg) {
        if(this.isErrorEnabled()) {
            this.log.error(msg);
        }
    }

    public void error(Throwable t, Object msg) {
        if( this.isErrorEnabled() ) {
            this.log.error(String.valueOf(msg), t);
        }
    }

    public void error(Throwable t, String msg) {
        if( this.isErrorEnabled() ) {
            this.log.error(msg, t);
        }
    }

    public void error(String msg, Object... msgArgs) {
        if (this.isErrorEnabled()) {
            this.log.error(MessageFormat.format(msg, msgArgs));
        }
    }

    public void error(Throwable t, String msg, Object... msgArgs) {
        if (this.isErrorEnabled()) {
            this.log.error(MessageFormat.format(msg, msgArgs), t);
        }
    }

    private String format(String msg, Object... args) {
        try {
            return MessageFormat.format(msg, args);
        } catch (Throwable t) {
            warn(t, "Unable to build message format for " + msg + " reason " + t.getMessage());
        }
        return "";
    }

    public boolean isTraceEnabled() {
        return this.log.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return this.log.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return this.log.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return this.log.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return this.log.isErrorEnabled();
    }
}
