<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>Keycloak Security Failure</title>

<!--    <link href="<%=application.getContextPath()%>/lib/bootstrap/css/bootstrap.css" rel="stylesheet">
    <link href="<%=application.getContextPath()%>/lib/font-awesome/css/font-awesome.css" rel="stylesheet"> -->
    <link href="<%=application.getContextPath()%>/saas/css/reset.css" rel="stylesheet">
    <link href="<%=application.getContextPath()%>/saas/css/base.css" rel="stylesheet">
</head>

<body>
    <h1>Security Failure</h1>
    <hr/>
    <div class="modal-body">
        <div id="error-message" class="alert alert-block alert-error" style="block"><%=request.getAttribute("KEYCLOAK_SECURITY_FAILURE_MESSAGE") %></div>
    </div>
</body>
</html>
