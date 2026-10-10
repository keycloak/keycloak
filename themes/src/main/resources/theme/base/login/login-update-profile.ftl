<#import "template.ftl" as layout>
<#import "user-profile-commons.ftl" as userProfileCommons>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=true; section>
    <#if section = "header">
        ${msg("loginProfileTitle")}
    <#elseif section = "form">
        <form id="kc-update-profile-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <@userProfileCommons.userProfileFormFields/>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <@buttons.button label="doSubmit" fullWidth=false class=["kcButtonLargeClass"] value=msg("doSubmit") />
                        <@buttons.button name="cancel-aia" label="doCancel" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value="true" formnovalidate="formnovalidate" />
                    <#else>
                        <@buttons.button label="doSubmit" class=["kcButtonLargeClass"] value=msg("doSubmit") />
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
