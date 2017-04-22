FROM ubuntu

RUN apt-get update && apt-get install -y apache2 openssh-server mc && apt-get install -y libapache2-mod-auth-mellon

RUN mkdir /etc/apache2/mellon

COPY mellon/* /etc/apache2/mellon/

COPY auth_mellon.conf /etc/apache2/mods-enabled/

COPY www/* /var/www/html/

RUN mkdir /var/www/html/auth2

COPY www/auth2/* /var/www/html/auth2/

RUN echo 'root:a' | chpasswd

RUN mkdir /var/run/sshd

RUN sed -ri 's/^PermitRootLogin\s+.*/PermitRootLogin yes/' /etc/ssh/sshd_config

RUN sed -ri 's/UsePAM yes/#UsePAM yes/g' /etc/ssh/sshd_config

CMD bash -c '(/usr/sbin/sshd -D & ) ; /usr/sbin/apache2ctl -D FOREGROUND'
