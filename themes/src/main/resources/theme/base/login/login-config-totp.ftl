<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayRequiredFields=true; section>

    <#if section = "header">
        ${msg("loginTotpTitle")}

    <#elseif section = "form">

    <ol id="kc-totp-settings">
        <li>
            <p>${msg("loginTotpStep1")}</p>

            <ul id="kc-totp-supported-apps">
                <#list totp.policy.supportedApplications as app>
                <li>${app}</li>
                </#list>
            </ul>
        </li>

        <#if mode?? && mode = "manual">
            <li>
                <p>${msg("loginTotpManualStep2")}</p>
                <p><span id="kc-totp-secret-key">${totp.totpSecretEncoded}</span></p>
                <p><a href="${totp.qrUrl}" id="mode-barcode">${msg("loginTotpScanBarcode")}</a></p>
            </li>
            <li>
                <p>${msg("loginTotpManualStep3")}</p>
                <p>
                    <ul>
                        <li id="kc-totp-type">${msg("loginTotpType")}: ${msg("loginTotp." + totp.policy.type)}</li>
                        <li id="kc-totp-algorithm">${msg("loginTotpAlgorithm")}: ${totp.policy.getAlgorithmKey()}</li>
                        <li id="kc-totp-digits">${msg("loginTotpDigits")}: ${totp.policy.digits}</li>
                        <#if totp.policy.type = "totp">
                            <li id="kc-totp-period">${msg("loginTotpInterval")}: ${totp.policy.period}</li>
                        <#elseif totp.policy.type = "hotp">
                            <li id="kc-totp-counter">${msg("loginTotpCounter")}: ${totp.policy.initialCounter}</li>
                        </#if>
                    </ul>
                </p>
            </li>
        <#else>
            <li>
                <p>${msg("loginTotpStep2")}</p>
                <img id="kc-totp-secret-qr-code" src="data:image/png;base64, ${totp.totpSecretQrCode}" alt="Figure: Barcode"><br/>
                <p><a href="${totp.manualUrl}" id="mode-manual">${msg("loginTotpUnableToScan")}</a></p>
            </li>
        </#if>
        <li>
            <p>${msg("loginTotpStep3")}</p>
            <p>${msg("loginTotpStep3DeviceName")}</p>
        </li>
    </ol>

    <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-totp-settings-form" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <div class="${properties.kcInputWrapperClass!}">
                <label for="totp" class="control-label">${msg("authenticatorCode")}</label> <span class="required">*</span>
            </div>
            <div class="${properties.kcInputWrapperClass!}">
                <input type="text" id="totp" name="totp" autocomplete="off" class="${properties.kcInputClass!}" />
            </div>
            <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
            <#if mode??><input type="hidden" id="mode" name="mode" value="${mode}"/></#if>
        </div>

        <div class="${properties.kcFormGroupClass!}" ${messagesPerField.printIfExists('userLabel',properties.kcFormGroupErrorClass!)}">
            <div class="${properties.kcInputWrapperClass!}">
                <label for="userLabel" class="control-label">${msg("loginTotpDeviceName")}</label> <#if totp.otpCredentials?size gte 1><span class="required">*</span></#if>
            </div>

            <div class="${properties.kcInputWrapperClass!}">
                <input type="text" class="form-control" id="userLabel" name="userLabel" autocomplete="off">
            </div>
        </div>

        <#if isAppInitiatedAction??>
            <input type="submit"
                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                   id="saveTOTPBtn" value="${msg("doSubmit")}"
            />
            <button type="submit"
                    class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!} ${properties.kcButtonLargeClass!}"
                    id="cancelTOTPBtn" name="cancel-aia" value="true" />${msg("doCancel")}
            </button>
        <#else>
            <input type="submit"
                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                   id="saveTOTPBtn" value="${msg("doSubmit")}"
            />
        </#if>
    </form>
    </#if>
</@layout.registrationLayout>
