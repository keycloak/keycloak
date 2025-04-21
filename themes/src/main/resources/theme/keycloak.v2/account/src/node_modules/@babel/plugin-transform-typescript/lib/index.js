"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _helperPluginUtils = require("@babel/helper-plugin-utils");

var _pluginSyntaxTypescript = require("@babel/plugin-syntax-typescript");

var _core = require("@babel/core");

var _helperCreateClassFeaturesPlugin = require("@babel/helper-create-class-features-plugin");

var _constEnum = require("./const-enum");

var _enum = require("./enum");

var _namespace = require("./namespace");

function isInType(path) {
  switch (path.parent.type) {
    case "TSTypeReference":
    case "TSQualifiedName":
    case "TSExpressionWithTypeArguments":
    case "TSTypeQuery":
      return true;

    case "ExportSpecifier":
      return path.parentPath.parent.exportKind === "type";

    default:
      return false;
  }
}

const GLOBAL_TYPES = new WeakMap();
const NEEDS_EXPLICIT_ESM = new WeakMap();
const PARSED_PARAMS = new WeakSet();

function isGlobalType(path, name) {
  const program = path.find(path => path.isProgram()).node;
  if (path.scope.hasOwnBinding(name)) return false;
  if (GLOBAL_TYPES.get(program).has(name)) return true;
  console.warn(`The exported identifier "${name}" is not declared in Babel's scope tracker\n` + `as a JavaScript value binding, and "@babel/plugin-transform-typescript"\n` + `never encountered it as a TypeScript type declaration.\n` + `It will be treated as a JavaScript value.\n\n` + `This problem is likely caused by another plugin injecting\n` + `"${name}" without registering it in the scope tracker. If you are the author\n` + ` of that plugin, please use "scope.registerDeclaration(declarationPath)".`);
  return false;
}

function registerGlobalType(programNode, name) {
  GLOBAL_TYPES.get(programNode).add(name);
}

