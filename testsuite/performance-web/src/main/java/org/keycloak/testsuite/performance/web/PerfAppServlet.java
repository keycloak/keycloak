package org.keycloak.testsuite.performance.web;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PerfAppServlet extends HttpServlet {

    private Template indexTemplate;
    private OAuthClient oauthClient;

    @Override
    public void init() throws ServletException {
        try {
            Configuration cfg = new Configuration();
            cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/"));
            indexTemplate = cfg.getTemplate("perf-app-resources/index.ftl");

            oauthClient = new OAuthClient();
        } catch (IOException ioe) {
            throw new ServletException(ioe);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        String action = req.getParameter("action");

        if (action != null) {
            if (action.equals("code")) {
                keycloakLoginRedirect(req, resp);
                return;
            } else if (action.equals("exchangeCode")) {
                exchangeCodeForToken(req, resp);
            } else if (action.equals("refresh")) {
                refreshToken(req, resp);
            } else if (action.equals("logout")) {
                logoutRedirect(req, resp);
                return;
            }
        }

        String code = req.getParameter("code");
        if (code != null) {
            req.getSession().setAttribute("code", code);
        }

        String freemarkerRedirect = freemarkerRedirect(req, resp);
        resp.getWriter().println(freemarkerRedirect);
        resp.getWriter().flush();
    }

    protected void keycloakLoginRedirect(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String loginUrl = oauthClient.getLoginFormUrl();
        resp.sendRedirect(loginUrl);
    }

    protected void exchangeCodeForToken(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = (String)req.getSession().getAttribute("code");
        OAuthClient.AccessTokenResponse atResponse = oauthClient.doAccessTokenRequest(code, "password");

        String accessToken = atResponse.getAccessToken();
        String refreshToken = atResponse.getRefreshToken();
        req.getSession().setAttribute("accessToken", accessToken);
        req.getSession().setAttribute("refreshToken", refreshToken);
    }

    protected void refreshToken(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String refreshToken = (String)req.getSession().getAttribute("refreshToken");
        OAuthClient.AccessTokenResponse atResponse = oauthClient.doRefreshTokenRequest(refreshToken, "password");

        String accessToken = atResponse.getAccessToken();
        refreshToken = atResponse.getRefreshToken();
        req.getSession().setAttribute("accessToken", accessToken);
        req.getSession().setAttribute("refreshToken", refreshToken);
    }

    protected void logoutRedirect(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    private String freemarkerRedirect(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("requestURI", req.getRequestURI());
        attributes.put("code",  req.getSession().getAttribute("code"));
        attributes.put("accessToken",  req.getSession().getAttribute("accessToken"));
        attributes.put("refreshToken",  req.getSession().getAttribute("refreshToken"));

        try {
            Writer out = new StringWriter();
            indexTemplate.process(attributes, out);
            return out.toString();
        } catch (TemplateException te) {
            throw new ServletException(te);
        }
    }
}
