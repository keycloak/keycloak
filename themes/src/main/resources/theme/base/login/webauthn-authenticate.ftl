    <#import "template.ftl" as layout>
    <@layout.registrationLayout showAnotherWayIfPresent=false; section>
    <#if section = "title">
     title
    <#elseif section = "header">
        ${kcSanitize(msg("webauthn-login-title"))?no_esc}
    <#elseif section = "form">

    <form id="webauth" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
            <input type="hidden" id="authenticatorData" name="authenticatorData"/>
            <input type="hidden" id="signature" name="signature"/>
            <input type="hidden" id="credentialId" name="credentialId"/>
            <input type="hidden" id="userHandle" name="userHandle"/>
            <input type="hidden" id="error" name="error"/>
        </div>
    </form>

    <#if authenticators??>
        <form id="authn_select" class="${properties.kcFormClass!}">
            <#list authenticators.authenticators as authenticator>
                <input type="hidden" name="authn_use_chk" value="${authenticator.credentialId}"/>
            </#list>
        </form>
    </#if>

    <script type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
    <script type="text/javascript" src="${url.resourcesPath}/js/base64url.js"></script>
    <script type="text/javascript">

        window.onload = () => {
            let isUserIdentified = ${isUserIdentified};
            if (!isUserIdentified) {
                doAuthenticate([]);
                return;
            }
            checkAllowCredentials();
        };

        function checkAllowCredentials() {
            let allowCredentials = [];
            let authn_use = document.forms['authn_select'].authn_use_chk;

            if (authn_use !== undefined) {
                if (authn_use.length === undefined) {
                    allowCredentials.push({
                        id: base64url.decode(authn_use.value, {loose: true}),
                        type: 'public-key',
                    });
                } else {
                    for (let i = 0; i < authn_use.length; i++) {
                        allowCredentials.push({
                            id: base64url.decode(authn_use[i].value, {loose: true}),
                            type: 'public-key',
                        });
                    }
                }
            }
            doAuthenticate(allowCredentials);
        }


    function doAuthenticate(allowCredentials) {
        let challenge = "${challenge}";
        let userVerification = "${userVerification}";
        let rpId = "${rpId}";
        let publicKey = {
            rpId : rpId,
            challenge: base64url.decode(challenge, { loose: true })
        };

        if (allowCredentials.length) {
            publicKey.allowCredentials = allowCredentials;
        }

        if (userVerification !== 'not specified') publicKey.userVerification = userVerification;

        navigator.credentials.get({publicKey})
            .then((result) => {
                window.result = result;

                let clientDataJSON = result.response.clientDataJSON;
                let authenticatorData = result.response.authenticatorData;
                let signature = result.response.signature;

                $("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), { pad: false }));
                $("#authenticatorData").val(base64url.encode(new Uint8Array(authenticatorData), { pad: false }));
                $("#signature").val(base64url.encode(new Uint8Array(signature), { pad: false }));
                $("#credentialId").val(result.id);
                if(result.response.userHandle) {
                    $("#userHandle").val(base64url.encode(new Uint8Array(result.response.userHandle), { pad: false }));
                }
                $("#webauth").submit();
            })
            .catch((err) => {
                $("#error").val(err);
                $("#webauth").submit();
            })
        ;
    }

    </script>
    <#elseif section = "info">

    </#if>
    </@layout.registrationLayout>
