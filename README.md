# DbSpec interpreter

An interpreter for the DbSpec language, cf.
the [language reference](doc/DbSpec%20language%20reference.docx) (.docx)
and the corresponding [Tree-sitter grammar](https://github.com/immortalvm/tree-sitter-dbspec).

## Building and running

The interpreter can be built (and run) using Gradle.
This can be encapsulated using Docker build, see `Dockerfile`s in the root and example directories.


## Emacs mode

The directory `emacs` contains a major mode for the text editor Emacs
which offers syntax highlighting and basic validation when editing .dbspec files.
It requires Emacs version 29.1 (or higher) and some Emacs expertise.
First, evaluate the following, e.g. in `*scratch*`:
```elisp
(require 'treesit)

(unless (assq 'dbspec treesit-language-source-alist)
  (push '(dbspec "https://github.com/immortalvm/tree-sitter-dbspec")
        treesit-language-source-alist))

;; Pull, (re)compile and install tree-sitter-dbspec grammar library.
(treesit-install-language-grammar 'dbspec)
```

Next, add the following (or something equivalent) to your .emacs:
```elisp
(use-package dbspec-ts-mode
  :if (and (require 'treesit nil t)
           (treesit-ready-p 'dbspec))
  :load-path "<dir>"
  :commands dbspec-ts-mode
  :mode "\\.dbspec\\'")
```
after changing `<dir>` to the directory containing `dbspec-ts-mode.el`.


## Dependencies and choice of license

Since the DbSpec interpreter uses components from SIARD Suite internally,
we have decided to use the same license for now, which is CDDL 1.0.
In the future we might replace these components and switch to a more permissive license.
The parser submodule is made available under the MIT license.
The DbSpec source code also contains fragments of
[java-tree-sitter](https://github.com/serenadeai/java-tree-sitter),
which uses the MIT license.

## Release Notes

See the `release-notes` directory.
