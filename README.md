# DbSpec interpreter

An interpreter for the DbSpec language.

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

Currently, the recommended way to build the interpreter is using Docker (see ```Dockerfile```)
and execute the specifications in extensions of this image (see ```Dockerfile``` in ```examples``` and its subdirectories).

On Debian and Debian-based distros the package ```docker.io``` can be installed.
Add your user to the group ```docker```.


### Building and running without Docker

So far, the interpreter has only been tested on the Linux platform.

A Java Development Kit, version 8 or greater must be installed.
On Debian and Debian-based distros the package ```openjdk-11-jdk-headless``` is known to work.

The environment variable ```JAVA_HOME``` must be set.
For example:

```shell
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

Apache Ant is required for building the project.
On Debian and Debian-based distros the package ```ant``` can be installed.

Finally, you also need Python 3 and gcc.

Build the interpreter as a jar file (dbspec.jar):

```shell
ant clean
ant create_run_jar
```

To run a DbSpec file:

```shell
java -jar dbspec.jar test.dbspec
```


## Installing the DbSpec Emacs mode

The DbSpec Emacs mode offers syntax highlighting and validation when editing .dbspec files.
It requires Emacs version 29 or higher, and it can be installed as follows:

```shell
mkdir ~/.emacs.d/tree-sitter
cp libtree-sitter-dbspec.so ~/.emacs.d/tree-sitter
cp dbspec-ts-mode.el ~/.emacs.d
```
