<#-- Nest a custom control in <@field.group name="field-id" label=msg("labelKey")> for its label and errors. -->
<#macro group name label error="" required=false errorId="input-error-${name}">

<div class="${properties.kcFormGroupClass!}">
    <div class="${properties.kcFormGroupLabelClass!}">
        <label for="${name}" class="${properties.kcFormLabelClass!}">
        <span class="${properties.kcFormLabelTextClass!}">
            ${label}
        </span>
            <#if required>
                <span class="${properties.kcInputRequiredClass!}" aria-hidden="true">&#42;</span>
            </#if>
        </label>
    </div>

    <div class="${properties.kcFormGroupControlClass!}">
        <#nested>

        <@errorMessage name=name error=error errorId=errorId/>
    </div>
</div>

</#macro>

<#-- Render a field error using the theme helper text classes. -->
<#macro errorMessage name error="" errorId="input-error-${name}">
    <div id="input-error-container-${name}" class="${properties.kcFormHelperTextClass!}" aria-live="polite" <#if !error?has_content>hidden</#if>>
        <#if error?has_content>
            <div class="${properties.kcInputHelperTextClass!}">
                <div class="${properties.kcInputHelperTextItemClass!} ${properties.kcError!}" id="${errorId}">
                    <#if properties.kcInputHelperTextItemIconClass?has_content>
                        <span class="${properties.kcInputHelperTextItemIconClass}">
                            <i class="${properties.kcInputErrorIconClass!}" aria-hidden="true"></i>
                        </span>
                    </#if>
                    <span class="${properties.kcInputErrorMessageClass!}">${error}</span>
                </div>
            </div>
        </#if>
    </div>
</#macro>

<#-- Use <@field.errorIcon error=message/> inside a control to show its validation status. -->
<#macro errorIcon error="">
  <#if error?has_content>
    <span class="${properties.kcFormControlUtilClass!}">
        <span class="${properties.kcInputErrorIconStatusClass!}">
          <i class="${properties.kcInputErrorIconClass!}" aria-hidden="true"></i>
        </span>
    </span>
  </#if>
</#macro>

<#-- Use <@field.input name="username" label=msg("username")/>; errors default to messagesPerField. -->
<#macro input name label value="" required=false autocomplete="off" fieldName=name error=messagesPerField.get(fieldName) autofocus=false inputmode="" errorId="input-error-${name}">
  <@group name=name label=label error=error required=required errorId=errorId>
    <span class="${properties.kcInputClass!} <#if error?has_content>${properties.kcError!}</#if>">
        <input id="${name}" name="${name}" value="${value}" type="text" autocomplete="${autocomplete}" autocapitalize="none" spellcheck="false" <#if autofocus>autofocus</#if>
                <#if inputmode?has_content>inputmode="${inputmode}"</#if>
                <#if error?has_content>aria-invalid="true" aria-describedby="${errorId}"</#if>/>
        <@errorIcon error=error/>
    </span>
  </@group>
</#macro>

<#-- Use <@field.password name="password" label=msg("password")>; nest optional helper text or controls. -->
<#macro password name label value="" required=false forgotPassword=false fieldName=name error=messagesPerField.get(fieldName) autocomplete="off" autofocus=false>
  <#local helperItems><#compress><#nested></#compress></#local>
  <@group name=name label=label error=error required=required>
    <div class="${properties.kcInputGroup!}">
      <div class="${properties.kcInputGroupItemClass!} ${properties.kcFill!}">
        <span class="${properties.kcInputClass!} <#if error?has_content>${properties.kcError!}</#if>">
          <input id="${name}" name="${name}" value="${value}" type="password" autocomplete="${autocomplete}" <#if autofocus>autofocus</#if>
                  <#if error?has_content>aria-invalid="true" aria-describedby="input-error-${name}"</#if>/>
          <@errorIcon error=error/>
        </span>
      </div>
      <div class="${properties.kcInputGroupItemClass!}">
        <button class="${properties.kcFormPasswordVisibilityButtonClass!}" type="button" aria-label="${msg('showPassword')}"
                aria-controls="${name}" data-password-toggle
                data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}" data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}" id="${name}-show-password">
            <#if properties.kcButtonIconClass?has_content><span class="${properties.kcButtonIconClass}"></#if>
            <i data-password-toggle-icon class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
            <#if properties.kcButtonIconClass?has_content></span></#if>
        </button>
      </div>
    </div>
    <#if helperItems?has_content || forgotPassword>
    <div class="${properties.kcFormHelperTextClass!}" aria-live="polite">
        <div class="${properties.kcInputHelperTextClass!}">
            <#-- Additional helper items -->
            ${helperItems}
            <#if forgotPassword>
                <div class="${properties.kcInputHelperTextItemClass!}">
                  <span class="${properties.kcInputHelperTextItemTextClass!}">
                      <a href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
                  </span>
                </div>
            </#if>
        </div>
    </div>
    </#if>

  </@group>
  <#if !passwordVisibilityScriptLoaded??>
    <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    <#global passwordVisibilityScriptLoaded=true>
  </#if>
