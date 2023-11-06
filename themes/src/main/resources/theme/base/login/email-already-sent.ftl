<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        ${msg("emailVerifyTitle")}
    <#elseif section = "form">
        <p class="instruction">${msg("emailAlreadySentInstruction1",user.email)}</p> 
    <#elseif section = "info">
        <p class="instruction">
             <#if remainingMinutes gt 0 && remainingSeconds gt 0>
                ${msg("emailAlreadySentInstruction2WithMinutesAndSeconds", remainingMinutes, remainingSeconds)}
            <#elseif remainingMinutes == 0 && remainingSeconds gt 0>
                ${msg("emailAlreadySentInstruction2WithSeconds", remainingSeconds)}
            <#elseif remainingMinutes gt 0 && remainingSeconds == 0>
                ${msg("emailAlreadySentInstruction2WithMinutes", remainingMinutes)}
            </#if>
        </p>
    </#if>
</@layout.registrationLayout>
