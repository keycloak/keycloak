<#import "template.ftl" as layout>
<#list event.details as detail>
    <#if detail.key == "credential_type">
        <#assign credential_type = detail.value>
    </#if>
</#list>
<@layout.emailLayout>
${kcSanitize(msg("eventUpdateCredentialBodyHtml", credential_type!"unknown", event.date, event.ipAddress))?no_esc}
</@layout.emailLayout>
