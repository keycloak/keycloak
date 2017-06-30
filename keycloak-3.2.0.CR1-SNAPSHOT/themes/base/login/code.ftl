<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        <#if code.success>
            ${msg("codeSuccessTitle")}
        <#else>
            ${msg("codeErrorTitle", code.error)}
        </#if>
    <#elseif section = "form">
        <div id="kc-code">
            <#if code.success>
                <p>${msg("copyCodeInstruction")}</p>
                <input id="code" class="${properties.kcTextareaClass!}" value="${code.code}"/>
            <#else>
                <p id="error">${code.error}</p>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>
