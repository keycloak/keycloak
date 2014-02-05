<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>

    <#if section = "title">
    ${rb.loginTitle} ${realm.name}

    <#elseif section = "header">
    ${rb.loginTitle} <strong>${realm.name}</strong>

    <#elseif section = "form">
    <form id="kc-totp-login-form" action="${url.loginAction}" method="post">
        <input id="username" name="username" value="${login.username!''}" type="hidden" />
        <input id="password" name="password" value="${login.password!''}" type="hidden" />

        <div class="field-wrapper">
            <label for="totp">${rb.authenticatorCode}</label><input id="totp" name="totp" type="text" />
        </div>

        <div class="aside-btn">
            <!-- <input type="checkbox" id="remember" /><label for="remember">Remember Username</label> -->
            <!-- <p>Forgot <a href="#">Username</a> or <a href="#">Password</a>?</p> -->
        </div>

        <input class="btn-primary" type="submit" value="${rb.logIn}" />
    </form>

    <#elseif section = "info">
        <#if realm.registrationAllowed>
        <div id="kc-totp-register-link">
        <p>${rb.noAccount} <a href="${url.registrationUrl}">${rb.register}</a>.</p>
        </#if>
        </div>
    </#if>
</@layout.registrationLayout>