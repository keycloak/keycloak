<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("loginTotpTitle")}
    <#elseif section = "header">
        ${msg("loginTotpTitle")}
    <#elseif section = "form">
<ol id="kc-totp-settings">
    <li>
        <p>${msg("loginTotpStep1")}</p>
        </li>
    <li>
        <p>${msg("loginTotpStep2")}</p>
        <img id="kc-totp-secret-qr-code" src="data:image/png;base64, ${totp.totpSecretQrCode}" alt="Figure: Barcode"><br/>
        <span class="code">${totp.totpSecretEncoded}</span>
        </li>
    <li>
        <p>${msg("loginTotpStep3")}</p>
        </li>
    </ol>
    <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-totp-settings-form" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <div class="${properties.kcInputWrapperClass!}">
                <input type="text" id="totp" name="totp" autocomplete="off" class="${properties.kcInputClass!}" />
            </div>
            <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
        </div>

        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
    </form>
    </#if>
</@layout.registrationLayout>
