# User LDAP Filter is applied to Group lookups in `getFilterById()`, causing Group Members tab to return empty results

## Description

When a **User LDAP filter** (e.g. `(LoginDisabled=false)`) is configured in the LDAP federation settings, clicking the **Members** tab of a Group in the Admin console returns no results. The group cannot be found because the User LDAP filter is incorrectly appended to the LDAP group lookup query.

## Root Cause

`LDAPOperationManager.getFilterById()` unconditionally appends `config.getCustomUserSearchFilter()` to **every** `lookupById` call, regardless of whether the target entity is a user, a group, or a role.

https://github.com/keycloak/keycloak/blob/main/federation/ldap/src/main/java/org/keycloak/storage/ldap/idm/store/ldap/LDAPOperationManager.java#L394-L396

```java
public Condition getFilterById(String id) {
    LDAPQueryConditionsBuilder builder = new LDAPQueryConditionsBuilder();
    Condition conditionId;

    if (this.config.isObjectGUID()) {
        byte[] objectGUID = LDAPUtil.encodeObjectGUID(id);
        conditionId = builder.equal(getUuidAttributeName(), objectGUID);
    } else if (this.config.isEdirectoryGUID()) {
        byte[] objectGUID = LDAPUtil.encodeObjectEDirectoryGUID(id);
        conditionId = builder.equal(getUuidAttributeName(), objectGUID);
    } else {
        conditionId = builder.equal(getUuidAttributeName(), id);
    }

    // BUG: This filter is applied unconditionally to ALL lookupById calls,
    // including group and role lookups where it does not belong.
    if (config.getCustomUserSearchFilter() != null) {
        return builder.andCondition(new Condition[]{conditionId, builder.addCustomLDAPFilter(config.getCustomUserSearchFilter())});
    } else {
        return conditionId;
    }
}
```

## Steps to Reproduce

1. Configure an LDAP User Federation provider
2. Set a **User LDAP filter** in the federation settings (e.g. `(LoginDisabled=false)`)
3. Configure a Group LDAP mapper with membership type DN
4. Go to the Admin console -> Groups -> select a group -> click the **Members** tab
5. Observe that no members are shown

## Expected Behavior

The Members tab should list all LDAP users that are members of the group. The User LDAP filter should only be applied to user searches, not to group lookups.

## Actual Behavior

The Members tab shows no results. The LDAP group lookup fails silently because the User LDAP filter is appended to the group search query, and the group object does not have the filtered attribute.

## LDAP Query Logs

### Without User LDAP filter (correct behavior)

**Query 1** - Group lookup:
```
LdapOperation: lookupById
baseDN: ou=groups,o=com
filter: (cn=Group1)
searchScope: 2
returningAttrs: [cn, member]
took: 3 ms
```

**Query 2** - Member lookup using CNs from the group's `member` attribute:
```
LdapOperation: search
baseDn: o=com
filter: (&(|(cn=user1)(cn=user2))(objectclass=inetOrgPerson)(objectclass=organizationalPerson))
searchScope: 2
returningAttrs: [krb5PrincipalName, pwdChangedTime, cn, title, givenName, mobile, mail, createTimestamp, sn, fullName]
resultSize: 2
took: 6 ms
```

### With User LDAP filter `(LoginDisabled=false)` (broken behavior)

**Query 1** - Group lookup has the User LDAP filter injected:
```
LdapOperation: lookupById
baseDN: ou=groups,o=com
filter: (cn=Group1)(LoginDisabled=false))
searchScope: 2
returningAttrs: [cn, member]
took: 3 ms
```

The group object does not have a `LoginDisabled` attribute, so the query returns no results. Query 2 (member lookup) never executes.

## Code Path

1. Admin console clicks Members tab
2. `GroupLDAPStorageMapper.getGroupMembers()` calls `loadLDAPGroupByName(groupName)`
3. `loadLDAPGroupByName()` creates a group query with condition `(cn=Group1)` and calls `getFirstResult()`
4. `LDAPIdentityStore.fetchQueryResults()` detects that `cn` matches the UUID attribute and takes the `lookupById` shortcut
5. `LDAPOperationManager.lookupById()` calls `getFilterById(id)`
6. **`getFilterById()` appends the User LDAP filter to the group lookup query**
7. The LDAP server returns no results because the group entry does not match the user filter
8. `loadLDAPGroupByName()` returns `null`, so `getGroupMembers()` returns an empty list

## Suggested Fix

`getFilterById()` should not unconditionally apply the User LDAP filter. The filter should only be applied when the lookup target is a user entity. Possible approaches:

- Add a boolean parameter to `getFilterById()` and `lookupById()` to control whether the user filter should be applied
- Have `fetchQueryResults()` pass context about the entity type being queried
- Only apply the custom user search filter in the user-specific query paths (`createQueryForUserSearch`) rather than in the shared `getFilterById` method

Note: Even if this specific `lookupById` path is fixed, the User LDAP filter is also applied to the second query (member user search) via `LDAPUtils.createQueryForUserSearch()` (lines 140-144). This is arguably correct behavior for the member lookup (only showing users that match the federation filter), but should be evaluated as part of the fix.
