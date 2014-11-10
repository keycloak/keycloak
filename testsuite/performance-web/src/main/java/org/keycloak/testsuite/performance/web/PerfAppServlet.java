package org.keycloak.testsuite.performance.web;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.Time;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PerfAppServlet extends HttpServlet {

    public static final String BASE_URL_INIT_PARAM = "baseUrl";

    private Template indexTemplate;
    private OAuthClient oauthClient;

    @Override
    public void init() throws ServletException {
        try {
            Configuration cfg = new Configuration();
            cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/"));
            indexTemplate = cfg.getTemplate("perf-app-resources/index.ftl");

            String baseUrl = getInitParameter(BASE_URL_INIT_PARAM);
            oauthClient = new OAuthClient(baseUrl);
        } catch (IOException ioe) {
            throw new ServletException(ioe);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        String action = req.getParameter("action");
        String actionDone = null;

        if (action != null) {
            if (action.equals("code")) {
                keycloakLoginRedirect(req, resp);
                return;
            } else if (action.equals("exchangeCode")) {
                exchangeCodeForToken(req, resp);
                actionDone = "Token retrieved";
            } else if (action.equals("refresh")) {
                refreshToken(req, resp);
                actionDone = "Token refreshed";
            } else if (action.equals("logout")) {
                logoutRedirect(req, resp);
                return;
            }
        }

        String code = req.getParameter("code");
        if (code != null) {
            req.getSession().setAttribute("code", code);
            actionDone = "Code retrieved";
        }

        String freemarkerRedirect = freemarkerRedirect(req, resp, actionDone);
        resp.getWriter().println(freemarkerRedirect);
        resp.getWriter().flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().endsWith(AdapterConstants.K_LOGOUT)) {
            // System.out.println("Logout callback triggered");
            resp.setStatus(204);
        }
    }

    protected void keycloakLoginRedirect(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String loginUrl = oauthClient.getLoginFormUrl();
        resp.sendRedirect(loginUrl);
    }

    protected void exchangeCodeForToken(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = (String)req.getSession().getAttribute("code");
        OAuthClient.AccessTokenResponse atResponse = oauthClient.doAccessTokenRequest(code, "password");

        updateTokensInSession(req, atResponse);
    }

    protected void refreshToken(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String refreshToken = (String)req.getSession().getAttribute("refreshToken");
        OAuthClient.AccessTokenResponse atResponse = oauthClient.doRefreshTokenRequest(refreshToken, "password");

        updateTokensInSession(req, atResponse);
    }

    private void updateTokensInSession(HttpServletRequest req, OAuthClient.AccessTokenResponse atResponse) {
        String accessToken = atResponse.getAccessToken();
        String refreshToken = atResponse.getRefreshToken();
        AccessToken accessTokenParsed = oauthClient.verifyToken(accessToken);
        RefreshToken refreshTokenParsed = oauthClient.verifyRefreshToken(refreshToken);
        req.getSession().setAttribute("accessToken", accessToken);
        req.getSession().setAttribute("refreshToken", refreshToken);
        req.getSession().setAttribute("accessTokenParsed", accessTokenParsed);
        req.getSession().setAttribute("refreshTokenParsed", refreshTokenParsed);
    }

    protected void logoutRedirect(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Invalidate http session
        req.getSession(false).invalidate();

        String logoutURL = oauthClient.getLogoutUrl(oauthClient.getRedirectUri(), null);
        resp.sendRedirect(logoutURL);
    }

    private String freemarkerRedirect(HttpServletRequest req, HttpServletResponse resp, String actionDone) throws ServletException, IOException {
        AccessToken accessTokenParsed = (AccessToken)req.getSession().getAttribute("accessTokenParsed");
        RefreshToken refreshTokenParsed = (RefreshToken)req.getSession().getAttribute("refreshTokenParsed");

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("requestURI", req.getRequestURI());
        attributes.put("code",  req.getSession().getAttribute("code"));
        attributes.put("accessToken",  req.getSession().getAttribute("accessToken"));
        attributes.put("refreshToken",  req.getSession().getAttribute("refreshToken"));
        attributes.put("accessTokenParsed",  accessTokenParsed);
        attributes.put("refreshTokenParsed",  refreshTokenParsed);
        attributes.put("actionDone", actionDone);

        if (accessTokenParsed != null) {
            attributes.put("accessTokenExpiration", Time.toDate(accessTokenParsed.getExpiration()).toString());
        }
        if (refreshTokenParsed != null) {
            attributes.put("refreshTokenExpiration", Time.toDate(refreshTokenParsed.getExpiration()).toString());
        }

        try {
            Writer out = new StringWriter();
            indexTemplate.process(attributes, out);
            return out.toString();
        } catch (TemplateException te) {
            throw new ServletException(te);
        }
    }
}
