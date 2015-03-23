<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "title">
    ${message.summary}
    <#elseif section = "header">
    ${message.summary}
    <#elseif section = "form">
    <div id="kc-info-message">
        <p class="instruction">${message.summary}</p>
        <#if client.baseUrl??>
        <p><a href="${client.baseUrl}">${msg("backToApplication")}</a></p>
        </#if>
    </div>
    </#if>
</@layout.registrationLayout>