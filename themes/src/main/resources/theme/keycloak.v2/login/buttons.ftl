<#macro actionGroup>
  <div class="${properties.kcFormGroupClass}">
    <div class="${properties.kcFormActionGroupClass}">
      <#nested>
    </div>
</#macro>

<#macro button id name label class=["kcButtonPrimaryClass"]>
  <button class="<#list class as c>${properties[c]} </#list>" name="${name}" id="${id}" type="submit">${msg(label)}</button>
</#macro>

<#macro loginButton>
  <@buttons.actionGroup>
    <@buttons.button id="kc-login" name="login" label="doLogIn" class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
  </@buttons.actionGroup>
</#macro>