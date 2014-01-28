<#import "template.ftl" as layout>
<@layout.mainLayout active='totp' bodyClass='totp'; section>

    <h2 class="pull-left">
        <#if totp.enabled>
            Authenticators
        <#else>
            Google Authenticator Setup
        </#if>
    </h2>

    <#if totp.enabled>
        <form>
            <fieldset>
                <p class="info">You have the following authenticators set up:</p>
                <table class="list">
                    <caption>Table of social authenticators</caption>
                    <tbody>
                    <tr>
                        <td class="provider"><span class="social googleplus">Google</span></td>
                        <td class="action">
                            <a href="${url.totpRemoveUrl}" class="button">Remove Google</a>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <p class="info">
                    If the totp authentication is required by the realm and you remove your configured authenticator,
                    you will have to reconfigure it immediately or on the next login.
                </p>
            </fieldset>
            <div class="form-actions">
                <#if url.referrerURI??><a href="${url.referrerURI}">Back to application</a></#if>
            </div>
        </form>
    <#else>
        <ol>
            <li class="clearfix">
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
                        <label for="totp">${rb.authenticatorCode}</label>
                        <input type="text" id="totp" name="totp" />
                        <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
                    </div>
                    <div class="form-actions">
                        <#if url.referrerURI??><a href="${url.referrerURI}">Back to application</a></#if>
                        <button type="submit" class="primary">Submit</button>
                    </div>
                </form>
            </li>
        </ol>
    </#if>

</@layout.mainLayout>