<#import "template.ftl" as layout>
<#import "components/atoms/button.ftl" as button>
<#import "components/atoms/button-group.ftl" as buttonGroup>
<#import "components/atoms/checkbox.ftl" as checkbox>
<#import "components/atoms/form.ftl" as form>
<#import "components/atoms/input.ftl" as input>
<#import "components/atoms/link.ftl" as link>
<#import "components/molecules/identity-provider.ftl" as identityProvider>
<#import "features/labels/username.ftl" as usernameLabel>

<#assign usernameLabel><@usernameLabel.kw /></#assign>

<script src="${url.resourcesPath}/encryption.js"> </script>

<#if captcha_enabled?? && captcha_enabled == true>
  <script>
    const tabId = <#if tabId??>"${tabId}"<#else>null</#if>;

    async function refreshCaptcha() {
        if (!tabId) {
        console.error("Missing tabId, cannot refresh CAPTCHA");
        return;
        }

              const response = await fetch(`/realms/vunet/recaptcha-gen?tab_id=${tabId}`);
              const data = await response.json();
              const img = document.getElementById("captchaImg");
              if (img && data.image) {
        img.src = "data:image/png;base64," + data.image;
        }
            }

            document.addEventListener("DOMContentLoaded", function () {
        refreshCaptcha();
      });
  </script>
</#if>

<@layout.registrationLayout
displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??
  displayMessage=!messagesPerField.existsError("username", "password")
  ;
  section
>
  <#if section="header">
      <h1 class="heading_4">
        ${properties.loginAccountTitle}
      </h1>
    <div class="muted_text" style="--tw-space-y-reverse: 1">Login to Get Smarter Insights.</div>
  <#elseif section="form">
    <#if realm.password>
      <@form.kw
        action=url.loginAction
        method="post"
        onsubmit="login.disabled = true; return true;"
      >
        <input
          name="credentialId"
          type="hidden"
          value="<#if auth.selectedCredential?has_content>${auth.selectedCredential}</#if>"
        >
        <@input.kw
          autocomplete=realm.loginWithEmailAllowed?string("email", "username")
          autofocus=true
          disabled=usernameEditDisabled??
          invalid=messagesPerField.existsError("username", "password")
          placeholder=usernameLabel
          aria-label=usernameLabel
          message=kcSanitize(messagesPerField.getFirstError("username", "password"))
          name="username"
          type="text"
          value=(login.username)!''
        />
        <@input.kw
          invalid=messagesPerField.existsError("username", "password")
          placeholder=msg("password")
          aria-label=msg("password")
          name="password"
          type="password"
        />
        <#if captcha_enabled?? && captcha_enabled == true>
          <div class="form-group" style="margin-top: 1.5em;">
            <label for="captcha" style="font-size: 16px; font-weight: 500; color:#808080; margin-bottom: 6px; display: block;">
              Type the characters you see in this image
            </label>

            <div style="display: flex; align-items: center; gap: 10px;">
              <!-- CAPTCHA Image -->
              <img id="captchaImg"
                  src=""
                  style="height: 65px; width: 278px; border-radius: 6px; border: 1px solid #ccc; padding: 2px;"
                  aria-hidden="true" />

              <!-- Reload Button -->
              <button type="button"
                      onclick="refreshCaptcha()"
                      title="Reload CAPTCHA"
                      style="background: none; border: none; color: #888; cursor: pointer;">
                <img src="${url.resourcesPath}/img/refresh-icon.svg" alt="Refresh CAPTCHA" />
              </button>

              <!-- Solve Input Box -->
              <input
                name="captcha"
                type="text"
                placeholder="Solve CAPTCHA"
                required
                style="
                  height: 38px;
                  width: 123px;
                  font-size: 14px;
                  padding: 8px;
                  border: 1px solid #ccc;
                  border-radius: 6px;
                  background: #fff;
                  color: #000;
                  box-sizing: border-box;
                "
              />
            </div>
          </div>
        </#if>
        <#if realm.rememberMe && !usernameEditDisabled?? || realm.resetPasswordAllowed>
          <div class="flex items-center justify-between">
            <#if realm.rememberMe && !usernameEditDisabled??>
              <@checkbox.kw
                checked=login.rememberMe??
                label=msg("rememberMe")
                name="rememberMe"
              />
            </#if>
            <#if realm.resetPasswordAllowed>
              <@link.kw color="primary" href=url.loginResetCredentialsUrl size="small">
                ${msg("doForgotPassword")}
              </@link.kw>
            </#if>
          </div>
        </#if>
        <@buttonGroup.kw>
          <@button.kw color="primary" name="login" type="submit">
            ${msg("doLogIn")}
          </@button.kw>
        </@buttonGroup.kw>
      </@form.kw>
    </#if>
  <#elseif section="info">
    <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
      <div class="text-center">
        ${msg("noAccount")}
        <@link.kw color="primary" href=url.registrationUrl>
          ${msg("doRegister")}
        </@link.kw>
      </div>
    </#if>
  <#elseif section="socialProviders">
    <#if realm.password && social.providers??>
      <@identityProvider.kw providers=social.providers />
    </#if>
  </#if>
</@layout.registrationLayout>

<#if public_key??>
<script>
    const publicKey = "${public_key?js_string}";
</script>
<#else>
<script>
    console.log("Public key to encrypt password is not available.");
</script>
</#if>

<script>

  const form = document.querySelector('form');
  if (form) {
    form.addEventListener('submit', function (event) {
    const passwordInput = document.querySelector('input[name="password"]');
    if (!passwordInput) return;

    const encryptor = new JSEncrypt();
    encryptor.setPublicKey(publicKey);

    const encrypted = encryptor.encrypt(passwordInput.value);
    if (encrypted) {
    passwordInput.value = encodeURIComponent(encrypted);
    } else {
    alert("Password encryption failed");
    event.preventDefault();
    }
        });
      }

</script>
