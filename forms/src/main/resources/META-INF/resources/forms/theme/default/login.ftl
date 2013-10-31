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
                <label for="username">${rb.getString('username')}</label><input id="username" name="username" value="${login.username!''}" type="text" />
            </div>

            <#list login.requiredCredentials as c>
                <div>
                    <label for="${c.name}">${rb.getString(c.label)}</label><input id="${c.name}" name="${c.name}" type="${c.inputType}" />
                </div>
            </#list>

            <div class="aside-btn">
                <p>Forgot <a href="${url.loginPasswordResetUrl}">Password</a>?</p>
            </div>

            <input class="btn-primary" type="submit" value="Log In"/>
        </form>
    </div>

    <#elseif section = "info" >

    <div id="info">
        <#if realm.registrationAllowed>
            <p>${rb.getString('noAccount')} <a href="#">${rb.getString('register')}</a>.</p>
        </#if>
    </div>

    </#if>
</@layout.registrationLayout>