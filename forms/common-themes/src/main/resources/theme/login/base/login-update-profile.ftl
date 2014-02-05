<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">
    ${rb.loginProfileTitle}

    <#elseif section = "header">
    ${rb.loginProfileTitle}

    <#elseif section = "feedback">
    <div id="kc-feedback" class="feedback warning show">
        <p><strong>${rb.loginProfileWarning}</strong><br>${rb.loginProfileWarningFollow}</p>
    </div>
    <#elseif section = "form">
    <form id="kc-update-profile-form" action="${url.loginUpdateProfileUrl}" method="post">
        <div class="feedback error bottom-left">
            <p><strong>${rb.loginProfileError}</strong><br>${rb.loginProfileErrorSteps}</p>
        </div>
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