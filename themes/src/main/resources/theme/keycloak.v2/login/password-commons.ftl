<#macro logoutOtherSessions>
    <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
        <div class="${properties.kcFormOptionsWrapperClass!}">
            <div class="pf-v5-c-check">
                <input class="pf-v5-c-check__input" type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked>
                <label class="pf-v5-c-check__label" for="logout-sessions">
                    ${msg("logoutOtherSessions")}
                </label>
            </div>
        </div>
    </div>
</#macro>
