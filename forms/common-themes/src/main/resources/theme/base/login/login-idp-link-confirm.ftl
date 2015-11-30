<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "title">
        ${msg("confirmLinkIdpTitle")}
    <#elseif section = "header">
         ${msg("confirmLinkIdpTitle")}
    <#elseif section = "form">
        <div id="kc-error-message">
            <p class="instruction">${message.summary}</p>
        </div>

        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

                <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" id="updateProfile" value="updateProfile">${msg("confirmLinkIdpReviewProfile")}</button>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" id="linkAccount" value="linkAccount">${msg("confirmLinkIdpContinue", idpAlias)}</button>
                </div>

        </form>
    </#if>
</@layout.registrationLayout>