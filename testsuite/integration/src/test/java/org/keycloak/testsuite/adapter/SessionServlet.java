package org.keycloak.testsuite.adapter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String counter = increaseAndGetCounter(req);

        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", "Session Test");
        pw.printf("Counter=%s", counter);
        pw.print("</body></html>");
        pw.flush();


    }

    private String increaseAndGetCounter(HttpServletRequest req) {
        HttpSession session = req.getSession();
        Integer counter = (Integer)session.getAttribute("counter");
        counter = (counter == null) ? 1 : counter + 1;
        session.setAttribute("counter", counter);
        return String.valueOf(counter);
    }
}
