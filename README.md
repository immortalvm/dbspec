# DbSpec interpreter

An interpreter for the DbSpec language.

## Prerequisites

### JDK

A Java Development Kit, version 8 or greater must be installed.
On Debian and Debian-based distros the package ```openjdk-11-jdk-headless``` is known to work.

The environment variable ```JAVA_HOME``` must be set.
For example:

```shell
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

### Ant

Apache Ant is required for building the project.
On Debian and Debian-based distros the package ```ant``` can be installed.

### Docker

Docker is required for running the DBMS used for testing.
On Debian and Debian-based distros the package ```docker.io``` can be installed.

Add your user to the group ```docker```.

### PostgreSQL Client

The PostgreSQL client is is required for setting up a test database
On Debian and Debian-based distros the package ```postgresql-client-14``` can be installed.

## Preparing

Recursively clone the project with submodules:

```shell
git clone https://github.com/immortalvm/iDA-DbSpec-interpreter.git --recursive
```

Or clone first and update the submodules then:

```shell   
git clone https://github.com/immortalvm/iDA-DbSpec-interpreter.git
git submodule update --init --recursive  
# or:  git submodule init && git submodule update
```

## Setting up PostgreSQL for testing

To run the test script, we need to set up PostgreSQL in Docker container.
```shell
docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=geheim -v postgres:/var/lib/postgresql/data postgres:14
```

## Building

Build the interpreter as a jar file (dbspec.jar):

```shell
ant clean
ant create_run_jar
```

## Running

To run a DbSpec file:

```shell
java -jar dbspec.jar test.dbspec
```

## Installing the DbSpec Emacs mode

The DbSpec Emacs mode offers syntax validation.
It requires Emacs version 29 or higher, and it can be installed as follows:

```shell
mkdir ~/.emacs.d/tree-sitter
cp libtree-sitter-dbspec.so ~/.emacs.d/tree-sitter
cp dbspec-ts-mode.el ~/.emacs.d
```
