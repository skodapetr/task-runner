# Task Runner
Task runner allows you to expose executable files in form of a HTTP service.

## Requirements
 * Java 11

## Installation and startup
Task runner can be compiled from source using few simple steps.
```
git clone https://github.com/skodapetr/task-runner.git
cd task-runner
./gradlew installDist
```
Start the server with example data.
```
./dist/bin/task-runner-cli \
    --TemplatesDirectory=./example/templates \
    --TaskDirectory=./example/tasks \
    --WorkingDirectory=./example/working \
    --WorkerCount=1 \
    --HttpPort=8020
```                                                                                                                                                     
Crete a task:
```
curl http://localhost:8020/api/v1/task/echo/hello%20world
```
Retrieve status and output file:
```
curl http://localhost:8020/api/v1/task/echo/hello%20world
curl http://localhost:8020/api/v1/task/echo/hello%20world/public/result
```