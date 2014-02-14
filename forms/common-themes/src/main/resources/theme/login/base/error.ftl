<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "title">
        ${rb.errorTitle}
    <#elseif section = "header">
        ${rb.errorTitleHtml}
    <#elseif section = "form">
        <div id="kc-error-message">
            <p class="instruction">${message.summary}</p>
        </div>
    </#if>
</@layout.registrationLayout>