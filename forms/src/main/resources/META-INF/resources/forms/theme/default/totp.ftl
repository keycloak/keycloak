<#import "template-main.ftl" as layout>
<@layout.mainLayout active='totp' bodyClass='totp'; section>

    <#if section = "header">

    Google Authenticator Setup

    <#elseif section = "content">

    <!--h:messages globalOnly="true" /-->
        <#if totp.enabled>
        Google Authenticator enabled
        <#else>
        <h2>Google Authenticator Setup</h2>

        <ol>
            <li>
                <p><strong>1</strong>Download the <a href="http://code.google.com/p/google-authenticator/" target="_blank">Google Authenticator app</a> in your device.</p>
            </li>
            <li class="clearfix">
                <p><strong>2</strong>Create an account in Google Authenticator and scan the barcode or the provided key below.</p>
                <img src="${totp.totpSecretQrCodeUrl}" alt="Figure: Barcode">
                <span class="code">${totp.totpSecretEncoded}</span>
            </li>
            <li class="clearfix">
                <p><strong>3</strong>Enter the one-time-password provided by Google Authenticator below and click Submit to finish the setup.</p>
                <form action="${url.totpUrl}" method="post">
                    <div class="form-group">
                        <label for="totp">${rb.getString('authenticatorCode')}</label>
                        <input type="text" id="totp" name="totp" />
                        <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="primary">Submit</button>
                    </div>
                </form>
            </li>
        </ol>
        </#if>

    </#if>
</@layout.mainLayout>