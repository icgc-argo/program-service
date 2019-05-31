program-service
---

The central point to create and manage programs and maintain their metadata. 

## Development

### Protocol buffers submodule
Protobuf files are stored at a [separate repository](https://github.com/icgc-argo/argo-proto), to init the submodule

```sh
git submodule update --init
```

### Database
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

### Run Spring Boot
```sh
./mvnw spring-boot:run
```

## Test

### Unit Test
```sh
./mvnw test
```

### Integration Test
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

### Local Developement

#### Getting started
Since the EGO service is a dependency, having a local running docker instance can help the developement of unit tests and application code. Here are the steps for local developement:
1. Run `make dev-start` to start all the services needed for developement.
2. Run you program-service in the `dev` profile. This is already configured to listen to the ports forwarded by the previous step. To find the correct port forwarding, run `make dev-ps`. 

#### Additional commands
1. Run `make dev-ps` to see a list of all forwarded ports
2. Run `make dev-logs` to see the logs of all the running dev services
3. Run `make dev-stop` to kill all services and remove the containers
4. Run `make fresh-ego` to restart a fresh and empty EGO service.

#### Useful services:
1. **Mail server**

    A mail server called `MailHog` is used to capture all email activity from the program service. The default url of the MailHog UI is http://localhost:9025. 

2. **Spring Boot Admin server**

    A spring boot admin server is used to monitor and manage the program service. The default url is http://localhost:9081.


### Demo Mode
In this mode, the local program service code is built and run as a docker container along with all the dependent services. This allows the whole system to be demoed without having any external network dependency. Use the following commands to manage a demo:

- `make demo-start`: Starts all the services
- `make demo-stop` : Stops all the services and removes the instances
- `make demo-logs` : Shows the logs from all the services
- `make demo-ps`   : Shows all the running services and their ports
