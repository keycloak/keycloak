<%-- 
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
--%>

<%@page contentType="text/html" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
        <title>Keycloak Example App</title>
        <link rel="stylesheet" type="text/css" href="styles.css"/>
    </head>
    <body>
        <jsp:useBean id="controller" class="org.keycloak.quickstart.profilejee.Controller" scope="request"/>
        <c:set var="idToken" value="<%=controller.getIDToken(request)%>"/>
        <c:set var="tokenString" value="<%=controller.getTokenString(request)%>"/>
        <c:set var="accountUri" value="<%=controller.getAccountUri(request)%>"/>
        <c:set var="showToken" value="<%=controller.showToken(request)%>"/>

        <div class="wrapper" id="profile">
            <div class="menu">
                <c:if test="${!showToken}">
                    <button onclick="location.href = 'profile.jsp?showToken=true'">Token</button>
                </c:if>
                <c:if test="${showToken}">
                    <button onclick="location.href = 'profile.jsp'">Profile</button>
                </c:if>
                <button onclick="location.href = 'index.jsp?logout=true'" type="button">Logout</button>
                <button onclick="location.href = '${accountUri}'" type="button">Account</button>
            </div>

            <c:if test="${showToken}">
                <div class="content">
                    <div id="token-content" class="message">${tokenString}</div>
                   <!-- <script>document.write(JSON.stringify(JSON.parse('${tokenString}'), null, '  '));</script>-->
                </div>
            </c:if>

            <c:if test="${!showToken}">
                <div class="content">
                    <div id="profile-content" class="message">
                        <table cellpadding="0" cellspacing="0">
                            <tr>
                                <td class="label">First name</td>
                                <td><span id="firstName">${idToken.givenName}</span></td>
                            </tr>
                            <tr class="even">
                                <td class="label">Last name</td>
                                <td><span id="lastName">${idToken.familyName}</span></td>
                            </tr>
                            <tr>
                                <td class="label">Username</td>
                                <td><span id="username">${idToken.preferredUsername}</span></td>
                            </tr>
                            <tr class="even">
                                <td class="label">Email</td>
                                <td><span id="email">${idToken.email}</span></td>
                            </tr>
                        </table>
                    </div>
                </div>
            </c:if>
        </div>
    </body>
</html>
