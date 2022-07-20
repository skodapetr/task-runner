# Deprecated
This repository is no longer maintained.

# Task Runner
[![Maintainability](https://api.codeclimate.com/v1/badges/7e8ac60fa925731d15f2/maintainability)](https://codeclimate.com/github/skodapetr/task-runner/maintainability)

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
Create a new ```echo``` task with identifier ```hello world```:
```
curl http://localhost:8020/api/v1/task/echo/hello%20world
```
Retrieve status and output file:
```
curl http://localhost:8020/api/v1/task/echo/hello%20world
curl http://localhost:8020/api/v1/task/echo/hello%20world/stdout
```

## Task specification
Tasks can be specified using YAML or JSON file format. For each task following
options can be specified:
 * ```id``` - name of the data directory.
 * ```urlPath``` - used in URL to identify the task.
 * ```allowInputFiles``` - allow input files for POST methods.
 * ```createOnGet``` - create new task when user perform GET for non-existing 
   task.
 * ```mergeErrOutToStdOut```
 * ```readOnly``` - task can not be created or deleted.
 * ```disableListing``` - disable listing of all tasks, use for large number
   of tasks.
 * ```steps``` - commands to execute.
 * ```taskGetIdentificationTransformation``` - define transformation of task ID
   obtained from the URL.
 * ```allowGzipPublicFiles``` - if true and user ask for public file the GZIP
   version of the file can be served.
 * ```timeToLiveMinutes``` - how long must be a task preserved after it has
   been finished.
 * ```postResponseRedirectUrl``` - optional URL template. If provided and 
   user POST a new task the response is redirect to this URL.

For more information see [TaskTemplate.java](./task-runner-storage/src/main/java/cz/skodape/taskrunner/storage/template/model/TaskTemplate.java).

### Options taskGetIdentificationTransformation
Supported values are:
 * ```none``` - default, no transformation.
 * ```lowercase``` - change to lowercase.
 * ```uppercase``` - change to uppercase.

