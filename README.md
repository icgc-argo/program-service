# program-service

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

