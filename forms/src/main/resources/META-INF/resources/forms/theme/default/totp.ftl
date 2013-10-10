<#import "template-main.ftl" as layout>
<@layout.mainLayout active='totp' bodyClass='totp'; section>

    <#if section = "header">

        <#if totp.enabled>
            <h2>Authenticators</h2>
        <#else>
            <h2>Google Authenticator Setup</h2>
        </#if>

    <#elseif section = "content">

        <#if totp.enabled>
        <#-- TODO this is only mock page -->
        <form>
            <fieldset>
                <p class="info">You have the following authenticators set up:</p>
                <table class="list">
                    <caption>Table of social authenticators</caption>
                    <tbody>
                    <tr>
                        <td class="provider"><span class="social googleplus">Google</span></td>
                        <td class="soft">Connected as john@google.com</td>
                        <td class="action">
                            <a href="${url.totpRemoveUrl}" class="button">Remove Google</a>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </fieldset>
        </form>

        <#else>
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