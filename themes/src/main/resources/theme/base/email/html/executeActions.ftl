<#outputformat "plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><ol><#items as reqActionItem><li>${msg("requiredAction.${reqActionItem}")}</li></#items></ol></#list></#if></#assign>
</#outputformat>

<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("executeActionsBodyHtml",link, linkExpiration, realmName, requiredActionsText, linkExpirationFormatter(linkExpiration)))?no_esc}
</@layout.emailLayout>