var _default = (0, _helperPluginUtils.declare)((api, opts) => {
  api.assertVersion(7);
  const JSX_PRAGMA_REGEX = /\*?\s*@jsx((?:Frag)?)\s+([^\s]+)/;
  const {
    allowNamespaces = true,
    jsxPragma = "React.createElement",
    jsxPragmaFrag = "React.Fragment",
    onlyRemoveTypeImports = false,
    optimizeConstEnums = false
  } = opts;
  {
    var {
      allowDeclareFields = false
    } = opts;
  }
  const classMemberVisitors = {
    field(path) {
      const {
        node
      } = path;
      {
        if (!allowDeclareFields && node.declare) {
          throw path.buildCodeFrameError(`The 'declare' modifier is only allowed when the 'allowDeclareFields' option of ` + `@babel/plugin-transform-typescript or @babel/preset-typescript is enabled.`);
        }
      }

      if (node.declare) {
        if (node.value) {
          throw path.buildCodeFrameError(`Fields with the 'declare' modifier cannot be initialized here, but only in the constructor`);
        }

        if (!node.decorators) {
          path.remove();
        }
      } else if (node.definite) {
        if (node.value) {
          throw path.buildCodeFrameError(`Definitely assigned fields cannot be initialized here, but only in the constructor`);
        }

        {
          if (!allowDeclareFields && !node.decorators) {
            path.remove();
          }
        }
      } else {
        if (!allowDeclareFields && !node.value && !node.decorators && !_core.types.isClassPrivateProperty(node)) {
          path.remove();
        }
      }

      if (node.accessibility) node.accessibility = null;
      if (node.abstract) node.abstract = null;
      if (node.readonly) node.readonly = null;
      if (node.optional) node.optional = null;
      if (node.typeAnnotation) node.typeAnnotation = null;
      if (node.definite) node.definite = null;
      if (node.declare) node.declare = null;
      if (node.override) node.override = null;
    },

    method({
      node
    }) {
      if (node.accessibility) node.accessibility = null;
      if (node.abstract) node.abstract = null;
      if (node.optional) node.optional = null;
      if (node.override) node.override = null;
    },

    constructor(path, classPath) {
      if (path.node.accessibility) path.node.accessibility = null;
      const assigns = [];
      const {
        scope
      } = path;

      for (const paramPath of path.get("params")) {
        const param = paramPath.node;

        if (param.type === "TSParameterProperty") {
          const parameter = param.parameter;
          if (PARSED_PARAMS.has(parameter)) continue;
          PARSED_PARAMS.add(parameter);
          let id;

          if (_core.types.isIdentifier(parameter)) {
            id = parameter;
          } else if (_core.types.isAssignmentPattern(parameter) && _core.types.isIdentifier(parameter.left)) {
            id = parameter.left;
          } else {
            throw paramPath.buildCodeFrameError("Parameter properties can not be destructuring patterns.");
          }

          assigns.push(_core.template.statement.ast`
          this.${_core.types.cloneNode(id)} = ${_core.types.cloneNode(id)}`);
          paramPath.replaceWith(paramPath.get("parameter"));
          scope.registerBinding("param", paramPath);
        }
      }

      (0, _helperCreateClassFeaturesPlugin.injectInitialization)(classPath, path, assigns);
    }

  };
  return {
    name: "transform-typescript",
    inherits: _pluginSyntaxTypescript.default,
    visitor: {
      Pattern: visitPattern,
      Identifier: visitPattern,
      RestElement: visitPattern,
      Program: {
        enter(path, state) {
          const {
            file
          } = state;
          let fileJsxPragma = null;
          let fileJsxPragmaFrag = null;
          const programNode = path.node;

          if (!GLOBAL_TYPES.has(programNode)) {
            GLOBAL_TYPES.set(programNode, new Set());
          }

          if (file.ast.comments) {
            for (const comment of file.ast.comments) {
              const jsxMatches = JSX_PRAGMA_REGEX.exec(comment.value);

              if (jsxMatches) {
                if (jsxMatches[1]) {
                  fileJsxPragmaFrag = jsxMatches[2];
                } else {
                  fileJsxPragma = jsxMatches[2];
                }
              }
            }
          }

          let pragmaImportName = fileJsxPragma || jsxPragma;

          if (pragmaImportName) {
            [pragmaImportName] = pragmaImportName.split(".");
          }

          let pragmaFragImportName = fileJsxPragmaFrag || jsxPragmaFrag;

          if (pragmaFragImportName) {
            [pragmaFragImportName] = pragmaFragImportName.split(".");
          }

          for (let stmt of path.get("body")) {
            if (stmt.isImportDeclaration()) {
              if (!NEEDS_EXPLICIT_ESM.has(state.file.ast.program)) {
                NEEDS_EXPLICIT_ESM.set(state.file.ast.program, true);
              }

              if (stmt.node.importKind === "type") {
                for (const specifier of stmt.node.specifiers) {
                  registerGlobalType(programNode, specifier.local.name);
                }

                stmt.remove();
                continue;
              }

              const importsToRemove = new Set();
              const specifiersLength = stmt.node.specifiers.length;

              const isAllSpecifiersElided = () => specifiersLength > 0 && specifiersLength === importsToRemove.size;

              for (const specifier of stmt.node.specifiers) {
                if (specifier.type === "ImportSpecifier" && specifier.importKind === "type") {
                  registerGlobalType(programNode, specifier.local.name);
                  const binding = stmt.scope.getBinding(specifier.local.name);

                  if (binding) {
                    importsToRemove.add(binding.path);
                  }
                }
              }

              if (onlyRemoveTypeImports) {
                NEEDS_EXPLICIT_ESM.set(path.node, false);
              } else {
                if (stmt.node.specifiers.length === 0) {
                  NEEDS_EXPLICIT_ESM.set(path.node, false);
                  continue;
                }

                for (const specifier of stmt.node.specifiers) {
                  const binding = stmt.scope.getBinding(specifier.local.name);

                  if (binding && !importsToRemove.has(binding.path)) {
                    if (isImportTypeOnly({
                      binding,
                      programPath: path,
                      pragmaImportName,
                      pragmaFragImportName
                    })) {
                      importsToRemove.add(binding.path);
                    } else {
                      NEEDS_EXPLICIT_ESM.set(path.node, false);
                    }
                  }
                }
              }

              if (isAllSpecifiersElided()) {
                stmt.remove();
              } else {
                for (const importPath of importsToRemove) {
                  importPath.remove();
                }
              }

              continue;
            }

            if (stmt.isExportDeclaration()) {
              stmt = stmt.get("declaration");
            }

            if (stmt.isVariableDeclaration({
              declare: true
            })) {
              for (const name of Object.keys(stmt.getBindingIdentifiers())) {
                registerGlobalType(programNode, name);
              }
            } else if (stmt.isTSTypeAliasDeclaration() || stmt.isTSDeclareFunction() && stmt.get("id").isIdentifier() || stmt.isTSInterfaceDeclaration() || stmt.isClassDeclaration({
              declare: true
            }) || stmt.isTSEnumDeclaration({
              declare: true
            }) || stmt.isTSModuleDeclaration({
              declare: true
            }) && stmt.get("id").isIdentifier()) {
              registerGlobalType(programNode, stmt.node.id.name);
            }
          }
        },

        exit(path) {
          if (path.node.sourceType === "module" && NEEDS_EXPLICIT_ESM.get(path.node)) {
            path.pushContainer("body", _core.types.exportNamedDeclaration());
          }
        }

      },

      ExportNamedDeclaration(path, state) {
        if (!NEEDS_EXPLICIT_ESM.has(state.file.ast.program)) {
          NEEDS_EXPLICIT_ESM.set(state.file.ast.program, true);
        }

        if (path.node.exportKind === "type") {
          path.remove();
          return;
        }

        if (path.node.source && path.node.specifiers.length > 0 && path.node.specifiers.every(specifier => specifier.type === "ExportSpecifier" && specifier.exportKind === "type")) {
          path.remove();
          return;
        }

        if (!path.node.source && path.node.specifiers.length > 0 && path.node.specifiers.every(specifier => _core.types.isExportSpecifier(specifier) && isGlobalType(path, specifier.local.name))) {
          path.remove();
          return;
        }

        NEEDS_EXPLICIT_ESM.set(state.file.ast.program, false);
      },

      ExportSpecifier(path) {
        const parent = path.parent;

        if (!parent.source && isGlobalType(path, path.node.local.name) || path.node.exportKind === "type") {
          path.remove();
        }
      },

      ExportDefaultDeclaration(path, state) {
        if (!NEEDS_EXPLICIT_ESM.has(state.file.ast.program)) {
          NEEDS_EXPLICIT_ESM.set(state.file.ast.program, true);
        }

        if (_core.types.isIdentifier(path.node.declaration) && isGlobalType(path, path.node.declaration.name)) {
          path.remove();
          return;
        }

        NEEDS_EXPLICIT_ESM.set(state.file.ast.program, false);
      },

      TSDeclareFunction(path) {
        path.remove();
      },

      TSDeclareMethod(path) {
        path.remove();
      },

      VariableDeclaration(path) {
        if (path.node.declare) {
          path.remove();
        }
      },

      VariableDeclarator({
        node
      }) {
        if (node.definite) node.definite = null;
      },

      TSIndexSignature(path) {
        path.remove();
      },

      ClassDeclaration(path) {
        const {
          node
        } = path;

        if (node.declare) {
          path.remove();
          return;
        }
      },

      Class(path) {
        const {
          node
        } = path;
        if (node.typeParameters) node.typeParameters = null;
        if (node.superTypeParameters) node.superTypeParameters = null;
        if (node.implements) node.implements = null;
        if (node.abstract) node.abstract = null;
        path.get("body.body").forEach(child => {
          if (child.isClassMethod() || child.isClassPrivateMethod()) {
            if (child.node.kind === "constructor") {
              classMemberVisitors.constructor(child, path);
            } else {
              classMemberVisitors.method(child);
            }
          } else if (child.isClassProperty() || child.isClassPrivateProperty()) {
            classMemberVisitors.field(child);
          }
        });
      },

      Function(path) {
        const {
          node
        } = path;
        if (node.typeParameters) node.typeParameters = null;
        if (node.returnType) node.returnType = null;
        const params = node.params;

        if (params.length > 0 && _core.types.isIdentifier(params[0], {
          name: "this"
        })) {
          params.shift();
        }
      },

      TSModuleDeclaration(path) {
        (0, _namespace.default)(path, allowNamespaces);
      },

      TSInterfaceDeclaration(path) {
        path.remove();
      },

      TSTypeAliasDeclaration(path) {
        path.remove();
      },

      TSEnumDeclaration(path) {
        if (optimizeConstEnums && path.node.const) {
          (0, _constEnum.default)(path, _core.types);
        } else {
          (0, _enum.default)(path, _core.types);
        }
      },

      TSImportEqualsDeclaration(path) {
        if (_core.types.isTSExternalModuleReference(path.node.moduleReference)) {
          throw path.buildCodeFrameError(`\`import ${path.node.id.name} = require('${path.node.moduleReference.expression.value}')\` ` + "is not supported by @babel/plugin-transform-typescript\n" + "Please consider using " + `\`import ${path.node.id.name} from '${path.node.moduleReference.expression.value}';\` alongside ` + "Typescript's --allowSyntheticDefaultImports option.");
        }

        path.replaceWith(_core.types.variableDeclaration("var", [_core.types.variableDeclarator(path.node.id, entityNameToExpr(path.node.moduleReference))]));
      },

      TSExportAssignment(path) {
        throw path.buildCodeFrameError("`export =` is not supported by @babel/plugin-transform-typescript\n" + "Please consider using `export <value>;`.");
      },

      TSTypeAssertion(path) {
        path.replaceWith(path.node.expression);
      },

      TSAsExpression(path) {
        let {
          node
        } = path;

        do {
          node = node.expression;
        } while (_core.types.isTSAsExpression(node));

        path.replaceWith(node);
      },

      [api.types.tsInstantiationExpression ? "TSNonNullExpression|TSInstantiationExpression" : "TSNonNullExpression"](path) {
        path.replaceWith(path.node.expression);
      },

      CallExpression(path) {
        path.node.typeParameters = null;
      },

      OptionalCallExpression(path) {
        path.node.typeParameters = null;
      },

      NewExpression(path) {
        path.node.typeParameters = null;
      },

      JSXOpeningElement(path) {
        path.node.typeParameters = null;
      },

      TaggedTemplateExpression(path) {
        path.node.typeParameters = null;
      }

    }
  };

  function entityNameToExpr(node) {
    if (_core.types.isTSQualifiedName(node)) {
      return _core.types.memberExpression(entityNameToExpr(node.left), node.right);
    }

    return node;
  }

  function visitPattern({
    node
  }) {
    if (node.typeAnnotation) node.typeAnnotation = null;
    if (_core.types.isIdentifier(node) && node.optional) node.optional = null;
  }

  function isImportTypeOnly({
    binding,
    programPath,
    pragmaImportName,
    pragmaFragImportName
  }) {
    for (const path of binding.referencePaths) {
      if (!isInType(path)) {
        return false;
      }
    }

    if (binding.identifier.name !== pragmaImportName && binding.identifier.name !== pragmaFragImportName) {
      return true;
    }

    let sourceFileHasJsx = false;
    programPath.traverse({
      "JSXElement|JSXFragment"(path) {
        sourceFileHasJsx = true;
        path.stop();
      }

    });
    return !sourceFileHasJsx;
  }
});

exports.default = _default;