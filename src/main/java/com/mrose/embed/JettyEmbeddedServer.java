package com.mrose.embed;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JettyEmbeddedServer {
  private static final Logger log = LoggerFactory.getLogger(JettyEmbeddedServer.class);

  private static final CountDownLatch KEEP_RUNNING = new CountDownLatch(1);


  public void startServer() throws Exception {
    Server jetty = new Server(8080);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    jetty.setHandler(context);

    context.addServlet(org.eclipse.jetty.servlet.DefaultServlet.class, "/");
    context.addServlet(new ServletHolder(new EchoServlet()), "/echo/*");
    context.addServlet(new ServletHolder(new KillServlet()), "/kill/*");

    long t1 = System.nanoTime();
    jetty.start();
    long t2 = System.nanoTime();
    log.warn("Startup took: " + (t2*1000 - t1*1000) + " ms");
    try {
      KEEP_RUNNING.await();
      log.warn("Exiting");
    } catch (InterruptedException e) {
      jetty.stop();
      jetty.destroy();
    }
    log.warn("DONE");
    System.exit(1);
  }

  static class EchoServlet extends HttpServlet {

    private final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.getWriter().write(fmt.print(System.currentTimeMillis()));
      resp.getWriter().write(SystemUtils.LINE_SEPARATOR);
    }
  }

  static class KillServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.getWriter().write("OK");
      resp.getWriter().write(SystemUtils.LINE_SEPARATOR);
      KEEP_RUNNING.countDown();
    }
  }
}
