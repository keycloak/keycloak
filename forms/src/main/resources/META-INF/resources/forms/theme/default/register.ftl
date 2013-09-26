<#import "template-login.ftl" as layout>
<@layout.registrationLayout bodyClass="register" ; section>

    <#if section = "title">

    ${rb.getString('registerWith')} ${realm.name}

    <#elseif section = "header">

    ${rb.getString('registerWith')} <strong>${realm.name}</strong>

    <#elseif section = "form">

    <form action="${url.registrationAction}" method="post">
        <p class="subtitle">${rb.getString('allRequired')}</p>
        <div>
            <label for="name">${rb.getString('fullName')}</label>
            <input type="text" id="name" name="name" value="${register.formData.name?default('')}" />
        </div>
        <div>
            <label for="email">${rb.getString('email')}</label>
            <input type="text" id="email" name="email" value="${register.formData.email?default('')}" />
        </div>
        <div>
            <label for="username">${rb.getString('username')}</label>
            <input type="text" id="username" name="username" value="${register.formData.username?default('')}" />
        </div>
        <div>
            <label for="password">${rb.getString('password')}</label>
            <input type="password" id="password" name="password" />
        </div>
        <div>
            <label for="password-confirm">${rb.getString('passwordConfirm')}</label>
            <input type="password" id="password-confirm" name="password-confirm" />
        </div>

        <div class="aside-btn">
            <p>By registering you agree to the <a href="#">Terms of Service</a> and the <a href="#">Privacy Policy</a>.</p>
        </div>

        <input class="btn-primary" type="submit" value="Register"/>
    </form>

    <#elseif section = "info">

    <p>${rb.getString('alreadyHaveAccount')} <a href="${url.loginUrl}">${rb.getString('logIn')}</a>.</p>

    </#if>

</@layout.registrationLayout>