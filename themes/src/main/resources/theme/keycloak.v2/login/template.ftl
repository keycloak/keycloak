<#import "field.ftl" as field>
<#import "footer.ftl" as loginFooter>
<#import "theme-resources.ftl" as themeResourceTags>
<#macro username>
  <#assign label>
    <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
  </#assign>
  <@field.group name="username" label=label>
    <div class="${properties.kcInputGroup}">
      <div class="${properties.kcInputGroupItemClass} ${properties.kcFill}">
        <span class="${properties.kcInputClass} ${properties.kcFormReadOnlyClass}">
          <input id="kc-attempted-username" value="${auth.attemptedUsername}" readonly>
        </span>
      </div>
      <div class="${properties.kcInputGroupItemClass}">
        <button id="reset-login" class="${properties.kcFormPasswordVisibilityButtonClass} kc-login-tooltip" type="button" 
              aria-label="${msg('restartLoginTooltip')}" onclick="location.href='${url.loginRestartFlowUrl}'">
            <i class="fa-sync-alt fas" aria-hidden="true"></i>
            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
        </button>
      </div>
    </div>
  </@field.group>
</#macro>

<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${lang}"<#if realm.internationalizationEnabled> dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="color-scheme" content="light${darkMode?then(' dark', '')}">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${title!}</title>
    <#if themeResources?? && themeResources.favicons?has_content>
        <@themeResourceTags.renderFavicons themeResources.favicons url.resourcesPath />
    <#else>
        <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    </#if>
    <#if themeResources?? && themeResources.stylesCommon?has_content>
        <@themeResourceTags.renderStyles themeResources.stylesCommon url.resourcesCommonPath />
    <#elseif properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if themeResources?? && themeResources.styles?has_content>
        <@themeResourceTags.renderStyles themeResources.styles url.resourcesPath />
    <#elseif properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <#if darkMode>
      <script type="module" async blocking="render">
          <#outputformat "JavaScript">
          const DARK_MODE_CLASS = ${properties.kcDarkModeClass?c};
          const STORAGE_KEY = "fidar-color-scheme";
          const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

          // An explicit choice from the toggle outranks the system setting.
          // Storage can throw when cookies are blocked, so it is never trusted
          // to be available.
          function storedScheme() {
            try {
              return localStorage.getItem(STORAGE_KEY);
            } catch (error) {
              return null;
            }
          }

          const stored = storedScheme();
          updateDarkMode(stored ? stored === "dark" : mediaQuery.matches);

          mediaQuery.addEventListener("change", (event) => {
            // Only follow the system once the user has not picked a side.
            if (!storedScheme()) {
              updateDarkMode(event.matches);
            }
          });

          function updateDarkMode(isEnabled) {
            const { classList } = document.documentElement;

            if (isEnabled) {
              classList.add(DARK_MODE_CLASS);
            } else {
              classList.remove(DARK_MODE_CLASS);
            }
          }
          </#outputformat>
      </script>
    </#if>
    <#if themeResources?? && themeResources.scripts?has_content>
        <@themeResourceTags.renderScripts themeResources.scripts url.resourcesPath "text/javascript" />
    <#elseif properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    <script type="module" src="${url.resourcesPath}/js/login-enhancements.js"></script>
    <script type="module">
        <#outputformat "JavaScript">
        import { startSessionPolling } from ${(url.resourcesPath + "/js/authChecker.js")?c};

        startSessionPolling(
            ${url.ssoLoginInOtherTabsUrl?c}
        );
        </#outputformat>
    </script>
    <script type="module">
        document.addEventListener("click", (event) => {
            const link = event.target.closest("a[data-once-link]");

            if (!link) {
                return;
            }

            if (link.getAttribute("aria-disabled") === "true") {
                event.preventDefault();
                return;
            }

            const { disabledClass } = link.dataset;

            if (disabledClass) {
                link.classList.add(...disabledClass.trim().split(/\s+/));
            }

            link.setAttribute("role", "link");
            link.setAttribute("aria-disabled", "true");
        });
    </script>
    <#if authenticationSession??>
        <script type="module">
             <#outputformat "JavaScript">
            import { checkAuthSession } from ${(url.resourcesPath + "/js/authChecker.js")?c};

            checkAuthSession(
                ${authenticationSession.authSessionIdHash?c}
            );
            </#outputformat>
        </script>
    </#if>
    <script>
      // Workaround for https://bugzilla.mozilla.org/show_bug.cgi?id=1404468
      const isFirefox = true;
    </script>
