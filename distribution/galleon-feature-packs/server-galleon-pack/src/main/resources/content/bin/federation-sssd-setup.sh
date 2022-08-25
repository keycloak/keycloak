#!/bin/sh

# Setup for SSSD
SSSD_FILE="/etc/sssd/sssd.conf"

if [ -f "$SSSD_FILE" ];
then

  if ! grep -q ^ldap_user_extra_attrs $SSSD_FILE; then 
    sed -i '/ldap_tls_cacert/a ldap_user_extra_attrs = mail:mail, sn:sn, givenname:givenname, telephoneNumber:telephoneNumber' $SSSD_FILE
  fi

  if ! grep -q ^services.*ifp.* /etc/sssd/sssd.conf; then
    sed -i '/^services/ s/$/, ifp/' $SSSD_FILE
  fi

  if ! grep -q ^allowed_uids $SSSD_FILE; then 
    sed -i '/\[ifp\]/a allowed_uids = root' $SSSD_FILE
  fi

  if ! grep -q ^user_attributes $SSSD_FILE; then 
    sed -i '/allowed_uids/a user_attributes = +mail, +telephoneNumber, +givenname, +sn' $SSSD_FILE
  fi

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
