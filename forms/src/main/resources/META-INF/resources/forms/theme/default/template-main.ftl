<#macro mainLayout active bodyClass>
<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Edit Account</title>
    <link rel="icon" href="img/favicon.ico">

    <!-- Frameworks -->
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/reset.css">
    <!--link rel="stylesheet" href="bootstrap-3.0.0-wip/css/bootstrap.css"-->
    <link href="${template.formsPath}/lib/bootstrap/css/bootstrap.css" rel="stylesheet" />

    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/sprites.css">
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/select2.css">
    <link rel="stylesheet" href='http://fonts.googleapis.com/css?family=Open+Sans:400,300,300italic,400italic,600,600italic,700,700italic,800,800italic'>

    <!-- RCUE styles -->
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/base.css">
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/forms.css">
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/header.css">
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/tabs.css">
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/icons.css">
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/tables.css">

    <!-- Page styles -->
    <link rel="stylesheet" href="${template.formsPath}/theme/${template.theme}/css/admin-console.css">

    <script src="${template.formsPath}/lib/jquery/jquery-2.0.3.min.js"></script>
    <script src="${template.formsPath}/lib/bootstrap/js/bootstrap.js"></script>

</head>
<body class="admin-console user ${bodyClass}">

    <#if error?has_content>
    <!--div class="feedback success show"><p><strong>Success!</strong> Your changes have been saved.</p></div-->
    <div class="feedback-aligner">
        <div class="alert alert-danger">${rb.getString(error.summary)}</div>
    </div>
    </#if>

<div class="header rcue">
    <div class="navbar utility">
        <div class="navbar-inner clearfix">
            <h1><a href="#"><strong>Keycloak</strong> Central Login</a></h1>
            <ul class="nav pull-right">
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-user">Icon: user</span>
                    ${user.firstName!''} ${user.lastName!''}</a>
                </li>
            </ul>
        </div>
    </div>
</div><!-- End .header -->

<div class="container">
    <div class="row">
        <div class="bs-sidebar col-md-3 clearfix">
            <ul>
                <li class="<#if active=='account'>active</#if>"><a href="${url.accountUrl}">Account</a></li>
                <li class="<#if active=='password'>active</#if>"><a href="${url.passwordUrl}">Password</a></li>
                <li class="<#if active=='totp'>active</#if>"><a href="${url.totpUrl}">Authenticator</a></li>
                <li class="<#if active=='social'>active</#if>"><a href="${url.socialUrl}">Social Accounts</a></li>
                <li class="<#if active=='access'>active</#if>"><a href="${url.accessUrl}">Authorized Access</a></li>
            </ul>
        </div>

        <div id="content-area" class="col-md-9" role="main">
            <div id="content">
                <h2 class="pull-left"><#nested "header"></h2>
                <#nested "content">
            </div>
        </div>
        <div id="container-right-bg"></div>
    </div>
</div>
</body>
</html>
</#macro>