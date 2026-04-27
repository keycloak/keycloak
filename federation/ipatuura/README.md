# IPA-Tuura Federation How To

The FreeIPA and SSSD teams have collaborated on a project to enable Keycloak to get a unified access for users and groups in FreeIPA/LDAP/Active Directory. It consists of a new Keycloak user federation provider and a bridge service to access the identity providers (FreeIPA/AD/LDAP). The bridge project, Ipa-Tuura, is based on the SCIMv2 API and provides an improved integration for classic directory services to cloud environments with an easy deployment. The bridge can also be used in a variety of different scenarios, from migration to synchronization of identities across different providers.

In this `README` we will explain how to setup the Ipa-Tuura keycloak federation using Samba AD (Ipa-Tuura Keycloak federation -> IPA Tuura bridge service -> Samba AD).

## Run Samba AD

In our setup we will be using a Samba AD image to setup a directory service for realm `KEYCLOAK.ORG`:

```
$ docker run -d --privileged  --restart=unless-stopped --network=host -e REALM='KEYCLOAK.ORG' -e DOMAIN='KEYCLOAK' -e ADMIN_PASS='Passw0rd' -e DNS_FORWARDER='8.8.8.8' -v dc1_etc:/usr/local/samba/etc -v dc1_private:/usr/local/samba/private -v dc1_var:/usr/local/samba/var --name dc1 --hostname DC1 diegogslomp/samba-ad-dc
```

Once the container is running, we need to add an entry to the host's `/etc/hosts` pointing to the AD container:

Fist, we need to get the host's IP address:

```
$ hostname -I
192.168.15.23 172.17.0.1 172.18.0.1 2804:1b3:a541:3308:ed48:33d3:7038:a0d2
```

Then we need to add an entry to `/etc/hosts` for the AD service:

```
192.168.15.23   DC1.KEYCLOAK.ORG        DC1
```

## Run IPA-Tuura bridge service

Next up we will start the IPA-Tuura bridge service:

```
$ docker run --name=ipa-bridge -d --privileged --dns 192.168.15.23 --add-host ipa-bridge.keycloak.org:192.168.15.23 -p 8000:8000 -p 3500:3500 -p 81:81 -p 443:443 --hostname ipa-bridge.keycloak.org quay.io/freeipa/ipa-tuura
```

Once the bridge is running is also need to add it to `/etc/hosts`:

```
192.168.15.23   ipa-bridge.keycloak.org
```

## Configure bridge so it can communicate with AD and Keycloak

In this step we will configure the IPA-Tuura bridge. We need to add an integration domain that tells the bridge how to connect to the Samba AD service and also to Keycloak.

The first step is to add entries to `/etc/hosts` pointing to the AD service and also to the host where Keycloak is running:

```
$ docker exec -it ipa-bridge bash
$ vi /etc/hosts
```

Then add

```
192.168.15.23   keycloak.ipa.test
192.168.15.23   DC1.keycloak.org        DC1
```

Then we need to update `/etc/krb5.conf` to configure the AD Kerberos domain:

```
includedir /etc/krb5.conf.d/

[logging]
   default = FILE:/var/log/krb5libs.log
   kdc = FILE:/var/log/krb5kdc.log
   admin_server = FILE:/var/log/kadmind.log

[libdefaults]
   dns_lookup_realm = false
   dns_lookup_kdc = false
   ticket_lifetime = 24h
   renew_lifetime = 7d
   forwardable = true
   rdns = false
   pkinit_anchors = FILE:/etc/pki/tls/certs/ca-bundle.crt
   spake_preauth_groups = edwards25519
   dns_canonicalize_hostname = fallback
   qualify_shortname = ""
   default_ccache_name = KEYRING:persistent:%{uid}
   udp_preference_limit = 0
   default_realm = KEYCLOAK.ORG

[realms]
   KEYCLOAK.ORG = {
   kdc = DC1.keycloak.org
}

[domain_realm]
   .keycloak.org = KEYCLOAK.ORG
   keycloak.org = KEYCLOAK.ORG
```

Check if realm discovery is working:

```
$ realm discover -v DC1.keycloak.org
* Resolving: _ldap._tcp.dc1.keycloak.org
* Resolving: dc1.keycloak.org
* Performing LDAP DSE lookup on: 192.168.15.23
* Successfully discovered: keycloak.org
  keycloak.org
  type: kerberos
  realm-name: KEYCLOAK.ORG
  domain-name: keycloak.org
  configured: kerberos-member
  server-software: active-directory
  client-software: sssd
  required-package: oddjob
  required-package: oddjob-mkhomedir
  required-package: sssd
  required-package: adcli
  required-package: samba-common-tools
  login-formats: %U
  login-policy: allow-realm-logins
```

You can test keycloak is reachable from the bridge by sending a GET request to `keycloak.ipa.test:8080/realms/master`:

