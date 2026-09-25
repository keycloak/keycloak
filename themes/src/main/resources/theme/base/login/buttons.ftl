<#macro actionGroup horizontal=false id="" group=true>
  <#if group><div class="${properties.kcFormGroupClass!}"></#if>
    <div<#if id?has_content> id="${id}"</#if> class="${properties.kcFormActionGroupClass!(properties.kcFormButtonsClass!'')}<#if horizontal> ${properties.kcFormActionGroupHorizontalClass!}<#else> ${properties.kcFormActionGroupVerticalClass!}</#if>">
      <#nested>
    </div>
  <#if group></div></#if>
</#macro>

<#macro button label id="" name="" type="primary" fullWidth=true class=[] className="" labelText="" attributes={} extra...>
  <#local variantClass = properties['kcButton' + type?cap_first + 'Class']!>
  <#if type == "secondary" && !properties.kcButtonSecondaryClass??>
    <#local variantClass = properties.kcButtonDefaultClass!>
  </#if>
  <button class="${properties.kcButtonClass!} ${variantClass}<#if fullWidth> ${properties.kcButtonBlockClass!}</#if><#list class as c> ${properties[c]!}</#list><#if className?has_content> ${className}</#if>" name="${name}" id="${id}"
          type="submit" <#list attributes as attrName, attrVal>${attrName}="${attrVal}" </#list><#list extra as attrName, attrVal>${attrName}="${attrVal}" </#list>>
  <#if labelText?has_content>${labelText}<#else>${msg(label)}</#if>
  </button>
</#macro>

<#macro buttonLink href label id="" class=["kcButtonSecondaryClass", "kcButtonBlockClass"]>
  <a id="${id}" href="${href}" class="${properties.kcButtonClass!}<#list class as c> <#if c == "kcButtonSecondaryClass">${properties[c]!(properties.kcButtonDefaultClass!'')}<#else>${properties[c]!}</#if></#list>">${msg(label)}</a>
</#macro>

<#macro loginButton>
  <@buttons.actionGroup>
    <@buttons.button id="kc-login" name="login" label="doLogIn" />
  </@buttons.actionGroup>
</#macro>
