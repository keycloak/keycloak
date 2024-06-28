# Keycloak Arquillian Integration Testsuite

This directory contains a OpenSSL CA and Intermediate CA that can be used to manage certificates.

## Passwords

Passwords for any key file is `password`.

## Steps to create a client certificate

In the instructions below, you may usually need to create your own files for private key, CSR request, certificate , p12 and
also possibly custom openssl configuration. For the instructions below, replace the file names according your needs (For example
replace `test-user@localhost.key.pem` with something like `test-user-some@localhost.key.pem` )

### Create a private key for the client

openssl genrsa -aes256 -out certs/clients/test-user@localhost.key.pem 4096
chmod 400 certs/clients/test-user@localhost.key.pem

### Create a CSR for the client

openssl req -config intermediate/openssl.cnf -key certs/clients/test-user@localhost.key.pem -new -sha256 -out certs/clients/test-user@localhost.csr.pem

If you want to generate a CSR with extensions you can use a command similar to the following:

openssl req -config intermediate/openssl-san.cnf -key certs/clients/test-user@localhost.key.pem -new -sha256 -out certs/clients/test-user@localhost.csr.pem

### Create a certificate using the CSR

openssl ca -config intermediate/openssl.cnf -extensions usr_cert -days 375 -notext -md sha256 -in certs/clients/test-user@localhost.csr.pem -out certs/clients/test-user@localhost.cert.pem

chmod 444 certs/clients/test-user@localhost.cert.pem

### Verify the certificate

openssl x509 -noout -text -in certs/clients/test-user@localhost.cert.pem

### Check if certificate has a valid chain of trust

openssl verify -CAfile intermediate/certs/ca-chain.cert.pem certs/clients/test-user@localhost.cert.pem

### Transform both certificate and private key to PKCS12 format

openssl pkcs12 -export -in certs/clients/test-user@localhost.cert.pem -inkey certs/clients/test-user@localhost.key.pem -out certs/clients/test-user@localhost.p12 -name test-user -CAfile intermediate/certs/ca-chain.cert.pem
