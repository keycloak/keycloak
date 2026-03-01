<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>
<#import "user-profile-commons.ftl" as userProfileCommons>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=false; section>
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

                <@buttons.actionGroup horizontal=true>
                    <@buttons.button id="kc-submit" label="doSubmit" class=["kcButtonPrimaryClass","kcButtonBlockClass"]/>
                    <#if isAppInitiatedAction??>
                        <@buttons.button id="kc-cancel" label="doCancel" class=["kcButtonSecondaryClass","kcButtonBlockClass"] name="cancel-aia" value="true"/>
                    </#if>
                </@buttons.actionGroup>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
