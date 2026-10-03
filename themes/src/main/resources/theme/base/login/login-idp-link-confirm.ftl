<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("confirmLinkIdpTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <#if !hideReviewButton?has_content>
                    <@buttons.button name="submitAction" id="updateProfile" label="confirmLinkIdpReviewProfile" type="secondary" class=["kcButtonLargeClass"] value="updateProfile" />
                </#if>
                <@buttons.button name="submitAction" id="linkAccount" label="" labelText=msg("confirmLinkIdpContinue", idpDisplayName) type="secondary" class=["kcButtonLargeClass"] value="linkAccount" />
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
