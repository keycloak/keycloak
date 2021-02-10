# Keycloak Arquillian Integration Testsuite

This directory contains a OpenSSL CA and Intermediate CA that can be used to manage certificates.

## Passwords

Passwords for any key file is `password`.

## Steps to create a client certificate

### Create a private key for the client

openssl genrsa -aes256 -out signed/clients/test-user@localhost.key.pem 4096
chmod 400 signed/clients/test-user@localhost.key.pem

### Create a CSR for the client

openssl req -config intermediate/openssl.cnf -key signed/clients/test-user@localhost.key.pem -new -sha256 -out signed/clients/test-user@localhost.csr.pem

If you want to generate a CSR with extensions you can use a command similar to the following:

openssl req -config intermediate/openssl-san.cnf -key signed/clients/test-user@localhost.key.pem -new -sha256 -out signed/clients/test-user@localhost.csr.pem

### Create a certificate using the CSR

openssl ca -config intermediate/openssl.cnf -extensions usr_cert -days 375 -notext -md sha256 -in signed/clients/test-user@localhost.csr.pem -out signed/clients/test-user@localhost.cert.pem

chmod 444 signed/clients/test-user@localhost.cert.pem

### Verify the certificate

openssl x509 -noout -text -in signed/clients/test-user@localhost.cert.pem

### Check if certificate has a valid chain of trust

openssl verify -CAfile intermediate/certs/ca-chain.cert.pem signed/clients/test-user@localhost.cert.pem

### Transform both certificate and private key to PKCS12 format

openssl pkcs12 -export -in signed/clients/test-user@localhost.cert.pem -inkey signed/clients/test-user@localhost.key.pem -out signed/clients/test-user@localhost.p12 -name test-user -CAfile intermediate/certs/ca-chain.cert.pem