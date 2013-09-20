<#import "template-login.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>

    <#if section = "title">

    Log in to ${realm.name}

    <#elseif section = "header">

    Log in to <strong>${realm.name}</strong>

    <#elseif section = "form">

    <form action="${url.loginAction}" method="post">
        <input id="username" name="username" value="${login.username?default('')}" type="hidden" />
        <input id="password" name="password" value="${login.password?default('')}" type="hidden" />

        <div>
            <label for="totp">${rb.getString('authenticatorCode')}</label>
            <input id="totp" name="totp" type="text" />
        </div>

        <div class="aside-btn">
            <!-- <input type="checkbox" id="remember" /><label for="remember">Remember Username</label> -->
            <!-- <p>Forgot <a href="#">Username</a> or <a href="#">Password</a>?</p> -->
        </div>

        <input type="submit" value="Log In" />
    </form>

    <#elseif section = "info">

        <#if realm.registrationAllowed>
        <p>${rb.getString('noAccount')} <a href="${url.registrationUrl}">${rb.getString('register')}</a>.</p>
        </#if>

    </#if>
</@layout.registrationLayout>