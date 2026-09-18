<#ftl output_format="plainText">

<#if firstName?? && lastName??>
    ${msg("orgInviteBodyPersonalized", link, linkExpiration, realmName, organization.name, linkExpirationFormatter(linkExpiration), firstName, lastName)}
<#else>
    ${msg("orgInviteBody", link, linkExpiration, realmName, organization.name, linkExpirationFormatter(linkExpiration))}
</#if>

