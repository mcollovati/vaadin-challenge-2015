FROM registry.eu-gb.bluemix.net/ibmliberty
EXPOSE 9080
COPY challenge.war /opt/ibm/wlp/usr/servers/defaultServer/dropins/
ENV LICENSE accept
CMD ["/root/bin/run_supervisor"]
