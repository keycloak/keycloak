FROM registry.access.redhat.com/ubi9

ENV VERSION=18
ENV PGUSER=enterprisedb
ENV PGPASSWORD=password
ENV PGDATABASE=keycloak
ENV PGPORT=5432

ENV PGDATA=/var/lib/edb/as${VERSION}/data

# Get the token at https://www.enterprisedb.com/repos-downloads
RUN --mount=type=secret,id=edb_repo_token,required=true \
    export EDB_REPO_TOKEN=$(cat /run/secrets/edb_repo_token) && \
    (curl -1sSLf "https://downloads.enterprisedb.com/${EDB_REPO_TOKEN}/enterprise/setup.rpm.sh" | bash) && \
    dnf -y install edb-as${VERSION}-server

USER enterprisedb
WORKDIR /usr/edb/as${VERSION}/bin/

COPY init-and-start-db.sh .
CMD ./init-and-start-db.sh
EXPOSE ${PGPORT}