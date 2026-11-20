<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "form">
        <#if oauth.redirectUri??>
            <span id="oauth-redirect-uri">${oauth.redirectUri}</span>
        </#if>
    </#if>
</@layout.registrationLayout>
