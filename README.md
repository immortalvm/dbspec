# DbSpec interpreter

An interpreter for the DbSpec language.


## Building and running

The interpreter can be built (and run) using Gradle.
This can be encapsulated using Docker build, see `Dockerfile`s in the root and example directories.


## The DbSpec Emacs mode

The DbSpec Emacs mode offers syntax highlighting and basic validation when editing .dbspec files.
It requires Emacs version 29 (or higher) and some Emacs expertise.
First, evaluate the following in `*scracth*`:
```elisp
(unless (assq 'dbspec treesit-language-source-alist)
  (push (dbspec "https://github.com/immortalvm/tree-sitter-dbspec")
        treesit-language-source-alist))

;; Pull, (re)compile and install tree-sitter-dbspec grammar library.
(treesit-install-language-grammar 'dbspec)
```

Next, add the following (or something equivalent) to your .emacs:
```elisp
(when (treesit-ready-p 'dbspec)
  (autoload 'dbspec-ts-mode "<path>"
    "Major mode for editing DbSpec, powered by tree-sitter." t)
  (add-to-list 'auto-mode-alist '("\\.dbspec\\'" . dbspec-ts-mode)))
```
after changing `<path>` to the full path to your copy of `dbspec-ts-mode.el`.


## Dependencies and choice of license

Since the DbSpec interpreter uses components from SIARD Suite internally,
we have decided to use the same license for now, which is CDDL 1.0.
In the future we might replace these components and switch to a more permissive license.
The parser submodule is made available under the MIT license.
The DbSpec source code also contains fragments of
[java-tree-sitter](https://github.com/serenadeai/java-tree-sitter),
which uses the MIT license.
