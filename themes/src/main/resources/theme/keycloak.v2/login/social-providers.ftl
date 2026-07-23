<#macro show social>
  <#assign useGrid = (social.providers?size gt 3)>
  <div class="${properties.kcLoginSocialProvidersDivider!}">
      <span class="${properties.kcLoginMainFooterHelperText!}">
          <#if useGrid>
              ${msg("identity-provider-login-label-divider")}
          <#else>
              ${msg("identity-provider-login-label-short")}
          </#if>
      </span>
  </div>
  <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
      <ul class="${properties.kcFormSocialAccountListClass!} <#if useGrid>${properties.kcFormSocialAccountListGridClass!}</#if>">
          <#list social.providers as p>
              <li class="<#if useGrid>${properties.kcFormSocialAccountGridItem!}<#else>${properties.kcFormSocialAccountListItemClass!}</#if>">
                  <a data-once-link data-disabled-class="${properties.kcFormSocialAccountListButtonDisabledClass!}" id="social-${p.alias}"
                          class="${properties.kcFormSocialAccountListButtonClass!}" aria-label="<#if useGrid>${p.displayName!}<#else>${msg("signInWithProvider", p.displayName!)}</#if>"
                          type="button" href="${p.loginUrl}">
                        <#if p.iconClasses?has_content>
                            <span class="${p.iconClasses!}">
                                <#if useGrid>
                                    ${p.displayName!}
                                <#else>
                                    ${msg("signInWithProvider", p.displayName!)}
                                </#if>
                            </span>
                        <#else>
                        <#switch p.providerId>
                            <#case "github">
                            <#case "instagram">
                            <#case "twitter">
                                <#if darkMode>
                                    <picture>
                                        <source srcset="${url.resourcesPath}/img/social/${p.providerId}-light.svg" media="(prefers-color-scheme: dark)">
                                        <img src="${url.resourcesPath}/img/social/${p.providerId}.svg" aria-hidden="true" alt="">
                                    </picture>
                                <#else>
                                    <img src="${url.resourcesPath}/img/social/${p.providerId}.svg" aria-hidden="true" alt="">
                                </#if>
                                <#break>
                            <#case "google">
                            <#case "facebook">
                            <#case "gitlab">
                            <#case "linkedin-openid-connect">
                            <#case "microsoft">
                            <#case "bitbucket">
                            <#case "stackoverflow">
                            <#case "paypal">
                            <#case "openshift-v4">
                                <img src="${url.resourcesPath}/img/social/${p.providerId}.svg" aria-hidden="true" alt="">
                                <#break>
                            <#default>
                                <#if darkMode>
                                    <picture>
                                        <source srcset="${url.resourcesPath}/img/social/default-light.svg" media="(prefers-color-scheme: dark)">
                                        <img src="${url.resourcesPath}/img/social/default.svg" aria-hidden="true" alt="">
                                    </picture>
                                <#else>
                                    <img src="${url.resourcesPath}/img/social/default.svg" aria-hidden="true" alt="">
                                </#if>
                        </#switch>
                            <span class="${properties.kcFormSocialAccountNameClass!}"><#if useGrid>${p.displayName!}<#else>${msg("signInWithProvider", p.displayName!)}</#if></span>
                        </#if>
                  </a>
              </li>
          </#list>
      </ul>
  </div>
</#macro>