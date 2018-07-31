<%@page import="org.keycloak.common.util.Time"%>

<%
   Time.setOffset(Integer.parseInt(request.getParameter("offset")));
%>