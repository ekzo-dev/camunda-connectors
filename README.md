
# Ekzo Connectors + Ekzo Connectors Application

TBD: description here

## Build

You can package all modules by running the following command:

```bash
mvn clean package
```

This will create the following artifacts:

- A JAR of ekzo-connector-inbound-graphql without dependencies.
- An executable spring boot JAR of ekzo-connectors-application

ekzo-connectors-application includes connectors:

- ekzo-connector-inbound-graphql
- connector-http-json
- connector-graphql
- connector-rabbitmq
- connector-sendgrid
- connector-aws-sns
- connector-aws-sqs
- connector-kafka
- connector-microsoft-teams
- connector-aws-dynamodb
- connector-aws-lambda
- connector-google-sheets
- connector-slack
- connector-webhook
- connector-http-polling

## Docker

You can build docker image of ekzo-connectors-application.

```bash
cd ekzo-connectors-application
docker build -t ekzo-connectors .
```

This will create docker image with name ekzo-connectors.

Include it in your docker-compose to run ekzo-connectors-application.
