<#-- Render OTP credentials as single-select cards; the first one is preselected when none matches selectedId. -->
<#macro cards credentials selectedId="">
    <#local hasSelected = credentials?filter(c -> c.id == selectedId)?has_content>
    <#list credentials as credential>
        <@card credential=credential index=credential?index selected=(credential.id == selectedId || (!hasSelected && credential?is_first)) />
    </#list>
</#macro>

<#macro card credential index selected>
    <div class="${properties.kcOtpCredentialCardClass!}">
        <div class="${properties.kcOtpCredentialHeaderClass!}">
            <div class="${properties.kcOtpCredentialActionsClass!}">
                <div class="${properties.kcOtpCredentialSelectableActionsClass!}">
                    <div class="${properties.kcOtpCredentialRadioClass!}">
                        <input id="kc-otp-credential-${index}" class="${properties.kcOtpCredentialRadioInputClass!}" type="radio" name="selectedCredentialId" value="${credential.id}" aria-labelledby="kc-otp-credential-title-${index}" <#if selected>checked="checked"</#if>>
                        <label for="kc-otp-credential-${index}" class="${properties.kcOtpCredentialRadioLabelClass!}"></label>
                    </div>
                </div>
            </div>
            <div class="${properties.kcOtpCredentialHeaderMainClass!}">
                <div class="${properties.kcOtpCredentialTitleClass!}">
                    <span id="kc-otp-credential-title-${index}" class="${properties.kcOtpCredentialTitleTextClass!}">
                        <i class="${properties.kcLoginOTPListItemIconClass!}" aria-hidden="true"></i>
                        <#if credential.userLabel?has_content>
                            ${credential.userLabel}
                        <#else>
                            <#-- Label to be shown in the UI for the "unnamed" OTP credential, which doesn't have userLabel -->
                            ${msg("defaultOtpAuth")}
                        </#if>
                    </span>
                </div>
            </div>
        </div>
    </div>
</#macro>
