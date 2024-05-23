# DbSpec interpreter

An interpreter for the DbSpec language.


## Building and running

This is in the process of being updated. Stay tuned.


## Installing the DbSpec Emacs mode

The DbSpec Emacs mode offers syntax highlighting and validation when editing .dbspec files.
It requires Emacs version 29 or higher, and it can be installed as follows:

```shell
mkdir ~/.emacs.d/tree-sitter
cp libtree-sitter-dbspec.so ~/.emacs.d/tree-sitter
cp dbspec-ts-mode.el ~/.emacs.d
```


## Dependencies and choice of license

Since the DbSpec interpreter uses components from SIARD Suite internally,
we have decided to use the same license for now, which is CDDL 1.0.
In the future we might replace these components and switch to a more permissive license.
The parser submodule is made available under the MIT license.
The DbSpec source code also contains fragments of
[java-tree-sitter](https://github.com/serenadeai/java-tree-sitter),
which uses the MIT license.
