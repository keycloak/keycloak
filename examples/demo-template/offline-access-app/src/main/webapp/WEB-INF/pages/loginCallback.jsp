<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>Offline Access Example</title>
    </head>
    <body bgcolor="#ffffff">
        <h1>Offline Access Example</h1>

        <hr />

        <p>
            Login finished and refresh token saved successfully.
        </p>

        <p>
            <div style="background-color: #ddd; border: 1px solid #ccc; padding: 10px;">
                <% if ((Boolean) request.getAttribute("isOfflineToken")) { %>
                    Token type <b>is</b> offline token! You will be able to load customers even after logout or server restart. Offline token can be revoked in account management or by admin in admin console.
                <% } else { %>
                    Token <b>is not</b> offline token! Once you logout or restart server, token won't be valid anymore and you won't be able to load customers.
                <% } %>
            </div>
        </p>

        <p>
            <a href="/offline-access-portal/app">Back to home page</a>
        </p>

    </body>
</html>