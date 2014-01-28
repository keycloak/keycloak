<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>

    <#if section = "title">

    Log in to ${realm.name}

    <#elseif section = "header">

    Log in to <strong>${realm.name}</strong>

    <#elseif section = "form">

    <form action="${url.loginAction}" method="post">
        <input id="username" name="username" value="${login.username!''}" type="hidden" />
        <input id="password" name="password" value="${login.password!''}" type="hidden" />

        <div>
            <label for="totp">${rb.authenticatorCode}</label><input id="totp" name="totp" type="text" />
        </div>

        <div class="aside-btn">
            <!-- <input type="checkbox" id="remember" /><label for="remember">Remember Username</label> -->
            <!-- <p>Forgot <a href="#">Username</a> or <a href="#">Password</a>?</p> -->
        </div>

        <input class="btn-primary" type="submit" value="Log In" />
    </form>

    <#elseif section = "info">

        <#if realm.registrationAllowed>
        <p>${rb.noAccount} <a href="${url.registrationUrl}">${rb.register}</a>.</p>
        </#if>

    </#if>
</@layout.registrationLayout>