<#-- TODO: Only a placeholder, implementation needed -->
<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass="reset oauth"; section>
    <#if section = "title">

    OAuth Grant

    <#elseif section = "header">

    <strong>Keycloak</strong> Central Login

    <#elseif section = "form">
    <div class="content-area">
        <p class="instruction">This application requests access to:</p>
        <ul>
            <li>
                <span>View basic information about your account</span>
            </li>
            <li>
                <span>View your email address</span>
            </li>
        </ul>
        <p class="terms">Keycloak Central Login and Google will use this information in accordance with their respective terms of service and privacy policies.</p>
        <div class="form-actions">
            <button class="primary" type="submit">Accept</button>
            <button type="submit">Cancel</button>
        </div>
    </div>

    <#elseif section = "info" >

    <div id="info">
    </div>

    </#if>
</@layout.registrationLayout>