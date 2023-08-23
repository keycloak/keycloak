<!--
  ~ Copyright 2016 Red Hat, Inc. and/or its affiliates
  ~ and other contributors as indicated by the @author tags.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
<title>Keycloak SAML Client Adapter Example Application</title>
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
<link rel="StyleSheet" href="css/idp.css" type="text/css">
</head>

<body>
	<img src="images/keycloak_default_banner-1180px.png"
		style="margin-top: -10px; margin-left: -10px; opacity: 0.4; filter: alpha(opacity =   40);" />
	<div class="loginBox"
		style="margin-bottom: 80px; border: 1px solid #000000; width: 440px; background-color: #F8F8F8; align: center;">
		<center>
			<p>
				<b>Logged out.  <a href="<%= request.getContextPath() %>">Login</a> again.</b>
			</p>
		</center>
	</div>
</body>
</html>