```
$ curl keycloak.ipa.test:8080/realms/master
{"realm":"master","public_key":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwjhMTETlWs96eAuy5vYkXfImXXtocYf7EFL84tbuCCLCoWsBrRmFyd8LiixyHPOdQ/9gWlK2TualDF4lXx21zuS6L35u5rPvC6FodGEs6haOYsXmnznDsAOBDqs7XUgWQ0+Mei1ukLU1+UBFjbXTJrKlfffKZ3cZptkFgzPqZXGgKfmkOPf0sVv2RqoGTtlgYgZd3MEdqaQ14qHZIPpycOMnSRZ5S6MgE9+0Iv6v+wYMMhP8vsFcELaHOaBlOW1nLGemHLDDxgoF5C932zkZ0V61Cn4cNvJWKl7ypEKudH0X41SK8yP0I5KXijbClvo8gRoQT1aLKLqB42yH0BGYFQIDAQAB","token-service":"http://keycloak.ipa.test:8080/realms/master/protocol/openid-connect","account-service":"http://keycloak.ipa.test:8080/realms/master/account","tokens-not-before":0}[root@ipa-bridge ipa-tuura]#
```

You can also test the AD integration is working in the bridge using `kinit`:

```
$ kinit Administrator@KEYCLOAK.ORG
Password for Administrator@KEYCLOAK.ORG:
Warning: Your password will expire in 36 days on Wed Jan  8 15:24:44 2025
```

```
$ klist
Ticket cache: KEYRING:persistent:0:0
Default principal: Administrator@KEYCLOAK.ORG

Valid starting     Expires            Service principal
12/02/24 20:01:56  12/03/24 06:01:56  krbtgt/KEYCLOAK.ORG@KEYCLOAK.ORG
renew until 12/09/24 20:01:54
```

This ensures your `krb5.conf` is working.

Now leave the session open and listening to the `httpd` error logs to catch error messages in case you face errors when adding the integration domain in the next step:

```
$ tail -f /var/log/httpd/error_log
```

## Add an integration domain to the bridge service

The integration domain is configured in a JSON file that has to be sent to the bridge via HTTP. Send the following request from the actual host:

```
$ curl -v -k -X POST "https://ipa-bridge.keycloak.org/domains/v1/domain/" -H "accept: application/json" -H "Content-Type: application/json" -H "X-CSRFToken: x1yU9RGPKs4mJdWIOzEc7wKbwbnJ0B6iTHuW6ja0gdBpEOBVacK1vIhSSYlfsnRw" -d @integrationdomain.json
```

The `integrationdomain.json` file looks like this:

```
{
   "id": "1",
   "name": "keycloak.org",
   "description": "AD Integration Domain",
   "integration_domain_url": "ldap://DC1.keycloak.org",
   "client_id": "Administrator@KEYCLOAK.ORG",
   "client_secret": "Passw0rd",
   "id_provider": "ad",
   "user_extra_attrs": "mail:mail, sn:sn, givenname:givenname",
   "user_object_classes": "user,organizationalPerson,person,top",
   "users_dn": "CN=Users,DC=keycloak,DC=org",
   "ldap_tls_cacert": "/etc/openldap/certs/cacert.pem",
   "keycloak_hostname": "keycloak.ipa.test"
}
```

Check to verify the integration domain was added with:

```
$ curl -k -X GET "https://ipa-bridge.keycloak.org/domains/v1/domain/" -H "accept:application/json" -H "X-CSRFToken: x1yU9RGPKs4mJdWIOzEc7wKbwbnJ0B6iTHuW6ja0gdBpEOBVacK1vIhSSYlfsnRw"
```

### Important observations:
1- `"name"` must match the AD domain name (i.e. `keycloak.org`)

2- in `"integration_domain_url"`, use `ldap` instead of `ldaps`

3- the `"ldap_tls_cert"` is not actually used in this setup, just set any value

4- `"keycloak_hostname"` must resolve to the host running keycloak (see step above to ensure it was added to `/etc/hosts`)

## Setup outgoing certificates

Retrieve the certificate from the bridge and add it to a keystore file.

```
$ openssl s_client -connect bridge.ipa.test:443 2>/dev/null </dev/null |  sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /opt/keycloak/bridge.crt

$ keytool -importcert -alias bridge -file /opt/keycloak/bridge.crt -keystore /opt/keycloak/keystore.jks -trustcacerts -storepass redhat -noprompt
```

The `/opt/keycloak/keystore.jks` keystore file must be copied to the keycloak system or container.

## Setup the IPA-Tuura user federation in keycloak

First start keycloak with the `ipa-tuura-federation` feature enabled and truststore arguments:

```
$ ./kc.sh start-dev --features=ipa-tuura-federation --spi-truststore-file-file=/opt/keycloak/keystore.jks --spi-truststore-file-password=redhat --spi-truststore-file-hostname-verification-policy=ANY
```

Log into the admin console, go to `User Federation`, then click on `Add new provider` and select `Ipatuura`

Add the following settings:

```
Ipatuura Server URL: ipa-bridge.keycloak.org
Login username: scim
Login password: Secret123
```

Now you can try fetching users from AD by username in the `Users` section. Creating users should propagate them to the SambaAD as well.

NOTE: For the moment, the bridge service allows searching for users by username only. Broad searches like the one performed by the admin console when we click on `Users` return an empty stream. So to test the integration you need to fetch users by username.

## Further reading

IPA-Tuura bridge service:
https://github.com/freeipa/ipa-tuura?tab=readme-ov-file

Keycloak Ipa-Tuura storage provider:
https://github.com/justin-stephenson/scim-keycloak-user-storage-spi/?tab=readme-ov-file#plugin-communication

FOSDEM'24 presentation about the new storage provider and the IPA-Tuura bridge service:
https://archive.fosdem.org/2024/schedule/event/fosdem-2024-2618-ipa-tuura-freeipa-connector-for-keycloak/