</head>

<#-- Fidar: info.ftl renders a "click here to proceed" interstitial while a
     required action is outstanding. For Quantum Pass enrolment that page only
     asks the user to confirm something they already chose, so hand the target
     to login-enhancements.js and let it go straight there. -->
<#assign fidarSkipActionUri = "">
<#if actionUri?? && actionUri?has_content && requiredActions??>
    <#list requiredActions as fidarReqAction>
        <#if fidarReqAction == "webauthn-register" || fidarReqAction == "webauthn-register-passwordless">
            <#assign fidarSkipActionUri = actionUri>
            <#break>
        </#if>
    </#list>
</#if>

<body id="keycloak-bg" class="${properties.kcBodyClass!}" data-page-id="login-${pageId}" data-capslock-text="${msg('capsLockOn')}" data-dark-class="${properties.kcDarkModeClass!}" data-realm="${realm.name!}"<#if fidarSkipActionUri?has_content> data-skip-to-action="${fidarSkipActionUri}"</#if>>
<#if darkMode>
  <button type="button" id="kc-theme-toggle" class="kc-theme-toggle"
          aria-label="${msg('toggleColorScheme')}" title="${msg('toggleColorScheme')}">
    <span class="kc-theme-toggle__sun" aria-hidden="true"></span>
    <span class="kc-theme-toggle__moon" aria-hidden="true"></span>
  </button>
