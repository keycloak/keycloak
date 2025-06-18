<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<@layout.registrationLayout; section>
    <#if section = "header">
        <#if code.success>
            ${msg("codeSuccessTitle")}
        <#else>
            ${kcSanitize(msg("codeErrorTitle", code.error))}
        </#if>
    <#elseif section = "form">
        <div id="kc-code">
            <#if code.success>
                <p>${msg("copyCodeInstruction")}</p>
                <@field.input name="code" label="" value=code.code />
            <#else>
                <p id="error">${kcSanitize(code.error)}</p>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>
