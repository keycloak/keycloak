<#macro kw>
  <div class="logo_container">
    <img style="max-height: 40px" id="logoutImage" class="ml-auto" src='data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg"
                width="1" height="1" />' />
    <#if client.attributes.logoUri??>
        <img style="max-height: 75px" class="mr-auto" src="${client.attributes.logoUri}"/>
    </#if>
  </div>
  <div class="font-bold text-2xl">
    <#nested>
  </div>
</#macro>
