<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("emailVerifiedMessageHeader")}
    <#elseif section = "form">
        <p class="instruction">
            ${msg("emailVerifiedMessage")}
        </p>
    </#if>
</@layout.registrationLayout>
