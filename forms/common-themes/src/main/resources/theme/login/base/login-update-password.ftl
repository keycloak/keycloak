<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
    ${rb.emailUpdateHeader}

    <#elseif section = "header">
    ${rb.emailUpdateHeader}

    <#elseif section = "form">
    <form id="kc-passwd-update-form" action="${url.loginUpdatePasswordUrl}" method="post">
        <div class="field-wrapper">
            <label for="password-new">${rb.passwordNew}</label><input type="password" id="password-new" name="password-new" />
        </div>
        <div class="field-wrapper">
            <label for="password-confirm" class="two-lines">${rb.passwordConfirm}</label><input type="password" id="password-confirm" name="password-confirm" />
        </div>

        <input class="btn-primary" type="submit" value="${rb.submit}" />
    </form>
    </#if>
</@layout.registrationLayout>