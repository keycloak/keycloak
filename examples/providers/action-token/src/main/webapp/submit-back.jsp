<%@page import="java.net.URLDecoder"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="javax.crypto.spec.SecretKeySpec"%>
<%@page import="org.keycloak.common.util.Base64"%>
<%@page import="org.keycloak.jose.jws.JWSBuilder"%>
<%@page import="org.keycloak.representations.JsonWebToken"%>

<%
    String secret = "aSqzP4reFgWR4j94BDT1r+81QYp/NYbY9SBwXtqV1ko=";
    
    JsonWebToken tokenSentBack = new JsonWebToken();
    SecretKeySpec hmacSecretKeySpec = new SecretKeySpec(Base64.decode(secret), "HmacSHA256");

    for (java.util.Map.Entry<String, String[]> me : request.getParameterMap().entrySet()) {
        String name = me.getKey();
        if (! name.startsWith("_")) {
            String decodedValue = URLDecoder.decode(me.getValue()[0], "UTF-8");
            tokenSentBack.setOtherClaims(name, decodedValue);
        }
    }
    
    String appToken = new JWSBuilder().jsonContent(tokenSentBack).hmac256(hmacSecretKeySpec);
    String encodedToken = URLEncoder.encode(appToken, "UTF-8");

    String decodedUrl = URLDecoder.decode(request.getParameter("_tokenUrl"), "UTF-8");
    response.sendRedirect(decodedUrl.replace("{APP_TOKEN}", encodedToken));
%>
