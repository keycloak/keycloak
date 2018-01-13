<#outputformat "plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#sep></#items></#list></#if></#assign>
</#outputformat>

<html>
<body>
${msg("executeActionsBodyHtml",link, linkExpiration, realmName, requiredActionsText)?no_esc}
</body>
</html>
