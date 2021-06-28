<#macro userProfileFormFields>
	<#list profile.attributes as attribute>
		<#nested "beforeField" attribute>
		<div class="${properties.kcFormGroupClass!}">
		    <div class="${properties.kcLabelWrapperClass!}">
		        <label for="${attribute.name}" class="${properties.kcLabelClass!}">${advancedMsg(attribute.displayName!'')}</label>
		        <#if attribute.required>*</#if>
		    </div>
		    <div class="${properties.kcInputWrapperClass!}">
		        <input type="text" id="${attribute.name}" name="${attribute.name}" value="${(attribute.value!'')}"
		               class="${properties.kcInputClass!}"
		               aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
		               <#if attribute.readOnly>disabled</#if>
		               <#if attribute.autocomplete??>autocomplete="${attribute.autocomplete}"</#if>
		        />
		
		        <#if messagesPerField.existsError('${attribute.name}')>
		            <span id="input-error-${attribute.name}" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
		                ${kcSanitize(messagesPerField.get('${attribute.name}'))?no_esc}
		            </span>
		        </#if>
		    </div>
		</div>
		<#nested "afterField" attribute>
	</#list>
</#macro>