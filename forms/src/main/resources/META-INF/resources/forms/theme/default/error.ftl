<#-- TODO: Only a placeholder, implementation needed -->
<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass="reset"; section>
    <#if section = "title">

    We're sorry...

    <#elseif section = "header">

    We're <strong>sorry</strong> ...

    <#elseif section = "form">

        <p class="instruction">Something happened and we could not process your request.</p>
        <p class="instruction second">Please make sure the URL you entered is correct.</p>
        <a href="saas-login.html" class="link-right">Go to the homepage »</a>

    <#elseif section = "info" >

    <div id="info">
    </div>

    </#if>
</@layout.registrationLayout>