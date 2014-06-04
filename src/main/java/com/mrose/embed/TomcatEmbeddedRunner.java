package com.mrose.embed;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: mrose
 * Date: 1/10/14
 * Time: 10:38 AM
 * <p/>
 * Comments
 */
public class TomcatEmbeddedRunner {
    private static final Logger log = LoggerFactory.getLogger(TomcatEmbeddedRunner.class);

    private static final CountDownLatch KEEP_RUNNING = new CountDownLatch(1);

    public void startServer() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        File base = new File(System.getProperty("java.io.tmpdir"));
        Context rootCtx = tomcat.addContext("", base.getAbsolutePath());

        Tomcat.addServlet(rootCtx, "KillServlet", new KillServlet());
        rootCtx.addServletMapping("/shutdown", "KillServlet");

        Tomcat.addServlet(rootCtx, "EchoServlet", new EchoServlet());
        rootCtx.addServletMapping("/*", "EchoServlet");
        tomcat.start();

        try {
            KEEP_RUNNING.await();
        } catch (InterruptedException e) {
            ;
        }
    }

    static class EchoServlet extends HttpServlet {
        private final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write(fmt.print(System.currentTimeMillis()));
            resp.getWriter().write(SystemUtils.LINE_SEPARATOR);
        }
    }

    static class KillServlet extends HttpServlet {
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write("OK");
            resp.getWriter().write(SystemUtils.LINE_SEPARATOR);
            KEEP_RUNNING.countDown();
        }
    }
}
