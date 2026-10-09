<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "form">
        <span id="oauth-redirect-uri">${oauth.redirectUri}</span>
    </#if>
</@layout.registrationLayout>
