<#macro actionGroup>
  <div class="${properties.kcFormGroupClass}">
    <div class="${properties.kcFormActionGroupClass}">
      <#nested>
    </div>
  </div>
</#macro>

<#macro button label id="" name="" class=["kcButtonPrimaryClass"]>
  <button class="<#list class as c>${properties[c]} </#list>" name="${name}" id="${id}" type="submit">${msg(label)}</button>
</#macro>

<#macro buttonLink href label id="" class=["kcButtonSecondaryClass"]>
  <a id="${id}" href="${href}" class="<#list class as c>${properties[c]} </#list>">${kcSanitize(msg(label))?no_esc}</a>
</#macro>

<#macro loginButton>
  <@buttons.actionGroup>
    <@buttons.button id="kc-login" name="login" label="doLogIn" class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
  </@buttons.actionGroup>
</#macro>