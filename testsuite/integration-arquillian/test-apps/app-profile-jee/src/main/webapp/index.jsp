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
        <% controller.handleLogout(request); %>
        
        <c:set var="isLoggedIn" value="<%=controller.isLoggedIn(request)%>"/>
        <c:if test="${isLoggedIn}">
            <c:redirect url="profile.jsp"/>
        </c:if>

        <div class="wrapper" id="welcome">
            <div class="menu">
                <button onclick="location.href = 'profile.jsp'" type="button">Login</button>
            </div>

            <div class="content">
                <div class="message">Please login</div>
            </div>
        </div>
    </body>
</html>
