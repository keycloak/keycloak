<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${rb.emailVerifyTitle}
    <#elseif section = "header">
        ${rb.emailVerifyTitle}
    <#elseif section = "form">
        <p class="instruction">
            ${rb.emailVerifyInstruction1}
        </p>
        <p class="instruction">
            ${rb.emailVerifyInstruction2} <a href="${url.loginEmailVerificationUrl}">${rb.doClickHere}</a> ${rb.emailVerifyInstruction3}
        </p>
    </#if>
</@layout.registrationLayout>