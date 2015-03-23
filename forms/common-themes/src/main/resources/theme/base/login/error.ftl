<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "title">
        ${msg("errorTitle")}
    <#elseif section = "header">
        ${msg("errorTitleHtml")}
    <#elseif section = "form">
        <div id="kc-error-message">
            <p class="instruction">${message.summary}</p>
        </div>
    </#if>
</@layout.registrationLayout>