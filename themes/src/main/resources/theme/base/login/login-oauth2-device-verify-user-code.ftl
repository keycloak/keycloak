<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("oauth2DeviceVerificationTitle")}
    <#elseif section = "subHeader">
        <div class="${properties.kcLabelWrapperClass!}">
            <label for="device-user-code" class="${properties.kcFormLabelClass!}">${msg("verifyOAuth2DeviceUserCode")}</label>
        </div>
    <#elseif section = "errorDesc">
        ${msg("oauth2DeviceInvalidUserCodeTitle")}
    <#elseif section = "form">
    
        <form id="kc-user-verify-device-user-code-form" class="${properties.kcFormClass!}" action="${url.oauth2DeviceVerificationAction}" method="post">
            <div class="${properties.kcFormGroupClass!} pf-u-mt-lg">
                <div class="${properties.kcInputWrapperClass!}">
                    <label class=${properties.kcFormLabelClass}>
                        <span>${msg("oauth2DeviceCodeLabel")}</span>
                    </label>
                    <input id="device-user-code" name="device_user_code" autocomplete="off" type="text" class="${properties.kcInputClass!}" autofocus />
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!}" type="submit" value="${msg("doSubmit")}"/>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
