FROM google/cadvisor:v0.26.1
RUN apk add --no-cache bash curl
ADD entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
