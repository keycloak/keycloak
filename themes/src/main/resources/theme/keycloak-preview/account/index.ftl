<!DOCTYPE html>
<html>
<head>
    <title></title>

    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel="shortcut icon" href="${resourceUrl}/img/favicon.ico">

    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${resourceUrl}/${style}" rel="stylesheet" />
        </#list>
    </#if>

    <script type="text/javascript">
        var authUrl = '${authUrl}';
        var resourceUrl = '${resourceUrl}';
    </script>

    <script src="${resourceUrl}/js/app.js" type="text/javascript"></script>
    <script src="${authUrl}/js/${resourceVersion}/keycloak.js" type="text/javascript"></script>

    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script type="text/javascript" src="${resourceUrl}/${script}"></script>
        </#list>
    </#if>
</head>
<body>
</body>
</html>