<#macro qrCode id content alt title="" label="">
  <div class="${properties.kcLoginQrCode!}">
      <img id="${id}" src="data:image/png;base64, ${content}" alt="${alt}">
      <#if title?has_content>
          <p class="${properties.kcLoginQrCodeTitle!}">${title}</p>
      </#if>
      <#if label?has_content>
          <p class="${properties.kcLoginMainFooterHelperText!}">${label}</p>
      </#if>
      <#nested>
  </div>
</#macro>
