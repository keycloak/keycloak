<#ftl output_format="plainText">

<#if firstName?? && lastName??>
    ${kcSanitize(msg("orgInviteBodyPersonalized", link, linkExpiration, realmName, organization.name, linkExpirationFormatter(linkExpiration), firstName, lastName))}
<#else>
    ${kcSanitize(msg("orgInviteBody", link, linkExpiration, realmName, organization.name, linkExpirationFormatter(linkExpiration)))}
</#if>

