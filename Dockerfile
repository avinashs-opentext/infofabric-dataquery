FROM registry-master.at4d.liacloud.com/alloy/jre:3.4.3

COPY --chown=$ALLOY_USER:$ALLOY_USER ./build/install/dm-dataquery /opt/dm-dataquery

COPY --chown=$ALLOY_USER:$ALLOY_USER ./containerstart.sh /opt/start.sh

USER $ALLOY_USER

ENTRYPOINT ["/opt/start.sh"]
