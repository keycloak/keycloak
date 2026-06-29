import { declare } from "@babel/helper-plugin-utils";

const REACT_HOOK_FORM_HOOKS = new Set([
  "useForm",
  "useFormContext",
  "useController",
]);

const addNoMemoDirective = (functionPath, t) => {
  if (!t.isBlockStatement(functionPath.node.body)) {
    functionPath
      .get("body")
      .replaceWith(
        t.blockStatement([t.returnStatement(functionPath.node.body)]),
      );
  }

  if (!t.isBlockStatement(functionPath.node.body)) {
    return;
  }

  const directives = functionPath.node.body.directives ?? [];
  const hasManualOptOut = directives.some(
    (directive) => directive.value.value === "use no memo",
  );

  if (hasManualOptOut) {
    return;
  }

  functionPath.node.body.directives = [
    ...directives,
    t.directive(t.directiveLiteral("use no memo")),
  ];
};

/**
 * Opts functions that use react-hook-form hooks out of React Compiler
 * memoization by inserting the "use no memo" directive.
 *
 * @see https://github.com/react-hook-form/react-hook-form/issues/12298
 */
export default declare(({ types: t }) => ({
  visitor: {
    Program(programPath) {
      if (programPath.hub.file.opts.filename?.includes("node_modules")) {
        return;
      }

      const { bindings } = programPath.scope;

      for (const key in bindings) {
        const binding = bindings[key];
        if (
          !t.isImportSpecifier(binding.path.node) ||
          !t.isIdentifier(binding.path.node.imported) ||
          !t.isImportDeclaration(binding.path.parentPath?.node) ||
          binding.path.parentPath?.node.source.value !== "react-hook-form" ||
          !REACT_HOOK_FORM_HOOKS.has(binding.path.node.imported.name)
        ) {
          continue;
        }

        binding.referencePaths.forEach((refPath) => {
          const functionPath = refPath.getFunctionParent();
          if (!functionPath) {
            return;
          }

          addNoMemoDirective(functionPath, t);
        });
      }
    },
  },
}));
