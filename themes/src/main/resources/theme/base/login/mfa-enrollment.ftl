<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header" || section = "show-username">
        <script type="text/javascript">
            function fillAndSubmit(authExecId) {
                document.getElementById('mfa-method-hidden-input').value = authExecId;
                document.getElementById('kc-select-mfa-form').submit();
            }
        </script>
        <#if section = "header">
            ${msg("loginChooseMfa")}
        </#if>
    <#elseif section = "form">

        <form id="kc-select-mfa-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcSelectAuthListClass!}">
                <#list mfa.requiredActions as requiredAction>
                    <div class="${properties.kcSelectAuthListItemClass!}" onclick="fillAndSubmit('${requiredAction.providerId}')" id="mfa-${requiredAction.providerId}">

                        <div class="${properties.kcSelectAuthListItemIconClass!}">
                            <i class="${properties.kcSelectAuthListItemIconPropertyClass!}"></i>
                        </div>
                        <div class="${properties.kcSelectAuthListItemBodyClass!}">
                            <div class="${properties.kcSelectAuthListItemHeadingClass!}">
                                ${msg('requiredAction.${requiredAction.providerId}')}
                            </div>
                            <div class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                ${msg('requiredAction.${requiredAction.providerId}-help-text')}
                            </div>
                        </div>
                        <div class="${properties.kcSelectAuthListItemFillClass!}"></div>
                        <div class="${properties.kcSelectAuthListItemArrowClass!}">
                            <i class="${properties.kcSelectAuthListItemArrowIconClass!}"></i>
                        </div>
                    </div>
                </#list>
                <input type="hidden" id="mfa-method-hidden-input" name="mfaMethod" />
            </div>
        </form>

    </#if>
</@layout.registrationLayout>

