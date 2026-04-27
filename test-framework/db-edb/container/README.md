# Create test EDB image

1. Get EDB repo token from Get the token at https://www.enterprisedb.com/repos-downloads.
2. Build the multi-arch image:
```
EDB_TOKEN=[your token] podman build --secret id=edb_repo_token,env=EDB_TOKEN --manifest quay.io/keycloakqe/enterprisedb:[version] --platform=linux/arm64,linux/amd64 .
```
3. Push the multi-arch image:
```
podman manifest push quay.io/keycloakqe/enterprisedb:[version]
```