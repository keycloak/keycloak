<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "header">
        ${msg("oauth2DeviceConfirmTitle")}
    <#elseif section = "form">
        <div id="kc-oauth" class="content-area">
            <p>${msg("oauth2DeviceConfirmMessage", deviceConfirm.clientName)}</p>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label class="${properties.kcLabelClass!}">${msg("oauth2DeviceConfirmUserCode")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <p><strong>${deviceConfirm.userCode}</strong></p>
                </div>
            </div>

            <form class="form-actions" action="${url.loginAction}" method="POST">
                <input type="hidden" name="session_code" value="${deviceConfirm.code}"/>
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-options">
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                        </div>
                    </div>

                    <div id="kc-form-buttons">
                        <div class="${properties.kcFormButtonsWrapperClass!}">
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-confirm" type="submit" value="${msg("oauth2DeviceConfirmProceed")}"/>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("oauth2DeviceConfirmCancel")}"/>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>
