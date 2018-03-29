<#ftl output_format="plainText">
${msg("loginTotpIntro")}

${msg("loginTotpStep1")}

<#list totp.policy.supportedApplications as app>
* ${app}
</#list>

${msg("loginTotpManualStep2")}

    ${totp.totpSecretEncoded}


${msg("loginTotpManualStep3")}

- ${msg("loginTotpType")}: ${msg("loginTotp." + totp.policy.type)}
- ${msg("loginTotpAlgorithm")}: ${totp.policy.getAlgorithmKey()}
- ${msg("loginTotpDigits")}: ${totp.policy.digits}
<#if totp.policy.type = "totp">
- ${msg("loginTotpInterval")}: ${totp.policy.period}

<#elseif totp.policy.type = "hotp">
- ${msg("loginTotpCounter")}: ${totp.policy.initialCounter}

</#if>

Enter in your one time password so we can verify you have installed it correctly.



