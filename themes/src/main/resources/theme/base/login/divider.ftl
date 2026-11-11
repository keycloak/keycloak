<#-- Use <@divider.or/> between alternative actions, or pass text to replace the default label. -->
<#macro or text=msg("orLabel")>
  <div class="${properties.kcLoginDivider!}">
      <span class="${properties.kcLoginMainFooterHelperText!}">
          ${text}
      </span>
  </div>
</#macro>
