FROM registry.eu-gb.bluemix.net/ibmliberty
EXPOSE 9080
COPY server.xml /opt/ibm/wlp/usr/servers/defaultServer/
COPY challenge.war /opt/ibm/wlp/usr/servers/defaultServer/dropins/
ENV LICENSE accept
# CMD ["/root/bin/run_supervisor"]
