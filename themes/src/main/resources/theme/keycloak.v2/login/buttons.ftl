<#--
  @param horizontal - if true, buttons are placed on the same row sharing the full width
-->
<#macro actionGroup horizontal=false>
  <div class="${properties.kcFormGroupClass}">
    <div class="${properties.kcFormActionGroupClass} <#if horizontal>pf-v5-u-flex-nowrap<#else>pf-v5-u-flex-wrap</#if>">
      <#nested>
    </div>
  </div>
</#macro>

<#--
  @param label            - message key for the button text
  @param id               - HTML id attribute
  @param name             - HTML name attribute
  @param type             - button style: "primary", "secondary", etc. (resolves to kcButton{Type}Class property)
  @param fullWidth        - if true, adds the block class for full-width buttons
  @param class            - list of theme property keys to append (e.g. ["kcButtonLargeClass"])
  @param extraClass       - list of raw CSS classes to append (e.g. ["g-recaptcha"])
  @param specialAttributes - hash of arbitrary HTML attributes, supports hyphenated keys (e.g. {"data-sitekey": value})
  @param extra            - additional simple (non-hyphenated) HTML attributes passed as named parameters
-->
<#macro button label id="" name="" type="primary" fullWidth=true class=[] extraClass=[] specialAttributes={} extra...>
  <button class="${properties['kcButton' + type?cap_first + 'Class']}<#if fullWidth> ${properties.kcButtonBlockClass}</#if><#list class as c> ${properties[c]}</#list><#list extraClass as c> ${c}</#list>" name="${name}" id="${id}"
          type="submit"<#list extra as attrName, attrVal> ${attrName}="${attrVal}"</#list><#list specialAttributes as key, val> ${key}="${val}"</#list>>
  ${msg(label)}
  </button>
</#macro>

<#macro buttonLink href label id="" class=["kcButtonSecondaryClass", "kcButtonBlockClass"]>
  <a id="${id}" href="${href}" class="<#list class as c>${properties[c]} </#list>">${msg(label)}</a>
</#macro>

<#macro loginButton>
  <@buttons.actionGroup>
    <@buttons.button id="kc-login" name="login" label="doLogIn" />
  </@buttons.actionGroup>
</#macro>