;;; dbspec-ts-mode.el --- tree-sitter support for dbspec  -*- lexical-binding: t; -*-

(require 'treesit)
(require 'rx)

(declare-function treesit-parser-create "treesit.c")
(declare-function treesit-induce-sparse-tree "treesit.c")
(declare-function treesit-node-start "treesit.c")
(declare-function treesit-node-type "treesit.c")
(declare-function treesit-node-child-by-field-name "treesit.c")


(defcustom dbspec-ts-mode-indent-offset 4
  "Number of spaces for each indentation step in `dbspec-ts-mode'."
  :version "29.1"
  :type 'integer
  :safe 'integerp
  :group 'dbspec)

(defvar dbspec-ts-mode--font-lock-settings
  ;; Since the current DbSpec Tree-sitter grammar was not developed with
  ;; editor support in mind, this is not great. For instance, many keywords
  ;; are not registered as such, and comments are essentially treated as
  ;; insignificant whitespace.
  (treesit-font-lock-rules

   :language 'dbspec
   :feature 'string
   '((string "\"" @font-lock-string-face)
     ((string_content) @font-lock-string-face))

   :language 'dbspec
   :feature 'integer
   '((integer) @font-lock-number-face)

   :language 'dbspec
   :feature 'identifier
   '((identifier) @font-lock-variable-use-face)

   :language 'dbspec
   :feature 'identifier
   '((parameter (identifier) @font-lock-variable-set-face)
     (set (identifier) @font-lock-variable-set-face))

   :language 'dbspec
   :feature 'short_description
   '((short_description) @font-lock-doc-face)

   :language 'dbspec
   :feature 'interpolation
   :override 'keep
   '((interpolation) @font-lock-function-call-face)

   :language 'dbspec
   :feature 'raw
   :override 'keep
   '((raw) @default)

   ;; This is a hack: In order to compensate for limitations in the
   ;; current tree-sitter-dbspec, we assume that everything is a keyword
   ;; by default.
   :language 'dbspec
   :feature 'keyword
   :override 'keep
   '((_) @font-lock-keyword-face)

   :language 'dbspec
   :feature 'error
   :override t
   '((ERROR) @font-lock-warning-face)
   )
  "Font-lock settings for DbSpec.")

;;;###autoload
(define-derived-mode dbspec-ts-mode prog-mode "DbSpec"
  "Major mode for editing DbSpec, powered by tree-sitter."
  :group 'dbspec

  ;; TODO: Is there a better way to ensure this?
  (cond
   ((eq buffer-file-coding-system 'utf-8-unix))
   ((eq buffer-file-coding-system 'undecided-unix)
    (setq buffer-file-coding-system 'utf-8-unix))
   (t
    ;; This will mark the buffer as modified.
    (set-buffer-file-coding-system 'utf-8-unix)))

  (unless (treesit-ready-p 'dbspec)
    (error "Tree-sitter for DbSpec isn't available"))

  (treesit-parser-create 'dbspec)

  ;; Font-lock.
  (setq-local treesit-font-lock-settings dbspec-ts-mode--font-lock-settings)
  (setq-local treesit-font-lock-feature-list
              '((error identifier short_description keyword raw
                       interpolation string integer)))

  (setq-local font-lock-keywords
              '(("^\t*\\(#.*\\)$" 1 'font-lock-comment-face prepend)))

  (setq-local indent-tabs-mode t)
  (setq-local tab-width dbspec-ts-mode-indent-offset)
  (setq-local indent-line-function 'indent-to-left-margin)

  ;; Comments.
  (setq-local comment-start "#")
  (setq-local comment-end "")
  (setq-local comment-padding " ")
  (setq-local comment-style "plain")

  (treesit-major-mode-setup)

  ;; This is another hack in order to compensate for limitations in the
  ;; current tree-sitter grammar. It must come after treesit-major-mode-setup.
  (font-lock-add-keywords nil '(("^\t*\\(#.*\\)$" 1 '(font-lock-doc-markup-face default) prepend)))

  (setq-local whitespace-style '(face tabs tab-mark))
  (whitespace-mode 1))

(if (treesit-ready-p 'dbspec)
    (add-to-list 'auto-mode-alist
                 '("\\.dbspec\\'" . dbspec-ts-mode)))

(provide 'dbspec-ts-mode)

;;; dbspec-ts-mode.el ends here
