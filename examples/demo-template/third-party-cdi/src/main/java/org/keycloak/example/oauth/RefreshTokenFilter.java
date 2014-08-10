package org.keycloak.example.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.servlet.ServletOAuthClient;

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
import java.io.IOException;
import java.util.Map;

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

        if (reqParams.containsKey(OAuth2Constants.CODE)) {
            try {
                String accessToken = oauthClient.getBearerToken(request).getToken();
                userData.setAccessToken(accessToken);
            } catch (ServerRequest.HttpFailure e) {
                throw new ServletException(e);
            }
        } else if (reqParams.containsKey(OAuth2Constants.ERROR)) {
            String oauthError = reqParams.get(OAuth2Constants.ERROR)[0];
            request.setAttribute(OAUTH_ERROR_ATTR, oauthError);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
