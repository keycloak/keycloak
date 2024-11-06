<#ftl output_format="plainText">
<#list event.details as detail>
    <#if detail.key == "credential_type">
        <#assign credential_type = detail.value>
    </#if>
</#list>
${msg("eventUpdateCredentialBody", credential_type!"unknown", event.date, event.ipAddress)}