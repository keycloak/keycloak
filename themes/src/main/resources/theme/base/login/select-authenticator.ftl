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
                <div role="button" class="${properties.kcSelectAuthListItemClass!}" tabindex="0"
                     onclick="this.closest('li').querySelector('form').requestSubmit()"
                     onkeydown="if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); this.click(); }">
                    <div class="${properties.kcSelectAuthListItemContentClass!}">
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

