<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        {{applicationName}}
    <#elseif section = "form">
        <form id="kc-totp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input id="username" name="username" value="${login.username!''}" type="hidden" />
            <input id="password-token" name="password-token" value="${login.passwordToken!''}" type="hidden" />

            <div class="${properties.kcFormGroupClass!}">
                    <label for="totp" class="${properties.kcLabelClass!}">${rb.authenticatorCode}</label>&nbsp;
                    <input id="totp" name="totp" type="text" class="${properties.kcInputClass!}" />
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="btn btn-primary btn-lg cui-button cui-button-medium" name="login" id="kc-login" type="submit" value="${rb.logIn}"/>
                        <input class="btn btn-default btn-lg cui-button cui-button-medium" name="cancel" id="kc-cancel" type="submit" value="${rb.cancel}"/>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>