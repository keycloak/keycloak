<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        ${msg("emailVerifyTitle")}
    <#elseif section = "form">
        <p class="instruction">${msg("emailAlreadySentInstruction1",user.email)}</p> 
    <#elseif section = "info">
        <p class="instruction">
            ${msg("emailAlreadySentInstruction2")}
        </p>
    </#if>
</@layout.registrationLayout>
