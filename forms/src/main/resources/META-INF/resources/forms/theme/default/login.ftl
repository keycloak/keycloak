<#import "template-login.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">

    Log in to ${realm.name}

    <#elseif section = "header">

    Log in to <strong>${(realm.name)!''}</strong>

    <#elseif section = "form">

    <div id="form">
        <form action="${url.loginAction}" method="post">
            <div>
                <label for="username">${rb.getString('username')}</label><input id="username" name="username" value="${login.username!''}" type="text" autofocus />
            </div>

            <div>
                <label for="password">${rb.getString('password')}</label><input id="password" name="password" type="password" />
            </div>

                <input class="btn-primary" name="login" type="submit" value="Log In"/>
                <input class="btn-secondary" name="cancel" type="submit" value="Cancel"/>
        </form>
    </div>

    <#elseif section = "info" >

    <div id="info">
        <#if realm.registrationAllowed>
            <p>${rb.getString('noAccount')} <a href="${url.registrationUrl}">${rb.getString('register')}</a>.</p>
        </#if>
        <#if realm.resetPasswordAllowed>
            <p>Forgot <a href="${url.loginUsernameReminderUrl}">Username</a> / <a href="${url.loginPasswordResetUrl}">Password</a>?</p>
        </#if>
    </div>

    </#if>
</@layout.registrationLayout>