</#if>
<div class="${properties.kcLogin!}">
  <div class="${properties.kcLoginContainer!}">
    <header id="kc-header" class="pf-v5-c-login__header">
      <#-- Always the Fidar mark, never the realm's name.

           Upstream renders msg("loginTitleHtml", realm.displayNameHtml) here,
           and RealmBean falls back displayNameHtml -> displayName -> the realm's
           internal *name*. None of our realms set a display name, so this
           rendered raw identifiers such as "FIDAR_WEBAUTH_V2" as the masthead of
           the sign-in page.

           Per-tenant identity is not this element's job — that is the logo below
           inside the card, resolved per realm from the branding API. This slot is
           the product's own mark and is the same on every realm.

           The mark is drawn as a background image on .kc-logo-text (see
           --keycloak-logo-url) rather than an <img>, so it follows the light/dark
           toggle, which flips a class and would not trigger a <picture> element's
           prefers-color-scheme sources. The span is the logo's accessible name
           and is clipped, not hidden, by the stylesheet. -->
      <div id="kc-header-wrapper" class="pf-v5-c-brand">
        <div class="kc-logo-text"><span>Fidar</span></div>
      </div>
      <#-- dir="auto" so each string takes direction from its own content: these
           fall back to English on locales without a translation, and Latin text
           inside an RTL page otherwise has its trailing punctuation relocated. -->
      <div class="kc-brand-copy">
        <h2 class="kc-brand-headline" dir="auto">${msg("brandHeadline")}</h2>
        <p class="kc-brand-tagline" dir="auto">${msg("brandTagline")}</p>
      </div>
    </header>
    <main class="${properties.kcLoginMain!}">
      <#-- Tenant logo, resolved at runtime from the branding API. Starts hidden
           and is only revealed once an image actually loads, so realms without
           one show no gap and a failed request degrades to the Fidar mark. -->
      <div id="kc-realm-logo" class="kc-realm-logo" hidden>
        <img id="kc-realm-logo-img" src="" alt="" />
      </div>
      <div class="${properties.kcLoginMainHeader!}">
        <h1 class="${properties.kcLoginMainTitle!}" id="kc-page-title"><#nested "header"></h1>
        <#if realm.internationalizationEnabled  && locale.supported?size gt 1>
        <div class="${properties.kcLoginMainHeaderUtilities!}">
          <div class="${properties.kcInputClass!}">
            <select
              aria-label="${msg("languages")}"
              id="login-select-toggle"
              onchange="if (this.value) window.location.href=this.value"
            >
              <#list locale.supported?sort_by("label") as l>
                <option
                  value="${l.url}"
                  ${(l.languageTag == locale.currentLanguageTag)?then('selected','')}
                >
                  ${l.label}
                </option>
              </#list>
            </select>
            <span class="${properties.kcFormControlUtilClass}">
              <span class="${properties.kcFormControlToggleIcon!}">
                <svg
                  class="pf-v5-svg"
                  viewBox="0 0 320 512"
                  fill="currentColor"
                  aria-hidden="true"
                  role="img"
                  width="1em"
                  height="1em"
                >
                  <path
                    d="M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z"
                  >
                  </path>
                </svg>
              </span>
            </span>
          </div>
        </div>
        </#if>
      </div>
      <div class="${properties.kcLoginMainBody!}">
        <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
            <#if displayRequiredFields>
                <div class="${properties.kcContentWrapperClass!}">
                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                        <span class="${properties.kcInputHelperTextItemTextClass!}">
                          <span class="${properties.kcInputRequiredClass!}">*</span> ${msg("requiredFields")}
                        </span>
                    </div>
                </div>
            </#if>
        <#else>
            <#if displayRequiredFields>
                <div class="${properties.kcContentWrapperClass!}">
                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                        <span class="${properties.kcInputHelperTextItemTextClass!}">
                          <span class="${properties.kcInputRequiredClass!}">*</span> ${msg("requiredFields")}
                        </span>
                    </div>
                    <div class="${properties.kcFormClass} ${properties.kcContentWrapperClass}">
                        <#nested "show-username">
                        <@username />
                    </div>
                </div>
            <#else>
                <div class="${properties.kcFormClass} ${properties.kcContentWrapperClass}">
                  <#nested "show-username">
                  <@username />
                </div>
            </#if>
        </#if>

        <#-- App-initiated actions should not see warning messages about the need to complete the action -->
        <#-- during login.                                                                               -->
        <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="${properties.kcAlertClass!} pf-m-${(message.type = 'error')?then('danger', message.type)}">
                <div class="${properties.kcAlertIconClass!}">
                    <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                    <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                    <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                    <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                </div>
                <span class="${properties.kcAlertTitleClass!} kc-feedback-text">${message.summary}</span>
            </div>
        </#if>

        <#nested "form">

        <#-- OAuth 2.0 device grant: the browser leg is finished, so hand control
             back to the app that started it. A Chrome Custom Tab cannot close
             itself, so without this the user is stranded on a success page with no
             route back to the app — the Keycloakify build used to bounce them.

             Matched on the server-resolved header rather than on the page's text,
             so the check holds in every locale, and it fails closed: if that
             message key ever moves upstream the block simply stops rendering and
             the page reads exactly as it does today.

             The link is rendered even though the redirect is automatic. Navigating
             to a custom scheme is silently ignored when nothing has registered it
             (a desktop browser, or the app not installed), and there is no event to
             detect that, so the manual route has to be on the page already. -->
        <#if pageId == "info" && messageHeader?? && messageHeader == msg("oauth2DeviceVerificationCompleteHeader")
             && properties.fidarDeviceCallbackUri?has_content>
          <div id="kc-device-callback" class="kc-device-callback" data-callback-uri="${properties.fidarDeviceCallbackUri}">
            <a href="${properties.fidarDeviceCallbackUri}"
               class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}">${msg("backToApplication")}</a>
          </div>
        </#if>

        <#if auth?has_content && auth.showTryAnotherWayLink()>
          <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post" novalidate="novalidate">
              <input type="hidden" name="tryAnotherWay" value="on"/>
              <a id="try-another-way" href="javascript:document.forms['kc-select-try-another-way-form'].requestSubmit()"
                  class="${properties.kcButtonSecondaryClass} ${properties.kcButtonBlockClass} ${properties.kcMarginTopClass}">
                    ${msg("doTryAnotherWay")}
              </a>
          </form>
        </#if>

        <#if switchOrganizationEnabled?? && switchOrganizationEnabled>
          <form id="kc-switch-organization-form" action="${url.loginAction}" method="post" novalidate="novalidate">
              <input type="hidden" name="switchOrganization" value="true"/>
              <a id="switch-organization" href="javascript:document.forms['kc-switch-organization-form'].requestSubmit()"
                  class="${properties.kcButtonSecondaryClass} ${properties.kcButtonBlockClass} ${properties.kcMarginTopClass}">
                    ${msg("doSwitchOrganization")}
              </a>
          </form>
        </#if>

          <div class="${properties.kcLoginMainFooter!}">
              <#nested "socialProviders">

              <#if displayInfo>
                  <div id="kc-info" class="${properties.kcLoginMainFooterBand!} ${properties.kcFormClass}">
                      <div id="kc-info-wrapper" class="${properties.kcLoginMainFooterBandItem!}">
                          <#nested "info">
                      </div>
                  </div>
              </#if>
          </div>
      </div>

        <div class="${properties.kcLoginMainFooter!}">
            <@loginFooter.content/>
        </div>
    </main>
  </div>
</div>
</body>
</html>
</#macro>
