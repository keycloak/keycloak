<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>
<#import "buttons.ftl" as buttons>
<#import "user-profile-commons.ftl" as userProfileCommons>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=true; section>
    <#if section = "header">
        ${msg("updateEmailTitle")}
    <#elseif section = "form">
        <form id="kc-update-email-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <@userProfileCommons.userProfileFormFields/>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <@passwordCommons.logoutOtherSessions/>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <@buttons.button label="doSubmit" fullWidth=false class=["kcButtonLargeClass"] value=msg("doSubmit") />
                        <@buttons.button name="cancel-aia" label="doCancel" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value="true" />
                    <#else>
                        <@buttons.button label="doSubmit" class=["kcButtonLargeClass"] value=msg("doSubmit") />
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
