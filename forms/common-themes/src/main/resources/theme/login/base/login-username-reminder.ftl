<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">
    ${rb.emailUsernameForgotHeader}

    <#elseif section = "header">
    ${rb.emailUsernameForgotHeader}

    <#elseif section = "form">
    <form id="kc-username-reminder-form" action="${url.loginUsernameReminderUrl}" method="post">
        <p class="instruction">${rb.emailUsernameInstruction}</p>
        <div class="field-wrapper">
            <label for="email">${rb.email}</label><input type="text" id="email" name="email" />
        </div>
        <input class="btn-primary" type="submit" value="${rb.submit}" />
    </form>
    <#elseif section = "info" >
    <p><a href="${url.loginUrl}">${rb.backToLogin}</a></p>
    </#if>
</@layout.registrationLayout>