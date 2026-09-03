<#import "template.ftl" as layout>
<#import "divider.ftl" as divider>
<#import "qr-code.ftl" as qr>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header">
        ${msg("oid4vpLoginTitle")}
    <#elseif section = "form">
        <#if (crossDeviceQrCode!'')?has_content>
            <div id="oid4vp-qr-block" class="${properties.kcFormGroupClass!}"
                 data-oid4vp-wallet-url="${crossDeviceWalletUrl!''}">
                <@qr.qrCode id="oid4vp-qr-code" content=crossDeviceQrCode alt=msg("oid4vpQrCodeAlt") title=msg("oid4vpScanQrCodeTitle") label=msg("oid4vpScanQrCode") />
            </div>
            <div id="oid4vp-qr-expired" class="${properties.kcFormGroupClass!}" style="text-align: center" role="status" aria-live="polite" hidden>
                ${msg("oid4vpQrExpired")} <a id="oid4vp-restart-login" href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a> .
            </div>
            <script type="module">
                <#outputformat "JavaScript">
                import { startCrossDeviceStatusPolling } from ${(url.resourcesPath + "/js/authChecker.js")?c};

                startCrossDeviceStatusPolling(
                    ${crossDeviceStatusUrl?c},
                    () => {
                        document.getElementById("oid4vp-qr-block")?.setAttribute("hidden", "");
                        document.getElementById("oid4vp-qr-expired")?.removeAttribute("hidden");
                    }
                );
                </#outputformat>
            </script>
        </#if>
        <#if (sameDeviceWalletUrl!'')?has_content>
            <#if (crossDeviceQrCode!'')?has_content>
                <@divider.or />
            </#if>
            <div class="${properties.kcFormGroupClass!}">
                <a id="oid4vp-open-wallet"
                   href="${sameDeviceWalletUrl}"
                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}">
                    ${msg("oid4vpOpenWalletApp")}
                </a>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
