<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
    ${rb.loginProfileTitle}

    <#elseif section = "header">
    ${rb.loginProfileTitle}

    <#elseif section = "form">
    <form id="kc-update-profile-form" action="${url.loginUpdateProfileUrl}" method="post">
        <p class="subtitle">All fields required</p>
        <div class="field-wrapper">
            <label for="email">${rb.email}</label><input type="text" id="email" name="email" value="${user.email!''}" />
        </div class="field-wrapper">
        <div class="field-wrapper">
            <label for="firstName">${rb.firstName}</label><input type="text" id="firstName" name="firstName" value="${user.firstName!''}" />
        </div>
        <div class="field-wrapper">
            <label for="lastName">${rb.lastName}</label><input type="text" id="lastName" name="lastName" value="${user.lastName!''}" />
        </div>
        <input class="btn-primary" type="submit" value="${rb.submit}" />
    </form>
    </#if>
</@layout.registrationLayout>