#!/bin/sh

# Setup for SSSD
SSSD_FILE="/etc/sssd/sssd.conf"

if [ -f "$SSSD_FILE" ];
then
  sed -i '/ldap_tls_cacert/a ldap_user_extra_attrs = mail:mail, sn:sn, givenname:givenname, telephoneNumber:telephoneNumber' $SSSD_FILE
  sed -i 's/nss, sudo, pam/nss, sudo, pam, ifp/' $SSSD_FILE
  sed -i '/\[ifp\]/a allowed_uids = root\nuser_attributes = +mail, +telephoneNumber, +givenname, +sn' $SSSD_FILE
  systemctl restart sssd
else
  echo "Please make sure you have $SSSD_FILE into your system! Aborting."
  exit 1
fi

# Setup for PAM
PAM_FILE="/etc/pam.d/keycloak"

if [ ! -f "$PAM_FILE" ];
then
cat <<EOF > $PAM_FILE
  auth    required   pam_sss.so
  account required   pam_sss.so
EOF
else
  echo "$PAM_FILE already exists. Skipping it..."
  exit 0
fi


