<%@page import="java.net.URLEncoder"%>

<html>
<head>
<title>Keycloak Sample External Application</title>
<meta>
</meta>
</head>

<body>
    <form action="submit-back.jsp" accept-charset="UTF-8">
        <input name="_tokenUrl" type="hidden" value="<%= request.getParameter("token") %>">
        Field 1: <input name="field_1">
        <br>
        Field 2: <input name="field_2">
        <br>
        <button type="submit">Submit value back to Keycloak</button>
    </form>
</body>
</html>
