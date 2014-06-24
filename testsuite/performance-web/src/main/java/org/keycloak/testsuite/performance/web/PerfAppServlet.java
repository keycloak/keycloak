package org.keycloak.testsuite.performance.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PerfAppServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String resourcePath = "perf-app-resources" + req.getPathInfo();
        System.out.println("Resource path: " + resourcePath);

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            resp.getWriter().println("Not found: " + resourcePath);
        } else {
            OutputStream servletOutputStream = resp.getOutputStream();

            byte[] buf = new byte[1024];
            int bytesRead = 0;
            while (bytesRead != -1) {
                bytesRead = inputStream.read(buf);
                if (bytesRead != -1) {
                    servletOutputStream.write(buf, 0, bytesRead);
                }
            }
            servletOutputStream.flush();
        }
    }
}
