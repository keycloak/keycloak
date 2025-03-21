<#import "field.ftl" as field>
<#macro userProfileFormFields>
	<#assign currentGroup="">
	
	<#list profile.attributes as attribute>

		<#assign group = (attribute.group)!"">
		<#if group != currentGroup>
			<#assign currentGroup=group>
			<#if currentGroup != "">
				<div class="${properties.kcFormGroupClass!}"
				<#list group.html5DataAnnotations as key, value>
					data-${key}="${value}"
				</#list>
				>

					<#assign groupDisplayHeader=group.displayHeader!"">
					<#if groupDisplayHeader != "">
						<#assign groupHeaderText=advancedMsg(groupDisplayHeader)!group>
					<#else>
						<#assign groupHeaderText=group.name!"">
					</#if>
					<div class="${properties.kcContentWrapperClass!}">
						<label id="header-${attribute.group.name}" class="${kcFormGroupHeader!}">${groupHeaderText}</label>
					</div>

					<#assign groupDisplayDescription=group.displayDescription!"">
					<#if groupDisplayDescription != "">
						<#assign groupDescriptionText=advancedMsg(groupDisplayDescription)!"">
						<div class="${properties.kcLabelWrapperClass!}">
							<label id="description-${group.name}" class="${properties.kcLabelClass!}">${groupDescriptionText}</label>
						</div>
					</#if>
				</div>
			</#if>
		</#if>

		<#nested "beforeField" attribute>

		<@field.group name=attribute.name label=advancedMsg(attribute.displayName!'') error=kcSanitize(messagesPerField.get('${attribute.name}'))?no_esc required=attribute.required>
			<div class="${properties.kcInputWrapperClass!}">
				<#if attribute.annotations.inputHelperTextBefore??>
					<div class="${properties.kcInputHelperTextBeforeClass!}" id="form-help-text-before-${attribute.name}" aria-live="polite">${kcSanitize(advancedMsg(attribute.annotations.inputHelperTextBefore))?no_esc}</div>
				</#if>
				<@inputFieldByType attribute=attribute/>
				<#if attribute.annotations.inputHelperTextAfter??>
					<div class="${properties.kcInputHelperTextAfterClass!}" id="form-help-text-after-${attribute.name}" aria-live="polite">${kcSanitize(advancedMsg(attribute.annotations.inputHelperTextAfter))?no_esc}</div>
				</#if>
			</div>
		</@field.group>
		<#nested "afterField" attribute>
	</#list>

	<#list profile.html5DataAnnotations?keys as key>
        <script type="module" src="${url.resourcesPath}/js/${key}.js"></script>
    </#list>
</#macro>

<#macro inputFieldByType attribute>
	<#switch attribute.annotations.inputType!''>
	<#case 'textarea'>
		<@textareaTag attribute=attribute/>
		<#break>
	<#case 'select'>
	<#case 'multiselect'>
		<@selectTag attribute=attribute/>
		<#break>
	<#case 'select-radiobuttons'>
	<#case 'multiselect-checkboxes'>
		<@inputTagSelects attribute=attribute/>
		<#break>
	<#default>
		<#if attribute.multivalued && attribute.values?has_content>
			<#list attribute.values as value>
				<@inputTag attribute=attribute value=value!''/>
			</#list>
		<#else>
			<@inputTag attribute=attribute value=attribute.value!''/>
		</#if>
	</#switch>
</#macro>

<#macro inputTag attribute value>
	<span class="${properties.kcInputClass} <#if error?has_content>${properties.kcError}</#if>">
		<input type="<@inputTagType attribute=attribute/>" id="${attribute.name}" name="${attribute.name}" value="${(value!'')}" class="${properties.kcInputClass!}"
			aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
			<#if attribute.readOnly>disabled</#if>
			<#if attribute.autocomplete??>autocomplete="${attribute.autocomplete}"</#if>
			<#if attribute.annotations.inputTypePlaceholder??>placeholder="${advancedMsg(attribute.annotations.inputTypePlaceholder)}"</#if>
			<#if attribute.annotations.inputTypePattern??>pattern="${attribute.annotations.inputTypePattern}"</#if>
			<#if attribute.annotations.inputTypeSize??>size="${attribute.annotations.inputTypeSize}"</#if>
			<#if attribute.annotations.inputTypeMaxlength??>maxlength="${attribute.annotations.inputTypeMaxlength}"</#if>
			<#if attribute.annotations.inputTypeMinlength??>minlength="${attribute.annotations.inputTypeMinlength}"</#if>
			<#if attribute.annotations.inputTypeMax??>max="${attribute.annotations.inputTypeMax}"</#if>
			<#if attribute.annotations.inputTypeMin??>min="${attribute.annotations.inputTypeMin}"</#if>
			<#if attribute.annotations.inputTypeStep??>step="${attribute.annotations.inputTypeStep}"</#if>
			<#list attribute.html5DataAnnotations as key, value>
					data-${key}="${value}"
			</#list>
		/>
	</span>
