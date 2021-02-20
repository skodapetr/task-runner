# Task Runner
[![Maintainability](https://api.codeclimate.com/v1/badges/7e8ac60fa925731d15f2/maintainability)](https://codeclimate.com/github/skodapetr/task-runner/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/7e8ac60fa925731d15f2/test_coverage)](https://codeclimate.com/github/skodapetr/task-runner/test_coverage)
[![Known Vulnerabilities](https://snyk.io/test/github/skodapetr/task-runner/master/badge.svg)](https://snyk.io/test/github/skodapetr/task-runner/master)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fskodapetr%2Ftask-runner.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fskodapetr%2Ftask-runner?ref=badge_shield)


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


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fskodapetr%2Ftask-runner.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fskodapetr%2Ftask-runner?ref=badge_large)