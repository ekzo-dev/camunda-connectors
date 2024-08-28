FROM eclipse-temurin:21.0.4_7-jre

VOLUME /tmp

# The /opt/app is used for the Connectors runtime, and out-of-the-box connectors
# Use the /opt/custom to mount your own connectors, secret providers, or include other jars into the classpath
RUN mkdir /opt/app && mkdir /opt/custom

COPY start.sh /start.sh
RUN chmod +x start.sh

# Copy Connector runtime from local build
COPY target/ekzo-connectors-application-*.jar /opt/app/ekzo-connectors-application.jar

# Create an unprivileged user / group and switch to that user
RUN groupadd --gid 1001 camunda && useradd --no-create-home --gid 1001 --uid 1001 camunda
USER 1001:1001

# Use entry point to allow downstream images to add JVM arguments using CMD
ENV ZEEBE_CLIENT_CONFIG_PATH=/tmp/connectors
ENTRYPOINT ["/start.sh"]
