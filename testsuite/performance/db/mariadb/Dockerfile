FROM mariadb:10.3

ADD wsrep.cnf /etc/mysql/conf.d/

ADD mariadb-healthcheck.sh /usr/local/bin/
RUN chmod -v +x /usr/local/bin/mariadb-healthcheck.sh
HEALTHCHECK --interval=5s --timeout=5s --retries=12 CMD ["mariadb-healthcheck.sh"]

ENV DATADIR /var/lib/mysql
ADD docker-entrypoint-wsrep.sh /usr/local/bin/
RUN chmod -v +x /usr/local/bin/docker-entrypoint-wsrep.sh
