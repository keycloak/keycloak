<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("pageExpiredTitle")}
    <#elseif section = "header">
        ${msg("pageExpiredTitle")}
    <#elseif section = "form">
        <p id="instruction1" class="instruction">
            ${msg("pageExpiredMsg1")} <a href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a> . ${msg("pageExpiredMsg2")} <a href="${url.loginAction}">${msg("doClickHere")}</a> .
        </p>
    </#if>
</@layout.registrationLayout>