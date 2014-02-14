<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${rb.loginTotpTitle}
    <#elseif section = "header">
        ${rb.loginTotpTitle}
    <#elseif section = "form">
        <form action="${url.loginUpdateTotpUrl}" class="${properties.kcFormClass!}" id="kc-totp-settings-form" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp" class="${properties.kcLabelClass!}">${rb.loginTotpOneTime}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="totp" name="totp" class="${properties.kcInputClass!}" />
                </div>
                <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="btn btn-primary btn-lg" type="submit" value="${rb.submit}"/>
                </div>
            </div>
        </form>
    <#elseif section = "info" >
        <ol id="kc-totp-settings">
            <li>
                <p>${rb.loginTotpStep1_1} <a href="http://code.google.com/p/google-authenticator/" target="_blank">${rb.loginTotpStep1_2}</a> ${rb.loginTotpStep1_3}</p>
            </li>
            <li>
                <p>${rb.loginTotpStep2}</p>
                <img src="${totp.totpSecretQrCodeUrl}" alt="Figure: Barcode"><br/>
                <span class="code">${totp.totpSecretEncoded}</span>
            </li>
        </ol>
    </#if>
</@layout.registrationLayout>
