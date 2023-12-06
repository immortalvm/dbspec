;;; dbspec-ts-mode.el --- tree-sitter support for dbspec  -*- lexical-binding: t; -*-

(require 'treesit)
(require 'rx)

(declare-function treesit-parser-create "treesit.c")
(declare-function treesit-induce-sparse-tree "treesit.c")
(declare-function treesit-node-start "treesit.c")
(declare-function treesit-node-type "treesit.c")
(declare-function treesit-node-child-by-field-name "treesit.c")


(defcustom dbspec-ts-mode-indent-offset 2
  "Number of spaces for each indentation step in `dbspec-ts-mode'."
  :version "29.1"
  :type 'integer
  :safe 'integerp
  :group 'dbspec)

(defvar dbspec-ts-mode--font-lock-settings
  (treesit-font-lock-rules
   :language 'dbspec
   :feature 'error
   :override t
   '((ERROR) @font-lock-warning-face))
  "Font-lock settings for DbSpec.")

;;;###autoload
(define-derived-mode dbspec-ts-mode prog-mode "DbSpec"
  "Major mode for editing DbSpec, powered by tree-sitter."
  :group 'dbspec

  (unless (treesit-ready-p 'dbspec)
    (error "Tree-sitter for DbSpec isn't available"))

  (treesit-parser-create 'dbspec)

  ;; Comments.
  (setq-local comment-start "#")
  (setq-local comment-end "")

  ;; Font-lock.
  (setq-local treesit-font-lock-settings dbspec-ts-mode--font-lock-settings)
  (setq-local treesit-font-lock-feature-list '((error)))

  (setq indent-tabs-mode t)
  (setq tab-width 4)
  (setq comment-start "#")
  (setq comment-style "plain")
  (setq whitespace-style '(face tabs tab-mark))

  (treesit-major-mode-setup)
  
  (whitespace-mode 1))

(if (treesit-ready-p 'dbspec)
    (add-to-list 'auto-mode-alist
                 '("\\.dbspec\\'" . dbspec-ts-mode)))

(provide 'dbspec-ts-mode)

;;; dbspec-ts-mode.el ends here
