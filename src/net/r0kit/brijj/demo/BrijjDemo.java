package net.r0kit.brijj.demo;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.r0kit.brijj.BrijjServlet;
import net.r0kit.brijj.RemoteRequestProxy;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class BrijjDemo {
  public static void main(String[] argsx) throws Exception {
    Server server = new Server(Integer.parseInt(argsx[0]));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/brijj");
    RemoteRequestProxy.register(Demo.class, UploadDownload.class, People.class, TestTypes.class);

    context.addServlet(BrijjServlet.class, "/*");

    // the bug in eclipse is fixed as follows:
    System.setProperty(System.getProperty("java.io.tmpdir"),System.getProperty("java.io.tmpdir"));
    
    context.addServlet(DefaultServlet.class, "/demo/*");
    context.setResourceBase(BrijjDemo.class.getResource("/net/r0kit/brijj").toExternalForm());
    ContextHandlerCollection contexts = new ContextHandlerCollection();
    contexts.setHandlers(new Handler[] { context });
    server.setHandler(contexts);
    server.start();
    server.join();
  }
  public static class Demo extends RemoteRequestProxy {
    public Demo(HttpServletRequest r, HttpServletResponse s) {
      super(r, s);
    }
    
    @Documentation(text="Supply the name of a person who will be greeted when the button is pressed")
    public String sayHello(@Eg(value="Joe") String name) {
      return "<span style=\"color: rgb(204,153,204);\">Hello, <b style=\"background: rgb(153,204,153); color: rgb(104,13,104); padding: 6px; \">" + name+"</b></span>";
    }
    public String[] getInsert() {
      return new String[] { "<p><strong style=color:red;>BriJJ is bridging!</strong></p>", "1.3.1" };
    }
    @Documentation(text="This just prefixes a constant string to the supplied value")
    public String getError(@Eg(value="bogus") String s) throws IOException {
      throw new IOException("invalid argument: " + s);
    }
  }
  public static class TestTypes extends RemoteRequestProxy {
    public TestTypes(HttpServletRequest q, HttpServletResponse p) {
      super(q, p);
    }
    public int getInt() {
      return 1000000000;
    }
    public double getDouble() {
      return 1000;
    }
    public int[] getIntArray() {
      return new int[] { 3, 6, 9, 101, 4324324 };
    }
    public double[] getDoubleArray() {
      return new double[] { 3, 6, 9, 101, 434343 };
    }
    public int sumArray(int[] a) {
      int b = 0;
      for (int c : a)
        b += c;
      return b;
    }
    public double productArray(double[] a) {
      double b = 1;
      for (double c : a)
        b *= c;
      return b;
    }
    public double crossProduct(int[] a, double[] b) {
      double res = 0;
      if (a.length != b.length) throw new IllegalArgumentException();
      for (int i = 0; i < a.length; i++)
        res += a[i] * b[i];
      return res;
    }
    public double productReals(double... ds) {
      double res = 1;
      for (double c : ds)
        res *= c;
      return res;
    }
  }
}
