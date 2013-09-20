<#import "template-main.ftl" as layout>
<@layout.mainLayout ; section>

    <#if section = "header">

    Google Authenticator Setup

    <#elseif section = "content">

    <!--h:messages globalOnly="true" /-->
        <#if totp.enabled>
        Google Authenticator enabled
        <#else>
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

                    <input type="button" value="Cancel" />
                    <input type="submit" value="Save" />
                </form>
            </li>
        </ol>
        </#if>

    </#if>
</@layout.mainLayout>