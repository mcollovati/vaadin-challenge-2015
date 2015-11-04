FROM registry.eu-gb.bluemix.net/ibmliberty
EXPOSE 9080
EXPOSE 22
EXPOSE 80

COPY server.xml /opt/ibm/wlp/usr/servers/defaultServer/
COPY challenge.war /opt/ibm/wlp/usr/servers/defaultServer/dropins/
ENV LICENSE accept
COPY bluemix.pub /root/.ssh/
RUN cat /root/.ssh/bluemix.pub >> /root/.ssh/authorized_keys

CMD ["/root/bin/run_supervisor"]