</#macro>

<#macro inputTagType attribute>
	<#compress>
	<#if attribute.annotations.inputType??>
		<#if attribute.annotations.inputType?starts_with("html5-")>
			${attribute.annotations.inputType[6..]}
		<#else>
			${attribute.annotations.inputType}
		</#if>
	<#else>
	text
	</#if>
	</#compress>
</#macro>

<#macro textareaTag attribute>
	<textarea id="${attribute.name}" name="${attribute.name}" class="${properties.kcInputClass!}"
		aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
		<#if attribute.readOnly>disabled</#if>
		<#if attribute.annotations.inputTypeCols??>cols="${attribute.annotations.inputTypeCols}"</#if>
		<#if attribute.annotations.inputTypeRows??>rows="${attribute.annotations.inputTypeRows}"</#if>
		<#if attribute.annotations.inputTypeMaxlength??>maxlength="${attribute.annotations.inputTypeMaxlength}"</#if>
	>${(attribute.value!'')}</textarea>
</#macro>

<#macro selectTag attribute>
	<div class="${properties.kcInputClass!}">
		<select id="${attribute.name}" name="${attribute.name}"
			aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
			<#if attribute.readOnly>disabled</#if>
			<#if attribute.annotations.inputType=='multiselect'>multiple</#if>
			<#if attribute.annotations.inputTypeSize??>size="${attribute.annotations.inputTypeSize}"</#if>
		>
			<#if attribute.annotations.inputType=='select'>
				<option value=""></option>
			</#if>

			<#if attribute.annotations.inputOptionsFromValidation?? && attribute.validators[attribute.annotations.inputOptionsFromValidation]?? && attribute.validators[attribute.annotations.inputOptionsFromValidation].options??>
				<#assign options=attribute.validators[attribute.annotations.inputOptionsFromValidation].options>
			<#elseif attribute.validators.options?? && attribute.validators.options.options??>
				<#assign options=attribute.validators.options.options>
			<#else>
				<#assign options=[]>
			</#if>

			<#list options as option>
				<option value="${option}" <#if attribute.values?seq_contains(option)>selected</#if>><@selectOptionLabelText attribute=attribute option=option/></option>
			</#list>
		</select>
		<span class="${properties.kcFormControlUtilClass}">
			<span class="${properties.kcFormControlToggleIcon!}">
				<svg
					class="pf-v5-svg"
					viewBox="0 0 320 512"
					fill="currentColor"
					aria-hidden="true"
					role="img"
					width="1em"
					height="1em"
				>
					<path
						d="M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z"
					>
					</path>
				</svg>
			</span>
		</span>
	</div>
</#macro>

<#macro inputTagSelects attribute>
	<#if attribute.annotations.inputType=='select-radiobuttons'>
		<#assign inputType='radio'>
		<#assign classDiv=properties.kcInputClassRadio!>
		<#assign classInput=properties.kcInputClassRadioInput!>
		<#assign classLabel=properties.kcInputClassRadioLabel!>
	<#else>
		<input type="hidden" id="${attribute.name}-empty" name="${attribute.name}" value=""/>
		<#assign inputType='checkbox'>
		<#assign classDiv=properties.kcInputClassCheckbox!>
		<#assign classInput=properties.kcInputClassCheckboxInput!>
		<#assign classLabel=properties.kcInputClassCheckboxLabel!>
	</#if>
	
	<#if attribute.annotations.inputOptionsFromValidation?? && attribute.validators[attribute.annotations.inputOptionsFromValidation]?? && attribute.validators[attribute.annotations.inputOptionsFromValidation].options??>
        <#assign options=attribute.validators[attribute.annotations.inputOptionsFromValidation].options>
    <#elseif attribute.validators.options?? && attribute.validators.options.options??>
        <#assign options=attribute.validators.options.options>
    <#else>
        <#assign options=[]>
    </#if>

    <#list options as option>
        <div class="${classDiv}">
            <input type="${inputType}" id="${attribute.name}-${option}" name="${attribute.name}" value="${option}" class="${classInput}"
                aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
                <#if attribute.readOnly>disabled</#if>
                <#if attribute.values?seq_contains(option)>checked</#if>
            />
            <label for="${attribute.name}-${option}" class="${classLabel}<#if attribute.readOnly> ${properties.kcInputClassRadioCheckboxLabelDisabled!}</#if>"><@selectOptionLabelText attribute=attribute option=option/></label>
        </div>
    </#list>
</#macro>

<#macro selectOptionLabelText attribute option>
	<#compress>
	<#if attribute.annotations.inputOptionLabels??>
		${advancedMsg(attribute.annotations.inputOptionLabels[option]!option)}
	<#else>
		<#if attribute.annotations.inputOptionLabelsI18nPrefix??>
			${msg(attribute.annotations.inputOptionLabelsI18nPrefix + '.' + option)}
		<#else>
			${option}
		</#if>
	</#if>
	</#compress>
</#macro>
