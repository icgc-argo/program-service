## Program Service


## Table of Contents

- [Objective](#objective)
- [Development](#development)
  - [Protocol Buffers](#protocol-buffers)
    - [NPM Program-Service-Proto Package](#npm-program-service-proto-package)
  - [Generate Protocol buffers Java files](#generate-protocol-buffers-java-files)
- [Running the Service](#running-the-service)
  - [Local](#local)
    - [Database](#database)
    - [Run Spring Boot](#run-spring-boot)
  - [Docker](#docker)
    - [Getting Started](#getting-started)
    - [Additional Commands](#additional-commands)
    - [Useful Services](#useful-services)
- [Testing](#testing)
  - [Unit Testing](#unit-testing)
  - [Integration Testing](#integration-testing)
  - [Test the running instances at DEV/QA](#test-the-running-instances-at-devqa)
  - [Test email](#test-email)
  - [Mocking service using WireMock](#mocking-service-using-wiremock)
- [Migrations and Rollbacks](#migrations-and-rollbacks)
  - [Run Migrations](#run-migrations)
  - [Rollback Migrations](#rollback-migrations)
- [Demo Mode](#demo-mode)
- [Notes](#notes)

## Objective

The central point to create and manage programs and maintain their metadata.

## Development

### Protocol Buffers

The Protocol Buffer file that define the GRPC interface can be found at [./src/main/proto/ProgramService.proto](https://github.com/icgc-argo/program-service/tree/master/src/main/proto)

#### NPM Program-Service-Proto Package

This service is supporting an NPM package which provides the proto file for NodeJS clients. The package is found here [@icgc-argo/program-service-proto](https://www.npmjs.com/package/@icgc-argo/program-service-proto). This package should published for every Program-Service release with a matching version number. Whenever a proto change is made, it is important to update the version number for the npm package and for Program-Service.

To publish, you need to have a [NPMJS](https://www.npmjs.com) account and be a member of the ICGC-Argo organization. With this, publish using `make publish-npm`. Make sure you have updated the version number in the package.json file.

### Generate Protocol buffers Java files

```sh
./mvnw compile
```

## Running the Service

### Local

#### Database

Run postgres at port 5432

```sh
docker run --name postgres -d -p 5432:5432 postgres
```

Create database program_db

```sh
psql -h localhost -p 5432 -U postgres -c 'create database program_db'
```

Set up database schema (although it could be done [automatically](https://github.com/spring-projects/spring-boot/blob/v2.1.5.RELEASE/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/flyway/FlywayProperties.java))

```sh
./fly.sh info
./fly.sh migrate
```

#### Run Spring Boot

```sh
./mvnw spring-boot:run
```

### Docker

#### Getting Started

Since the EGO service is a dependency, having a local running docker instance can help the developement of unit tests and application code. Here are the steps for local developement:

1. Run `make dev-start` to start all the services needed for developement. These services **DO NOT** have security enabled.
2. Run the program-service in the `debug` profile. This is already configured to listen to the ports forwarded by the previous step. To find the correct port forwarding, run `make dev-ps`.

#### Additional Commands

1. Run `make dev-ps` to see a list of all forwarded ports
2. Run `make dev-logs` to see the logs of all the running dev services
3. Run `make dev-stop` to kill all services and remove the containers
4. Run `make fresh-ego` to restart a fresh and empty EGO service. Useful for when your EGO service contains alot of junk and you just want a clean slate.

#### Useful Services

1. **Mail server**

   A mail server called `MailHog` is used to capture all email activity from the program service. The default url of the MailHog UI is http://localhost:9025.

2. **Spring Boot Admin server**

   A spring boot admin server is used to monitor and manage the program service. The default url is http://localhost:9081.

## Testing

### Unit Testing

```sh
./mvnw test
```

### Integration Testing

```sh
./mvnw verify
```

Note, the above command run unit tests as well. It uses your local database and run tests against DEV/QA environment.

### Test the running instances at DEV/QA

First, port forward the port which is serving grpc services (make sure you have access to the clusters).

```
kubectl port-forward --namespace qa svc/program-service-qa 50051
```

Then, use your favorite grpc debugging tool to test individual services, for example, using [grpc_cli](https://github.com/grpc/grpc/blob/master/doc/command_line_tool.md) to list all services and create program

```
grpc_cli list localhost:50051
grpc_cli call localhost:50051 CreateProgram "program: {name: 'programName', short_name: 'shortName'}"
```

If you prefer GUI interfaces, try [bloomrpc](https://github.com/uw-labs/bloomrpc)

### Test email

All emails sent are captured by [mailhog](https://mailhog.qa.cancercollaboratory.org) for both the developer's local machine or the Jenkins that run CI/CD.

### Mocking service using WireMock

Why use WireMock? 

When writing Ego integration tests, instead of calling actual Ego endpoints (which would pollute Ego's database with test data, or test failure if the Ego service is shutdown), we can mock Ego endpoints with WireMock.
WireMock starts a mock server at a given or random port. For example, to define a WireMock rule at a random port:
```
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());
```

The response can also be mocked:
```
    stubFor(get(urlEqualTo(url))
        .willReturn(aResponse()
          .withStatus(OK.value())
          .withHeader("Content-Type", "application/json")
          .withBodyFile(filename)));
```
Note that the "filename" is the name of a self-defined json file which mocks response body.
All mocked json response files are located in `src/test/resources/__files`. 

## Migrations and Rollbacks

Migrations are being managed with flyway. The migration scripts are found in `src/main/resources/flyway/sql`.

A script (`fly.sh`) has been provided for convenience to run flyway through maven, using the config file (`src/main/resources/flyway/conf/flyway.conf`).

### Run Migrations

To run all migrations, use the commands:

```sh
./fly.sh info
./fly.sh migrate
```

### Rollback Migrations

The free version of flyway does not allow 'undo' operations, so rollbacks need to be applied manually.

Rollback scripts are included in the same directory as migrations, however they are named with a prefix `U` instead of `V` (flyway's convention). The free Community Edition of flyway does not support the undo operation, so these scripts need to be applied manually.   
 
## Demo Mode

In this mode, the local program service code is built and run as a docker container along with all the dependent services. This allows the whole system to be demoed without having any external network dependency. Use the following commands to manage a demo:

- `make demo-start`: Starts all the services
- `make demo-stop` : Stops all the services and removes the instances
- `make demo-logs` : Shows the logs from all the services
- `make demo-ps` : Shows all the running services and their ports

## Notes

TOC generated by [gh-md-toc](https://github.com/ekalinin/github-markdown-toc)


Tests with `@Transactional` will not commit changes to DB (because they are always rolled back) and postgres doesn't allow reading from uncommitted changes. As a result some nested queries may not work. In such a case use repo functions to access db directly instead of using grpc functions.