<#-- TODO: Only a placeholder, implementation needed -->
<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">
    ${rb.errorTitle}

    <#elseif section = "header">
    ${rb.errorTitleHtml}

    <#elseif section = "form">
    <div id="kc-error-message">
        <p class="instruction">${rb.errorGenericMsg}</p>
        <p id="error-summary" class="instruction second">${message.summary}</p>
    </div>
    </#if>
</@layout.registrationLayout>