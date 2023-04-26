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
