openssl genrsa -des3 -out server.key 1024
openssl req -new -key server.key -out server.csr
openssl x509 -req -days 3650 -in server.csr -signkey server.key -out server.crt
cp server.key server.key.copy
openssl rsa -in server.key.copy -out server.key
rm server.key.copy
