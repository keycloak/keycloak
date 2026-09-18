<#import "field.ftl" as field>
<#macro logoutOtherSessions>
    <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
        <div class="${properties.kcFormOptionsWrapperClass!}"> 
            <@field.checkbox name="logout-sessions" label=msg("logoutOtherSessions") value=false />
        </div>
    </div>
</#macro>
