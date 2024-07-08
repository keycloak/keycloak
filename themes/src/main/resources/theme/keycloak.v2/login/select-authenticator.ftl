<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header" || section = "show-username">
        <#if section = "header">
            ${msg("loginChooseAuthenticator")}
        </#if>
    <#elseif section = "form">

        <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <ul class="pf-v5-c-data-list" role="list">
                <#list auth.authenticationSelections as authenticationSelection>
                <li class="pf-v5-c-data-list__item pf-m-clickable">
                    <div class="pf-v5-c-data-list__item-row" onclick="document.forms[0].submit()">
                        <div class="pf-v5-c-data-list__item-content">
                            <div class="pf-v5-c-data-list__cell pf-m-icon">
                                <i class="${properties['${authenticationSelection.iconCssClass}']!authenticationSelection.iconCssClass} ${properties.kcSelectAuthListItemIconPropertyClass!}"></i>
                            </div>
                            <div class="pf-v5-c-data-list__cell pf-m-no-fill">
                                <h2 class="pf-v5-u-font-family-heading">${msg('${authenticationSelection.displayName}')}</h2>
                            </div>
                            <div class="pf-v5-c-data-list__cell pf-m-no-fill">
                                ${msg('${authenticationSelection.helpText}')}
                            </div>
                        </div>
                        <div class="pf-v5-c-data-list__item-action">
                            <i class="${properties.kcSelectAuthListItemArrowIconClass!}" aria-hidden="true"></i>
                        </div>
                    </div>
                </li>
            </#list>
            </ul>
        </form>

    </#if>
</@layout.registrationLayout>

