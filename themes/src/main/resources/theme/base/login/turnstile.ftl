<#--
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 -->

<#--
  Macro for rendering Cloudflare Turnstile CAPTCHA widget.
  This macro should be used consistently across all forms (login, register, reset password)
  that require Turnstile protection.
  
  The macro checks for the turnstileRequired attribute and renders the widget
  with the configured parameters (site key, action, theme, size, language).
-->
<#macro turnstileWidget>
    <#if turnstileRequired??>
        <div class="form-group">
            <div class="${properties.kcInputWrapperClass!}">
                <div class="cf-turnstile" 
                     data-sitekey="${turnstileSiteKey}" 
                     data-action="${turnstileAction}" 
                     data-theme="${turnstileTheme}" 
                     data-size="${turnstileSize}" 
                     data-language="${turnstileLanguage}"></div>
            </div>
        </div>
    </#if>
</#macro>
