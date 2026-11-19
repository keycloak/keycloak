<#-- Override content in header.ftl to customize the brand area above the login card. -->
<#macro content>
  <div id="kc-header-wrapper"
          class="${properties.kcLoginBrand!}">${kcSanitize(msg("loginTitleHtml",(realm.displayNameHtml!'')))?no_esc}</div>
</#macro>
