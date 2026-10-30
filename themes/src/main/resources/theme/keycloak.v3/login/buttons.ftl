<#macro actionGroup horizontal=false>
  <div class="${properties.kcFormGroupClass!}">
    <div class="${properties.kcFormActionGroupClass!} <#if horizontal>${properties.kcFormActionGroupNowrapClass!}<#else>${properties.kcFormActionGroupWrapClass!}</#if>">
      <#nested>
    </div>
  </div>
</#macro>

<#macro button label="" id="" name="" type="primary" fullWidth=true class=[] htmlType="submit" extra...>
  <button class="${properties.kcButtonClass!} ${properties['kcButton' + type?cap_first + 'Class']!}<#if fullWidth> ${properties.kcButtonBlockClass!}</#if><#list class as c> ${properties[c]!}</#list>"
          <#if name?has_content>name="${name}"</#if> <#if id?has_content>id="${id}"</#if>
          type="${htmlType}" <#list extra as attrName, attrVal>${attrName}="${attrVal}"</#list>>
  <#if label?has_content>${msg(label)}<#else><#nested></#if>
  </button>
</#macro>

<#macro buttonLink href label id="" class=["kcButtonSecondaryClass", "kcButtonBlockClass"]>
  <a <#if id?has_content>id="${id}"</#if> href="${href}" class="${properties.kcButtonClass!}<#list class as c> ${properties[c]!}</#list>">${msg(label)}</a>
</#macro>

<#macro loginButton>
  <@actionGroup>
    <@button id="kc-login" name="login" label="doLogIn" />
  </@actionGroup>
</#macro>
