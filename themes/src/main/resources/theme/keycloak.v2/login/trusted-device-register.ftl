<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<#import "field.ftl" as field>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        ${msg("trustedDeviceHeader")}
    <#elseif section="form">
        <form class="${properties.kcFormClass}" action="${url.loginAction}" method="POST">
            <@field.group name="trusted-device-name-group" label=msg("deviceNameLabel")>
                <div class="${properties.kcInputWrapperClass!}">
                    <p id="kc-trusted-device-name" class="${properties.kcFormHelperTextClass!}">${deviceName}</p>
                </div>
            </@field.group>
            <@buttons.actionGroup>
                <@buttons.button id="kc-trusted-device-yes" name="trusted-device" label="doYes" value="yes"/>
                <@buttons.button id="kc-trusted-device-no" name="trusted-device" label="doNo" value="no" class=["kcButtonSecondaryClass"]/>
            </@buttons.actionGroup>
        </form>
    <#elseif section="info">
        <div id="kc-form-help-text-after-trusted-device" class="${properties.kcLoginMainFooterHelperText!}" aria-live="polite">
            ${msg("trustedDeviceExplanation")}
        </div>
    </#if>
</@layout.registrationLayout>
