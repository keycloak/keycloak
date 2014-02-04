<#ftl strip_whitespace=true strip_text=true>
<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">Log in to ${realm.name}

    <#elseif section = "header">
    Log in to <strong>${(realm.name)!''}</strong>

    <#elseif section = "form">
    <form id="kc-form-login" action="${url.loginAction}" method="post">
        <div class="field-wrapper">
            <label for="username">${rb.username}</label><input id="username" name="username" value="${login.username!''}" type="text" />
        </div>
        <div class="field-wrapper">
            <label for="password">${rb.password}</label><input id="password" name="password" type="password" />
        </div>
        <input class="btn-primary" name="login" type="submit" value="Log In"/>
        <input class="btn-secondary" name="cancel" type="submit" value="Cancel"/>
    </form>

    <#elseif section = "info" >
    <div id="kc-login-actions">
        <#if realm.registrationAllowed>
            <p>${rb.noAccount} <a href="${url.registrationUrl}">${rb.register}</a>.</p>
        </#if>
        <#if realm.resetPasswordAllowed>
            <p>Forgot <a href="${url.loginUsernameReminderUrl}">Username</a> / <a href="${url.loginPasswordResetUrl}">Password</a>?</p>
        </#if>
    </div>
    </#if>
</@layout.registrationLayout>
