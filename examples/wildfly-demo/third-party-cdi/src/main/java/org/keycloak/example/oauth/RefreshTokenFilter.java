package org.keycloak.example.oauth;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.adapters.TokenGrantRequest;
import org.keycloak.servlet.ServletOAuthClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@WebFilter(value = "/client.jsf")
public class RefreshTokenFilter implements Filter {

    public static final String OAUTH_ERROR_ATTR = "oauthErrorAttr";

    @Inject
    private ServletOAuthClient oauthClient;

    @Inject
    private UserData userData;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)resp;
        Map<String, String[]> reqParams = request.getParameterMap();

        if (reqParams.containsKey("code")) {
            try {
                String accessToken = oauthClient.getBearerToken(request);
                userData.setAccessToken(accessToken);
            } catch (TokenGrantRequest.HttpFailure e) {
                throw new ServletException(e);
            }
        } else if (reqParams.containsKey("error")) {
            String oauthError = reqParams.get("error")[0];
            request.setAttribute(OAUTH_ERROR_ATTR, oauthError);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
