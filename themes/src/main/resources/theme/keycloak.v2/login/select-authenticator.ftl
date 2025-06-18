<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
<!-- template: select-authenticator.ftl -->

    <#if section = "header" || section = "show-username">
        <#if section = "header">
            ${msg("loginChooseAuthenticator")}
        </#if>
    <#elseif section = "form">

    <ul class="${properties.kcSelectAuthListClass!}" role="list">
        <#list auth.authenticationSelections as authenticationSelection>
            <li class="${properties.kcSelectAuthListItemWrapperClass!}">
                <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                    <input type="hidden" name="authenticationExecution" value="${authenticationSelection.authExecId}">
                </form>
                <div class="${properties.kcSelectAuthListItemClass!}" onclick="document.forms[${authenticationSelection?index}].requestSubmit()">
                    <div class="pf-v5-c-data-list__item-content">
                        <div class="${properties.kcSelectAuthListItemIconClass!}">
                            <i class="${properties['${authenticationSelection.iconCssClass}']!authenticationSelection.iconCssClass} ${properties.kcSelectAuthListItemIconPropertyClass!}"></i>
                        </div>
                        <div class="${properties.kcSelectAuthListItemBodyClass!}">
                            <h2 class="${properties.kcSelectAuthListItemHeadingClass!}">
                                ${msg('${authenticationSelection.displayName}')}
                            </h2>
                        </div>
                        <div class="${properties.kcSelectAuthListItemDescriptionClass!}">
                            ${msg('${authenticationSelection.helpText}')}
                        </div>
                    </div>
                    <div class="${properties.kcSelectAuthListItemFillClass!}">
                        <i class="${properties.kcSelectAuthListItemArrowIconClass!}" aria-hidden="true"></i>
                    </div>
                </div>
            </li>
        </#list>
    </ul>

    </#if>
</@layout.registrationLayout>

