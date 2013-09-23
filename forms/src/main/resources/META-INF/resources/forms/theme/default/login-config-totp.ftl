<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">

    Config TOTP

    <#elseif section = "header">

    Config TOTP

    <#elseif section = "form">

    <div name="form">
        <h2>To setup Google Authenticator</h2>

        <ol>
            <li>Install Google Authenticator to your device</li>
            <li>Set up an account in Google Authenticator and scan the QR code below or enter the key<br />
                <img src="${totp.totpSecretQrCodeUrl}" /> ${totp.totpSecretEncoded}
            </li>
            <li>Enter a one-time password provided by Google Authenticator and click Save to finish the setup

                <form action="${url.totpUrl}" method="post">
                    <div>
                        <label for="totp">${rb.getString('authenticatorCode')}</label>
                        <input type="text" id="totp" name="totp" />
                        <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
                    </div>

                    <input type="submit" value="Submit" />
                </form>
            </li>
        </ol>
    </div>

    <#elseif section = "info" >

    <div name="info">
    </div>

    </#if>
</@layout.registrationLayout>