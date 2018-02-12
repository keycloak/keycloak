FROM mariadb:10.3.3

ARG MAX_CONNECTIONS=100

ADD wsrep.cnf.template /etc/mysql/conf.d/
RUN sed -e s/@MAX_CONNECTIONS@/$MAX_CONNECTIONS/ /etc/mysql/conf.d/wsrep.cnf.template > /etc/mysql/conf.d/wsrep.cnf; cat /etc/mysql/conf.d/wsrep.cnf

ADD mariadb-healthcheck.sh /usr/local/bin/
RUN chmod -v +x /usr/local/bin/mariadb-healthcheck.sh
HEALTHCHECK --interval=5s --timeout=5s --retries=12 CMD ["mariadb-healthcheck.sh"]

ENV DATADIR /var/lib/mysql
ADD docker-entrypoint-wsrep.sh /usr/local/bin/
RUN chmod -v +x /usr/local/bin/docker-entrypoint-wsrep.sh
