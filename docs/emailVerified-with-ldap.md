## EmailVerified with ldap 

If ldap user federation is used and the emailVerified details need to be updated in ldap following changes are needed.


### Keycloak configuration

In the  `User Federation > Ldap` settings, the `User Object Classes` value should be `inetOrgPerson, organizationalPerson,emailVerification` and `Edit Mode` should be `WRITABLE`. Fill the other details as how nomral connection is made to ldap

In the `User Federation > Ldap > LDAP Mappers` create a new `user-attribute-ldap-mapper` with `User Model Attribute` value as `emailVerified` and `LDAP Attribute` value as `emailVerified`.

### LDAP changes

A new ObjectClass `emailVerification` needs to be added with Attribute `emailVerified`

```
ldapmodify -v -D "${BIND-DN}" -w "${BIND-PASSWORD}" <<!
dn: cn=schema
changetype: modify
add: attributeTypes
attributeTypes: ( 2.25.28639311321113238241701611583088740684.14.2.2
  NAME 'emailVerified'
  EQUALITY booleanMatch
  SYNTAX 1.3.6.1.4.1.1466.115.121.1.7 )
-
add: objectClasses
objectClasses: ( 2.25.28639311321113238241701611583088740684.14.2.3
  NAME 'emailVerification'
  DESC 'emailVerification'
  SUP inetOrgPerson
  STRUCTURAL
  MAY (emailVerified)
)
!
```

New users will have `emailVerified` attribute after below command.
```
ipa config-mod --addattr=ipaUserObjectClasses=emailVerification
```


For existing users, the attribute has to be added per user.
```
ipa user-mod --addattr objectclass=emailVerification
ipa user-mod --addattr emailVerified=FALSE
```

Note : If the existing users are not updated, they won't be displayed in keycloak after `emailVerification` is added to `User Object Classes` value of LDAP user federation setting.

