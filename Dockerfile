FROM rockylinux:8
COPY rpm/target/rpm/com.teragrep-rlp_08/RPMS/noarch/com.teragrep-rlp_08-*.rpm /rpm/
RUN dnf -y install java-17-openjdk-headless /rpm/*.rpm && dnf clean all
WORKDIR /opt/teragrep/rlp_08
ENTRYPOINT [ "/usr/bin/java", "-jar", "lib/rlp_08.jar" ]
