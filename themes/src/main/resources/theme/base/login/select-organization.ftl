<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form action="${url.loginAction}" class="form-vertical" method="post">
            <div id="kc-user-organizations" class="${properties.kcFormGroupClass!}">
                <h2>${msg("organization.select")}</h2>

                <ul class="${properties.kcFormSocialAccountListClass!} <#if user.organizations?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>">
                    <#list user.organizations as organization>
                        <li>
                            <a id="organization-${organization.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if user.organizations?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                               type="button" onclick="document.forms[0]['kc.org'].value = '${organization.alias}'; document.forms[0].requestSubmit()">
                                <span class="${properties.kcFormSocialAccountNameClass!}">${organization.name!}</span>
                            </a>
                        </li>
                    </#list>
                </ul>
            </div>
            <input type="hidden" name="kc.org"/>
        </form>
    </#if>

</@layout.registrationLayout>
