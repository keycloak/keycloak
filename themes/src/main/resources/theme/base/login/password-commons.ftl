<#import "field.ftl" as field>
<#-- Use <@passwordCommons.logoutOtherSessions/> inside a form to offer logout from other sessions. -->
<#macro logoutOtherSessions>
    <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
        <div class="${properties.kcFormOptionsWrapperClass!}"> 
            <@field.checkbox name="logout-sessions" label=msg("logoutOtherSessions") value=false />
        </div>
    </div>
</#macro>
