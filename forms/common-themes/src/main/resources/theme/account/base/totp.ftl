<#import "template.ftl" as layout>
<@layout.mainLayout active='totp' bodyClass='totp'; section>

    <#if totp.enabled>
        <h2>Authenticators</h2>

        <table class="table table-bordered table-striped">
            <thead
                <tr>
                   <th colspan="2">Configured Authenticators</th>
                </tr>
            </thead>
            <tbody>
            <tr>
                <td class="provider"><span class="social googleplus">FreeOTP</span></td>
                <td class="action">
                    <a id="remove-google" href="${url.totpRemoveUrl}"><i class="pficon pficon-delete"></i></a>
                </td>
            </tr>
            </tbody>
        </table>
    <#else>
        <h2>Google Authenticator Setup</h2>

        <hr/>

        <ol>
            <li>Download the <a href="https://fedorahosted.org/freeotp/" target="_blank">FreeOTP app</a> in your device.</li>
            <li>Create an account in FreeOTP and scan the barcode or type in the provided key below.<br/>
                <img src="${totp.totpSecretQrCodeUrl}" alt="Figure: Barcode"><br/>
                <span class="code">${totp.totpSecretEncoded}</span>
            </li>
            <li>Enter the one-time-password provided by FreeOTP below and click Submit to finish the setup.</li>
        </ol>

        <hr/>

        <form action="${url.totpUrl}" class="form-horizontal" method="post">
            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
            <div class="form-group">
                <div class="col-sm-2 col-md-2">
                    <label for="totp" class="control-label">${rb.authenticatorCode}</label>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="text" class="form-control" id="totp" name="totp" autofocus>
                    <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
                </div>
            </div>

            <div class="form-group">
                <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                    <div class="">
                        <button type="submit" class="btn btn-primary btn-lg" name="submitAction" value="Save">Save</button>
                        <button type="submit" class="btn btn-default btn-lg" name="submitAction" value="Cancel">Cancel</button>
                    </div>
                </div>
            </div>
        </form>
    </#if>

</@layout.mainLayout>