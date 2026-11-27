<#ftl output_format="plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><#items as reqActionItem>${reqActionItem?counter}. ${msg("requiredAction.${reqActionItem}")}<#sep>${"\n"}</#sep></#items></#list><#else></#if></#assign>

${msg("executeActionsBody",link, linkExpiration, realmName, requiredActionsText, linkExpirationFormatter(linkExpiration))}