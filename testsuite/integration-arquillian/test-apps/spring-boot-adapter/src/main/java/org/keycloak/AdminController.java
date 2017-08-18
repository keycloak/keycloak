package org.keycloak;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.JsonSerialization;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping(path = "/admin")
public class AdminController {
	
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
}
