package com.accertify.genetic;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/10/11
 * Time: 3:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyManager {
    protected static transient Log log = LogFactory.getLog(PropertyManager.class);

    public static final Properties readProperties() {
        InputStream is = null;
        Properties p = new Properties();
        try {
            is = OrdersManager.class.getClassLoader().getResourceAsStream("application.properties");
            p.load(is);
        } catch(IOException iex) {
            log.warn(iex.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
        return p;
    }
}
