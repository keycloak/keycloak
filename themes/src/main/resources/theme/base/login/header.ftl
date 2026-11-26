<#-- Override content in header.ftl to customize the brand area above the login card. -->
<#macro content brandName=(realm.displayNameHtml)!''>
  <div id="kc-header-wrapper"
          class="${properties.kcLoginBrand!}">${kcSanitize(msg("loginTitleHtml",brandName))?no_esc}</div>
</#macro>
