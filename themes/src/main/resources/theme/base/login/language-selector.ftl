<#-- Render the locale dropdown with <@languageSelector.select/> when multiple locales are enabled. -->
<#macro select>
  <#if ((realm.internationalizationEnabled)!false) && locale.supported?size gt 1>
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
        <span class="${properties.kcFormControlUtilClass!}">
          <span class="${properties.kcFormControlToggleIcon!}">
            <svg
              class="${properties.kcSvgIconClass!}"
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
</#macro>
