package org.keycloak.social.utils;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ToJsonServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        toJson(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        toJson(req, resp);
    }

    private void toJson(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject o = new JSONObject();

        JSONObject headers = new JSONObject();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String n = headerNames.nextElement();
            headers.put(n, req.getHeader(n));
        }
        o.put("headers", headers);

        JSONObject params = new JSONObject();
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String n = parameterNames.nextElement();
            params.put(n, req.getParameter(n));
        }
        o.put("params", params);

        resp.setContentType("application/json");
        resp.getOutputStream().write(o.toString().getBytes());
    }

}
