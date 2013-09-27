<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">

    Verify email

    <#elseif section = "header">

    Verify email

    <#elseif section = "form">

    <div id="form">
    	An email with instructions to verify your email address has been sent to you. If you don't receive this email, 
    	<a href="${url.emailVerificationUrl}">click here</a> to re-send the email.
    </div>

    <#elseif section = "info" >

    <div id="info">
    </div>

    </#if>
</@layout.registrationLayout>