</#macro>

<#-- Use <@field.clipboard name="code" label=msg("labelKey") value=code/> for copy and expand controls. -->
<#macro clipboard name label ariaLabel=label value="" readonly=true>
  <@group name=name label=label>
    <div class="${properties.kcCodeClipboardCopyClass!}" id="kc-${name}-clipboard">
      <div class="${properties.kcCodeClipboardCopyGroupClass!}">
        <div class="${properties.kcInputGroup!}">
          <div class="${properties.kcInputGroupItemClass!}">
            <button 
              class="${properties.kcFormPasswordVisibilityButtonClass!}" 
              type="button" 
              aria-label="${msg("code-clipboard-label")}"
              aria-expanded="false"
              aria-controls="kc-${name}-content"
              data-icon-expanded-class="${properties.kcAngleDownIconClass!}"
              data-icon-collapsed-class="${properties.kcAngleRightIconClass!}"
              data-expanded-class="${properties.kcExpandedClass!}"
              id="kc-${name}-toggle"
            >
              <i id="kc-${name}-toggle-icon" class="${properties.kcAngleRightIconClass!}" aria-hidden="true"></i>
            </button>
          </div>
          <div class="${properties.kcInputGroupItemClass!} ${properties.kcFill!}">
            <span class="${properties.kcInputClass!} <#if readonly>${properties.kcFormReadOnlyClass!}</#if>">
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
          <div class="${properties.kcInputGroupItemClass!}">
            <button
              class="${properties.kcFormPasswordVisibilityButtonClass!}"
              type="button"
              aria-label="${msg("code-copy-label")}"
              data-icon-success="${properties.kcCheckIconClass!}"
              data-icon-failure="${properties.kcInputErrorIconClass!}"
              data-success-label="${msg("code-copy-success")}"
              data-failure-label="${msg("code-copy-failure")}"
              id="kc-${name}-copy-button"
            >
              <i id="kc-${name}-copy-icon" class="${properties.kcCopyIconClass!}" aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>
      <div class="${properties.kcCodeClipboardCopyContentClass!}" id="kc-${name}-content" hidden>
        <pre><code aria-label="${ariaLabel}">${value}</code></pre>
      </div>
    </div>
  </@group>
</#macro>

<#-- Use <@field.checkbox name="rememberMe" label=msg("rememberMe") value=true/>; value sets the checked state. -->
<#macro checkbox name label value=false required=false invalid=false error=messagesPerField.get(name) errorId="input-error-${name}">
  <div class="${properties.kcFormGroupControlClass!}">
    <label for="${name}" class="${properties.kcCheckboxClass!}">
      <input
        class="${properties.kcCheckboxInputClass!}"
        type="checkbox"
        id="${name}"
        name="${name}"
        <#if value>checked</#if>
        <#if invalid || error?has_content>aria-invalid="true"</#if>
        <#if error?has_content>aria-describedby="${errorId}"</#if>
      />
      <span class="${properties.kcCheckboxLabelClass!}">${label}<#if required><span class="${properties.kcCheckboxLabelRequiredClass!}" aria-hidden="true">&#42;</span></#if></span>
    </label>
    <#if error?has_content>
      <@errorMessage name=name error=error errorId=errorId/>
    </#if>
  </div>
</#macro>
