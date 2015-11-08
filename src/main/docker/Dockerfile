FROM registry.eu-gb.bluemix.net/mcvd/websphere-liberty:beta

# Aggiorna i pacchetti
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get -y install \
    dnsutils \
    openssh-server \
    supervisor &&\
  apt-get clean &&\
  rm -Rf /var/cache/*

# SSH
EXPOSE 22
RUN mkdir -p /var/run/sshd &&\
    mkdir -p /root/.ssh/ &&\
    touch /root/.ssh/authorized_keys &&\
    sed -i 's/session \+required \+pam_loginuid\.so/session    optional     pam_loginuid.so/' /etc/pam.d/sshd &&\
    sed -i 's/.*PasswordAuthentication yes/PasswordAuthentication no/g' /etc/ssh/sshd_config &&\
    sed -i 's/.*UsePAM yes/UsePAM no/g' /etc/ssh/sshd_config &&\
    sed -i 's/.*ChallengeResponseAuthentication yes/ChallengeResponseAuthentication no/g' /etc/ssh/sshd_config

# Accetta la licenza Liberty
COPY liberty.conf /etc/supervisor/conf.d/liberty.conf
ENV LICENSE accept

# Configura il supervisore
COPY sshd.conf /etc/supervisor/conf.d/sshd.conf
RUN mkdir -p /root/bin/ &&\
    echo "/usr/bin/supervisord -n" > /root/bin/run_supervisor &&\
    chmod 755 /root/bin/run_supervisor


EXPOSE 80 9080

COPY server.xml /opt/ibm/wlp/usr/servers/defaultServer/
COPY challenge.war /opt/ibm/wlp/usr/servers/defaultServer/dropins/
COPY bluemix.pub /root/.ssh/
RUN cat /root/.ssh/bluemix.pub >> /root/.ssh/authorized_keys

RUN /opt/ibm/wlp/bin/installUtility install --acceptLicense concurrent-1.0

# Esegui Liberty tramite il supervisore
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["/root/bin/run_supervisor"]
