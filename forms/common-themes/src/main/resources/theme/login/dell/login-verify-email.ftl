<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${rb.emailVerifyTitle}
    <#elseif section = "header">
        ${rb.emailVerifyTitle}
    <#elseif section = "form">
        <p class="instruction">
            ${rb.emailVerifyInstr}
        </p>
        <p class="instruction">${rb.emailVerifyInstrQ}
            <a href="${url.loginEmailVerificationUrl}">${rb.emailVerifyClick}</a> ${rb.emailVerifyResend}
        </p>
    </#if>
</@layout.registrationLayout>