package com.mrose.embed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: mrose
 * Date: 1/10/14
 * Time: 10:38 AM
 * <p/>
 * Comments
 */
public class VelocityRun {
    private static final Logger log = LoggerFactory.getLogger(VelocityRun.class);

    public static void main(String[] args) throws Exception {
        new TomcatEmbeddedRunner().startServer();
    }
}
