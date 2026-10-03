<#import "template.ftl" as layout>
<@layout.emailLayout>
    <p>${msg("untrustedLoginBodyIntro",(user.firstName)!user.username)}</p>

    <p>${msg("untrustedLoginBodyDetails")}</p>
    <ul>
        <li><strong>${msg("untrustedLoginTime")}:</strong> ${loginTime?number_to_datetime?string("yyyy-MM-dd HH:mm 'UTC'")}</li>
        <li><strong>${msg("untrustedLoginLocation")}:</strong> ${approximateLocation}</li>
        <li><strong>${msg("untrustedLoginIp")}:</strong> ${ipAddress}</li>
        <li><strong>${msg("untrustedLoginDevice")}:</strong> ${userAgent}</li>
    </ul>

    <p>${msg("untrustedLoginBodyAction")}</p>
</@layout.emailLayout>