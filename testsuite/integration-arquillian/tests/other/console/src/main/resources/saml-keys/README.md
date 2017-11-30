# Generate the Key
openssl genrsa -out client.key 2048

# Create a signing request
openssl req -new -key client.key -out certificate.csr -subj "/C=US/ST=MA/L=Boston/O=Red Hat/OU=Keyloak/CN=saml.client.local"

# PEM
openssl x509 -req -in certificate.csr -signkey client.key -out client.pem

# PKCS12
openssl pkcs12 -export -passin pass:secret -password pass:secret -in client.pem -inkey client.key -out client.p12 -name "samlKey"

# JKS
keytool -importkeystore -destkeystore client.jks -deststorepass secret -srckeystore client.p12 -srcstoretype PKCS12 -srcstorepass secret
