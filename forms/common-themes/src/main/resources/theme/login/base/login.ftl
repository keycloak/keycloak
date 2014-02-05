<#ftl strip_whitespace=true strip_text=true>
<#import "template.ftl" as layout>
<@layout.registrationLayout displaySocial=social.displaySocialProviders displaySeparator=realm.registrationAllowed; section>
    <#if section = "title">${rb.loginTitle} ${realm.name}

    <#elseif section = "header">
    ${rb.loginTitle} <strong>${(realm.name)!''}</strong>

    <#elseif section = "form">
    <form id="kc-form-login" action="${url.loginAction}" method="post">
        <div class="field-wrapper">
            <label for="username">${rb.username}</label><input id="username" name="username" value="${login.username!''}" type="text" />
        </div>
        <div class="field-wrapper">
            <label for="password">${rb.password}</label><input id="password" name="password" type="password" />
        </div>
        <input class="btn-primary" name="login" type="submit" value="${rb.logIn}"/>
        <input class="btn-secondary" name="cancel" type="submit" value="${rb.cancel}"/>
    </form>

    <#elseif section = "info" >
    <div id="kc-login-actions">
        <#if realm.registrationAllowed>
            <p>${rb.noAccount} <a href="${url.registrationUrl}">${rb.register}</a>.</p>
        </#if>
        <#if realm.resetPasswordAllowed>
            <p>${rb.loginForgot} <a href="${url.loginUsernameReminderUrl}">${rb.username}</a> / <a href="${url.loginPasswordResetUrl}">${rb.password}</a>?</p>
        </#if>
    </div>
    </#if>
</@layout.registrationLayout>
