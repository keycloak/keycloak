<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.example.OfflineExampleUris" %>
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

        <% if (request.getRemoteUser() == null) { %>
            <a href="<%= OfflineExampleUris.LOGIN_CLASSIC %>">Login classic</a> |
            <a href="<%= OfflineExampleUris.LOGIN_WITH_OFFLINE_TOKEN %>">Login with offline access</a> |
        <% } else { %>
            <a href='<%= OfflineExampleUris.LOGOUT %>'>Logout</a> |
        <% } %>

        <a href='<%= OfflineExampleUris.ACCOUNT_MGMT %>'>Account management</a> |

        <% if ((Boolean) request.getAttribute("savedTokenAvailable")) { %>
            <a href="<%= OfflineExampleUris.LOAD_CUSTOMERS %>">Load customers with saved token</a> |
        <% } %>

        <hr />

        <h2>Saved Refresh Token Info</h2>
        <div style="background-color: #ddd; border: 1px solid #ccc; padding: 10px;">
            <%= request.getAttribute("tokenInfo") %>
        </div>

        <hr />

        <h2>Customers</h2>
        <div style="background-color: #ddd; border: 1px solid #ccc; padding: 10px;">
            <%= request.getAttribute("customers") %>
        </div>

    </body>
</html>