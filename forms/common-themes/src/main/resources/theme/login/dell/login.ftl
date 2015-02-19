<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        <#if client.application>
             Single Sign On (SSO)
        <#elseif client.oauthClient>
             ${realm.name} ${rb.loginOauthTitle}
        </#if>
    <#elseif section = "form">        
        <#if realm.password>
        <form id='kc-form-login' novalidate
              name='emailForm'
              class='sign-in ${properties.kcFormClass!}'
              ng-submit="emailSignInClick()"
              ng-controller="EmailFormController as email">
            <div class='username'>
              <cui-text-box required
                name='emailAddress'
                class='username-textbox'
                type='email'
                ng-model-options="{ debounce: 500 }"
                ng-model='email.emailAddress'
                ng-change='email.onEmailAddressChange()'
                placeholder='someone@yourcompany.com'></cui-text-box>
              <input class='cui-button cui-button-medium cui-button-primary' ng-disabled='emailForm.$invalid'  value='Sign in' type='submit' ng-click='email.emailSignInClick()'>
            </div>
        </form>
        </#if>
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed>
            <div id="kc-registration">
                <span>${rb.noAccount} <a href="${url.registrationUrl}">${rb.register}</a></span>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
