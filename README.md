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

## Building

Before you can build the DbSpec interpreter, you need to build a shared library:

```shell
./build.py -o libjava-tree-sitter tree-sitter-dbspec
```

Then build the interpreter as a jar file (dbspec.jar):

```shell
ant build-project
ant create_run_jar
```

## Running

To interpret a DbSpec file:

```shell
java -jar dbspec.jar tree-sitter-dbspec/examples/complete_example.dbspec
```
