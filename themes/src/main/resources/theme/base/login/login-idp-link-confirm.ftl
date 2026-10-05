<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout; section>
<!-- template: login-idp-link-confirm.ftl -->
    <#if section = "header">
        ${msg("confirmLinkIdpTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <@buttons.actionGroup>
                <#if !hideReviewButton?has_content>
                    <@buttons.button name="submitAction" id="updateProfile" value="updateProfile" label="confirmLinkIdpReviewProfile" type="secondary"/>
                </#if>
                <@buttons.button name="submitAction" id="linkAccount" value="linkAccount" label="confirmLinkIdpContinue"/>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
