<#assign requiredActionsText>
<#if requiredActions??><#list requiredActions><b><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#items></b></#list><#else></#if>
</#assign>
<html>
<body>
${msg("executeActionsBodyHtml",link, linkExpiration, realmName, requiredActionsText)}
</body>
</html>
