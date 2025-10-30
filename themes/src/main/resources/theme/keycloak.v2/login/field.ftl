<#macro group name label error="" required=false>

<div class="${properties.kcFormGroupClass}">
    <div class="${properties.kcFormGroupLabelClass}">
        <label for="${name}" class="${properties.kcFormLabelClass}">
        <span class="${properties.kcFormLabelTextClass}">
            ${label}
        </span>
            <#if required>
                <span class="${properties.kcInputRequiredClass}" aria-hidden="true">&#42;</span>
            </#if>
        </label>
    </div>

    <#nested>

    <div id="input-error-container-${name}">
        <#if error?has_content>
            <div class="${properties.kcFormHelperTextClass}" aria-live="polite">
                <div class="${properties.kcInputHelperTextClass}">
                    <div class="${properties.kcInputHelperTextItemClass} ${properties.kcError}" id="input-error-${name}">
                        <span class="${properties.kcInputErrorMessageClass}">
                            ${error}
                        </span>
                    </div>
                </div>
            </div>
        </#if>
    </div>
</div>

</#macro>

<#macro errorIcon error="">
  <#if error?has_content>
    <span class="${properties.kcFormControlUtilClass}">
        <span class="${properties.kcInputErrorIconStatusClass}">
          <i class="${properties.kcInputErrorIconClass}" aria-hidden="true"></i>
        </span>
    </span>
  </#if>
</#macro>

<#macro input name label value="" required=false autocomplete="off" fieldName=name error=kcSanitize(messagesPerField.get(fieldName))?no_esc autofocus=false>
  <@group name=name label=label error=error required=required>
    <span class="${properties.kcInputClass} <#if error?has_content>${properties.kcError}</#if>">
        <input id="${name}" name="${name}" value="${value}" type="text" autocomplete="${autocomplete}" <#if autofocus>autofocus</#if>
                <#if autocomplete == "one-time-code">inputmode="numeric"</#if>
                aria-invalid="<#if error?has_content>true</#if>"/>
        <@errorIcon error=error/>
    </span>
  </@group>
</#macro>

<#macro password name label value="" required=false forgotPassword=false fieldName=name error=kcSanitize(messagesPerField.get(fieldName))?no_esc autocomplete="off" autofocus=false>
  <@group name=name label=label error=error required=required>
    <div class="${properties.kcInputGroup}">
      <div class="${properties.kcInputGroupItemClass} ${properties.kcFill}">
        <span class="${properties.kcInputClass} <#if error?has_content>${properties.kcError}</#if>">
          <input id="${name}" name="${name}" value="${value}" type="password" autocomplete="${autocomplete}" <#if autofocus>autofocus</#if>
                  aria-invalid="<#if error?has_content>true</#if>"/>
          <@errorIcon error=error/>
        </span>
      </div>
      <div class="${properties.kcInputGroupItemClass}">
        <button class="${properties.kcFormPasswordVisibilityButtonClass}" type="button" aria-label="${msg('showPassword')}"
                aria-controls="${name}" data-password-toggle
                data-icon-show="${properties.kcFormPasswordVisibilityIconShow}" data-icon-hide="${properties.kcFormPasswordVisibilityIconHide}"
                data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}" id="${name}-show-password">
            <i class="${properties.kcFormPasswordVisibilityIconShow}" aria-hidden="true"></i>
        </button>
      </div>
    </div>
    <div class="${properties.kcFormHelperTextClass}" aria-live="polite">
        <div class="${properties.kcInputHelperTextClass}">
            <#-- Additional helper items -->
            <#nested>
            <#if forgotPassword>
                <div class="${properties.kcInputHelperTextItemClass}">
                  <span class="${properties.kcInputHelperTextItemTextClass}">
                      <a href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
                  </span>
                </div>
            </#if>
        </div>
    </div>

  </@group>
</#macro>

<#macro clipboard name label ariaLabel=label value="" readonly=true>
  <@group name=name label=label>
    <div class="${properties.kcCodeClipboardCopyClass}" id="kc-${name}-clipboard">
      <div class="${properties.kcCodeClipboardCopyGroupClass}">
        <div class="${properties.kcInputGroup}">
          <div class="${properties.kcInputGroupItemClass}">
            <button 
              class="${properties.kcFormPasswordVisibilityButtonClass}" 
              type="button" 
              aria-label="${msg("code-clipboard-label")}"
              aria-expanded="false"
              aria-controls="kc-${name}-content"
              data-icon-expanded-class="${properties.kcAngleDownIconClass}"
              data-icon-collapsed-class="${properties.kcAngleRightIconClass}"
              data-expanded-class="${properties.kcExpandedClass}"
              id="kc-${name}-toggle"
            >
              <i id="kc-${name}-toggle-icon" class="${properties.kcAngleRightIconClass}" aria-hidden="true"></i>
            </button>
          </div>
          <div class="${properties.kcInputGroupItemClass} ${properties.kcFill}">
            <span class="${properties.kcInputClass} <#if readonly>${properties.kcFormReadOnlyClass}</#if>">
              <input
                id="${name}"
                name="${name}"
                value="${value}"
                type="text"
                <#if readonly>readonly</#if>
                aria-label="${ariaLabel}"
              />
            </span>
          </div>
          <div class="${properties.kcInputGroupItemClass}">
            <button
              class="${properties.kcFormPasswordVisibilityButtonClass}"
              type="button"
              aria-label="${msg("code-copy-label")}"
              data-icon-success="${properties.kcCheckIconClass}"
              data-icon-failure="${properties.kcInputErrorIconClass}"
              data-success-label="${msg("code-copy-success")}"
              data-failure-label="${msg("code-copy-failure")}"
              id="kc-${name}-copy-button"
            >
              <i id="kc-${name}-copy-icon" class="${properties.kcCopyIconClass}" aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>
      <div class="${properties.kcCodeClipboardCopyContentClass}" id="kc-${name}-content" hidden>
        <pre><code aria-label="${ariaLabel}">${value}</code></pre>
      </div>
    </div>
  </@group>
</#macro>

<#macro checkbox name label value=false required=false>
  <div class="${properties.kcCheckboxClass}">
    <label for="${name}" class="${properties.kcCheckboxClass}">
      <input
        class="${properties.kcCheckboxInputClass}"
        type="checkbox"
        id="${name}"
        name="${name}"
        <#if value>checked</#if>
      />
      <span class="${properties.kcCheckboxLabelClass}">${label}</span>
      <#if required>
        <span class="${properties.kcCheckboxLabelRequiredClass}" aria-hidden="true">&#42;</span>
      </#if>
    </label>
  </div>
</#macro>