<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%><!doctype html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Log in to Keycloak</title>
        <link rel="stylesheet" href="<%=application.getContextPath()%>/saas/css/reset.css">
        <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/base.css">
        <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/forms.css">
        <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/zocial/zocial.css">
        <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/login-screen.css">
        <link rel="stylesheet" type="text/css" href='http://fonts.googleapis.com/css?family=Open+Sans:400,300,300italic,400italic,600,600italic,700,700italic,800,800italic'>
    </head>
    <body class="rcue-login-register">
        <h1><a href="#" title="Go to the home page"><img src="<%=application.getContextPath()%>/saas/img/red-hat-logo.png" alt="Red Hat logo"></a></h1>
        <div class="content">
            <h2>Log in to <strong>Keycloak</strong></h2>
            <div class="background-area">
                <div class="form-area social clearfix">
                    <section class="app-form">
                        <h3>Application login area</h3>
                        <form action="<%=application.getContextPath()%>/rest/saas/login" method="POST">
                            <%
                               String errorMessage = (String)request.getAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE");
                               if (errorMessage != null) { %>
                            <div class="feedback feedback-error">
                                <p><font color="red"><%=errorMessage%></font></p>
                            </div>
                            <% } %>
                            <div>
                                <label for="username">Username</label><input type="text" id="username" name="username" autofocus>
                            </div>
                            <div>
                                <label for="password">Password</label><input type="password" name="Password" id="password">
                            </div> <!--
                            <div>
                                <label for="one-time-pswd" class="two-lines">One-time-password</label><input type="password" id="one-time-pswd">
                            </div> -->
                            <div class="aside-btn">
                                <input type="checkbox" id="remember"><label for="remember">Remember Username</label>
                                <p>Forgot <a href="#">Username</a> or <a href="#">Password</a>?</p>
                            </div>
                            <input type="submit" value="Log In">
                        </form>
                    </section>
                    <section class="social-login">
                        <span>or</span>
                        <h3>Social login area</h3>
                        <p>Log In with</p>
                        <ul>
                            <li>
                                <a href="#" class="zocial facebook">
                                    <span class="text">Facebook</span>
                                </a>
                            </li>
                            <li>
                                <a href="#" class="zocial googleplus">
                                    <span class="text">Google</span>
                                </a>
                            </li>
                            <li>
                                <a href="#" class="zocial twitter">
                                    <span class="text">Twitter</span>
                                </a>
                            </li>
                        </ul>
                    </section>
                    <section class="info-area">
                        <h3>Info area</h3>
                        <p>Does not have an account? <a href="<%=application.getContextPath()%>/saas/saas-register.html">Register</a>.</p>
                        <ul>
                            <li><strong>Domain:</strong> 10.0.0.1</li>
                            <li><strong>Zone:</strong> Live</li>
                            <li><strong>Appliance:</strong> Yep</li>
                        </ul>
                    </section>
                </div>
            </div>
        </div>
    </body>
</html>

