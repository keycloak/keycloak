package org.keycloak;

import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping(path = "/admin")
public class AdminController {

    private static Logger logger = LoggerFactory.getLogger(AdminController.class);
	
	@RequestMapping(path = "/TokenServlet", method = RequestMethod.GET)
	public String showTokens(WebRequest req, Model model, @RequestParam Map<String, String> attributes) throws IOException {
	    String timeOffset = attributes.get("timeOffset");
	    if (!StringUtils.isEmpty(timeOffset)) {
	        int offset;
	        try {
                offset = Integer.parseInt(timeOffset, 10);
            }
            catch (NumberFormatException e) {
	            offset = 0;
            }

            Time.setOffset(offset);
        }

        RefreshableKeycloakSecurityContext ctx =
        		(RefreshableKeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName(), WebRequest.SCOPE_REQUEST);
        String accessTokenPretty = JsonSerialization.writeValueAsPrettyString(ctx.getToken());
        RefreshToken refreshToken;
        try {
            refreshToken = new JWSInput(ctx.getRefreshToken()).readJsonContent(RefreshToken.class);
        } catch (JWSInputException e) {
            throw new IOException(e);
        }
        String refreshTokenPretty = JsonSerialization.writeValueAsPrettyString(refreshToken);
        
        model.addAttribute("accessToken", accessTokenPretty);
        model.addAttribute("refreshToken", refreshTokenPretty);
        model.addAttribute("accessTokenString", ctx.getTokenString());
        
        return "tokens";
	}

	@RequestMapping(path = "/SessionServlet", method = RequestMethod.GET)
    public String sessionServlet(WebRequest webRequest, Model model) {
	    String counterString = (String) webRequest.getAttribute("counter", RequestAttributes.SCOPE_SESSION);
	    int counter = 0;
	    try {
	        counter = Integer.parseInt(counterString, 10);
        }
        catch (NumberFormatException ignored) {
        }

        model.addAttribute("counter", counter);

	    webRequest.setAttribute("counter", Integer.toString(counter+1), RequestAttributes.SCOPE_SESSION);

	    return "session";
    }

    @RequestMapping(path = "/LinkServlet", method = RequestMethod.GET)
    public String tokenController(WebRequest webRequest,
                                  @RequestParam Map<String, String> attributes,
                                  Model model) {

        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession httpSession = attr.getRequest().getSession(true);

//        response.addHeader("Cache-Control", "no-cache");

        String responseAttr = attributes.get("response");

        if (StringUtils.isEmpty(responseAttr)) {
            String provider = attributes.get("provider");
            String realm = attributes.get("realm");
            KeycloakSecurityContext keycloakSession =
                    (KeycloakSecurityContext) webRequest.getAttribute(
                            KeycloakSecurityContext.class.getName(),
                            RequestAttributes.SCOPE_REQUEST);
            AccessToken token = keycloakSession.getToken();
            String clientId = token.getIssuedFor();
            String nonce = UUID.randomUUID().toString();
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            String input = nonce + token.getSessionState() + clientId + provider;
            byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64Url.encode(check);
            httpSession.setAttribute("hash", hash);
            String redirectUri = KeycloakUriBuilder.fromUri("http://localhost:8280/admin/LinkServlet")
                    .queryParam("response", "true").build().toString();
            String accountLinkUrl = KeycloakUriBuilder.fromUri("http://localhost:8180/")
                    .path("/auth/realms/{realm}/broker/{provider}/link")
                    .queryParam("nonce", nonce)
                    .queryParam("hash", hash)
                    .queryParam("client_id", token.getIssuedFor())
                    .queryParam("redirect_uri", redirectUri).build(realm, provider).toString();

            return "redirect:" + accountLinkUrl;
        } else {
            String error = attributes.get("link_error");
            if (StringUtils.isEmpty(error))
                model.addAttribute("error", "Account linked");
            else
                model.addAttribute("error", error);

            return "linking";
        }
    }
}
