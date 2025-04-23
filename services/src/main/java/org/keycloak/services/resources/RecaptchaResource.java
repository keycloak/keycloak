package org.keycloak.services.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;
import javax.imageio.ImageIO;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import com.google.code.kaptcha.Producer;

public class RecaptchaResource {

    private final KeycloakSession session;

    private static final Producer captchaProducer;

    public RecaptchaResource(KeycloakSession session) {
        this.session = session;
    }

    static {
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width", "200");
        properties.setProperty("kaptcha.image.height", "50");
        properties.setProperty("kaptcha.textproducer.char.length", "6");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.background.clear.from", "white");
        properties.setProperty("kaptcha.background.clear.to", "white");
        properties.setProperty("kaptcha.border", "no");

        Config config = new Config(properties);
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        kaptcha.setConfig(config);
        captchaProducer = kaptcha;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate() {
        RealmModel realm = session.getContext().getRealm();

        String authCookie = session.getContext().getRequestHeaders().getCookies().get("AUTH_SESSION_ID") != null
                ? session.getContext().getRequestHeaders().getCookies().get("AUTH_SESSION_ID").getValue()
                : null;

        if (authCookie == null || authCookie.contains(".")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing or invalid AUTH_SESSION_ID").build();
        }

        String[] parts = authCookie.split("\\.");
        String rootSessionId = parts[0];
        String tabId = session.getContext().getUri().getQueryParameters().getFirst("tab_id");

        RootAuthenticationSessionModel rootSession = session.authenticationSessions()
                .getRootAuthenticationSession(realm, rootSessionId);
        if (rootSession == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid root session").build();
        }

        AuthenticationSessionModel authSession = rootSession.getAuthenticationSessions().get(tabId);
        if (authSession == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid tab_id").build();
        }

        String captchaText = generateText();
        String image = generateCaptcha(captchaText);

        authSession.setAuthNote("captcha_expected", captchaText);

        return Response.ok(new CaptchaResponse(image)).build();
    }

    public String generateText(){
        return captchaProducer.createText();
    }

    public String generateCaptcha(String text) {
        BufferedImage image = captchaProducer.createImage(text);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);

            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CAPTCHA", e);
        }
    }

    public static class CaptchaResponse {
        public String image;
        public CaptchaResponse(String image) {
            this.image = image;
        }
    }
}
