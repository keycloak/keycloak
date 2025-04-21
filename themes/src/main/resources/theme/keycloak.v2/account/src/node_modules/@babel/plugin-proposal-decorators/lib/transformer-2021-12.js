"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = _default;

var _core = require("@babel/core");

var _pluginSyntaxDecorators = require("@babel/plugin-syntax-decorators");

var _helperReplaceSupers = require("@babel/helper-replace-supers");

var _helperSplitExportDeclaration = require("@babel/helper-split-export-declaration");

function incrementId(id, idx = id.length - 1) {
  if (idx === -1) {
    id.unshift(65);
    return;
  }

  const current = id[idx];

  if (current === 90) {
    id[idx] = 97;
  } else if (current === 122) {
    id[idx] = 65;
    incrementId(id, idx - 1);
  } else {
    id[idx] = current + 1;
  }
}

function createPrivateUidGeneratorForClass(classPath) {
  const currentPrivateId = [];
  const privateNames = new Set();
  classPath.traverse({
    PrivateName(path) {
      privateNames.add(path.node.id.name);
    }

  });
  return () => {
    let reifiedId;

    do {
      incrementId(currentPrivateId);
      reifiedId = String.fromCharCode(...currentPrivateId);
    } while (privateNames.has(reifiedId));

    return _core.types.privateName(_core.types.identifier(reifiedId));
  };
}

function createLazyPrivateUidGeneratorForClass(classPath) {
  let generator;
  return () => {
    if (!generator) {
      generator = createPrivateUidGeneratorForClass(classPath);
    }

    return generator();
  };
}

function replaceClassWithVar(path) {
  if (path.type === "ClassDeclaration") {
    const varId = path.scope.generateUidIdentifierBasedOnNode(path.node.id);

    const classId = _core.types.identifier(path.node.id.name);

    path.scope.rename(classId.name, varId.name);
    path.insertBefore(_core.types.variableDeclaration("let", [_core.types.variableDeclarator(varId)]));
    path.get("id").replaceWith(classId);
    return [_core.types.cloneNode(varId), path];
  } else {
    let className;
    let varId;

    if (path.node.id) {
      className = path.node.id.name;
      varId = path.scope.parent.generateDeclaredUidIdentifier(className);
      path.scope.rename(className, varId.name);
    } else if (path.parentPath.node.type === "VariableDeclarator" && path.parentPath.node.id.type === "Identifier") {
      className = path.parentPath.node.id.name;
      varId = path.scope.parent.generateDeclaredUidIdentifier(className);
    } else {
      varId = path.scope.parent.generateDeclaredUidIdentifier("decorated_class");
    }

    const newClassExpr = _core.types.classExpression(className && _core.types.identifier(className), path.node.superClass, path.node.body);

    const [newPath] = path.replaceWith(_core.types.sequenceExpression([newClassExpr, varId]));
    return [_core.types.cloneNode(varId), newPath.get("expressions.0")];
  }
}

function generateClassProperty(key, value, isStatic) {
  if (key.type === "PrivateName") {
    return _core.types.classPrivateProperty(key, value, undefined, isStatic);
  } else {
    return _core.types.classProperty(key, value, undefined, undefined, isStatic);
  }
}

function addProxyAccessorsFor(element, originalKey, targetKey, isComputed = false) {
  const {
    static: isStatic
  } = element.node;

  const getterBody = _core.types.blockStatement([_core.types.returnStatement(_core.types.memberExpression(_core.types.thisExpression(), _core.types.cloneNode(targetKey)))]);

  const setterBody = _core.types.blockStatement([_core.types.expressionStatement(_core.types.assignmentExpression("=", _core.types.memberExpression(_core.types.thisExpression(), _core.types.cloneNode(targetKey)), _core.types.identifier("v")))]);

  let getter, setter;

  if (originalKey.type === "PrivateName") {
    getter = _core.types.classPrivateMethod("get", _core.types.cloneNode(originalKey), [], getterBody, isStatic);
    setter = _core.types.classPrivateMethod("set", _core.types.cloneNode(originalKey), [_core.types.identifier("v")], setterBody, isStatic);
  } else {
    getter = _core.types.classMethod("get", _core.types.cloneNode(originalKey), [], getterBody, isComputed, isStatic);
    setter = _core.types.classMethod("set", _core.types.cloneNode(originalKey), [_core.types.identifier("v")], setterBody, isComputed, isStatic);
  }

  element.insertAfter(setter);
  element.insertAfter(getter);
}

function extractProxyAccessorsFor(targetKey) {
  return [_core.types.functionExpression(undefined, [], _core.types.blockStatement([_core.types.returnStatement(_core.types.memberExpression(_core.types.thisExpression(), _core.types.cloneNode(targetKey)))])), _core.types.functionExpression(undefined, [_core.types.identifier("value")], _core.types.blockStatement([_core.types.expressionStatement(_core.types.assignmentExpression("=", _core.types.memberExpression(_core.types.thisExpression(), _core.types.cloneNode(targetKey)), _core.types.identifier("value")))]))];
}

const FIELD = 0;
const ACCESSOR = 1;
const METHOD = 2;
const GETTER = 3;
const SETTER = 4;
const STATIC = 5;

function getElementKind(element) {
  switch (element.node.type) {
    case "ClassProperty":
    case "ClassPrivateProperty":
      return FIELD;

    case "ClassAccessorProperty":
      return ACCESSOR;

    case "ClassMethod":
    case "ClassPrivateMethod":
      if (element.node.kind === "get") {
        return GETTER;
      } else if (element.node.kind === "set") {
        return SETTER;
      } else {
        return METHOD;
      }

  }
}

function isDecoratorInfo(info) {
  return "decorators" in info;
}

function filteredOrderedDecoratorInfo(info) {
  const filtered = info.filter(isDecoratorInfo);
  return [...filtered.filter(el => el.isStatic && el.kind >= ACCESSOR && el.kind <= SETTER), ...filtered.filter(el => !el.isStatic && el.kind >= ACCESSOR && el.kind <= SETTER), ...filtered.filter(el => el.isStatic && el.kind === FIELD), ...filtered.filter(el => !el.isStatic && el.kind === FIELD)];
}

function generateDecorationExprs(info) {
  return _core.types.arrayExpression(filteredOrderedDecoratorInfo(info).map(el => {
    const decs = el.decorators.length > 1 ? _core.types.arrayExpression(el.decorators) : el.decorators[0];
    const kind = el.isStatic ? el.kind + STATIC : el.kind;
    const decInfo = [decs, _core.types.numericLiteral(kind), el.name];
    const {
      privateMethods
    } = el;

    if (Array.isArray(privateMethods)) {
      decInfo.push(...privateMethods);
    } else if (privateMethods) {
      decInfo.push(privateMethods);
    }

    return _core.types.arrayExpression(decInfo);
  }));
}

function extractElementLocalAssignments(decorationInfo) {
  const localIds = [];

  for (const el of filteredOrderedDecoratorInfo(decorationInfo)) {
    const {
      locals
    } = el;

    if (Array.isArray(locals)) {
      localIds.push(...locals);
    } else if (locals !== undefined) {
      localIds.push(locals);
    }
  }

  return localIds;
}

function addCallAccessorsFor(element, key, getId, setId) {
  element.insertAfter(_core.types.classPrivateMethod("get", _core.types.cloneNode(key), [], _core.types.blockStatement([_core.types.returnStatement(_core.types.callExpression(_core.types.cloneNode(getId), [_core.types.thisExpression()]))])));
  element.insertAfter(_core.types.classPrivateMethod("set", _core.types.cloneNode(key), [_core.types.identifier("v")], _core.types.blockStatement([_core.types.expressionStatement(_core.types.callExpression(_core.types.cloneNode(setId), [_core.types.thisExpression(), _core.types.identifier("v")]))])));
}

function isNotTsParameter(node) {
  return node.type !== "TSParameterProperty";
}

function movePrivateAccessor(element, key, methodLocalVar, isStatic) {
  let params;
  let block;

  if (element.node.kind === "set") {
    params = [_core.types.identifier("v")];
    block = [_core.types.expressionStatement(_core.types.callExpression(methodLocalVar, [_core.types.thisExpression(), _core.types.identifier("v")]))];
  } else {
    params = [];
    block = [_core.types.returnStatement(_core.types.callExpression(methodLocalVar, [_core.types.thisExpression()]))];
  }

  element.replaceWith(_core.types.classPrivateMethod(element.node.kind, _core.types.cloneNode(key), params, _core.types.blockStatement(block), isStatic));
}

function isClassDecoratableElementPath(path) {
  const {
    type
  } = path;
  return type !== "TSDeclareMethod" && type !== "TSIndexSignature" && type !== "StaticBlock";
}

function staticBlockToIIFE(block) {
  return _core.types.callExpression(_core.types.arrowFunctionExpression([], _core.types.blockStatement(block.body)), []);
}

function maybeSequenceExpression(exprs) {
  if (exprs.length === 0) return _core.types.unaryExpression("void", _core.types.numericLiteral(0));
  if (exprs.length === 1) return exprs[0];
  return _core.types.sequenceExpression(exprs);
}

function transformClass(path, state, constantSuper) {
  const body = path.get("body.body");
  const classDecorators = path.node.decorators;
  let hasElementDecorators = false;
  const generateClassPrivateUid = createLazyPrivateUidGeneratorForClass(path);

  for (const element of body) {
    if (!isClassDecoratableElementPath(element)) {
      continue;
    }

    if (element.node.decorators && element.node.decorators.length > 0) {
      hasElementDecorators = true;
    } else if (element.node.type === "ClassAccessorProperty") {
      const {
        key,
        value,
        static: isStatic,
        computed
      } = element.node;
      const newId = generateClassPrivateUid();
      const valueNode = value ? _core.types.cloneNode(value) : undefined;
      const newField = generateClassProperty(newId, valueNode, isStatic);
      const [newPath] = element.replaceWith(newField);
      addProxyAccessorsFor(newPath, key, newId, computed);
    }
  }

  if (!classDecorators && !hasElementDecorators) return;
  const elementDecoratorInfo = [];
  let firstFieldPath;
  let constructorPath;
  let requiresProtoInit = false;
  let requiresStaticInit = false;
  const decoratedPrivateMethods = new Set();
  let protoInitLocal, staticInitLocal, classInitLocal, classLocal;
  const assignments = [];
  const scopeParent = path.scope.parent;

  const memoiseExpression = (expression, hint) => {
    const localEvaluatedId = scopeParent.generateDeclaredUidIdentifier(hint);
    assignments.push(_core.types.assignmentExpression("=", localEvaluatedId, expression));
    return _core.types.cloneNode(localEvaluatedId);
  };

  if (classDecorators) {
    classInitLocal = scopeParent.generateDeclaredUidIdentifier("initClass");
    const [localId, classPath] = replaceClassWithVar(path);
    path = classPath;
    classLocal = localId;
    path.node.decorators = null;

    for (const classDecorator of classDecorators) {
      if (!scopeParent.isStatic(classDecorator.expression)) {
        classDecorator.expression = memoiseExpression(classDecorator.expression, "dec");
      }
    }
  } else {
    if (!path.node.id) {
      path.node.id = path.scope.generateUidIdentifier("Class");
    }

    classLocal = _core.types.cloneNode(path.node.id);
  }

  if (hasElementDecorators) {
    for (const element of body) {
      if (!isClassDecoratableElementPath(element)) {
        continue;
      }

      const {
        node
      } = element;
      const decorators = element.get("decorators");
      const hasDecorators = Array.isArray(decorators) && decorators.length > 0;

      if (hasDecorators) {
        for (const decoratorPath of decorators) {
          if (!scopeParent.isStatic(decoratorPath.node.expression)) {
            decoratorPath.node.expression = memoiseExpression(decoratorPath.node.expression, "dec");
          }
        }
      }

      const isComputed = "computed" in element.node && element.node.computed === true;

      if (isComputed) {
        if (!scopeParent.isStatic(node.key)) {
          node.key = memoiseExpression(node.key, "computedKey");
        }
      }

      const kind = getElementKind(element);
      const {
        key
      } = node;
      const isPrivate = key.type === "PrivateName";
      const isStatic = !!element.node.static;
      let name = "computedKey";

      if (isPrivate) {
        name = key.id.name;
      } else if (!isComputed && key.type === "Identifier") {
        name = key.name;
      }

      if (element.isClassMethod({
        kind: "constructor"
      })) {
        constructorPath = element;
      }

      if (hasDecorators) {
        let locals;
        let privateMethods;

        if (kind === ACCESSOR) {
          const {
            value
          } = element.node;
          const params = [_core.types.thisExpression()];

          if (value) {
            params.push(_core.types.cloneNode(value));
          }

          const newId = generateClassPrivateUid();
          const newFieldInitId = element.scope.parent.generateDeclaredUidIdentifier(`init_${name}`);

          const newValue = _core.types.callExpression(_core.types.cloneNode(newFieldInitId), params);

          const newField = generateClassProperty(newId, newValue, isStatic);
          const [newPath] = element.replaceWith(newField);

          if (isPrivate) {
            privateMethods = extractProxyAccessorsFor(newId);
            const getId = newPath.scope.parent.generateDeclaredUidIdentifier(`get_${name}`);
            const setId = newPath.scope.parent.generateDeclaredUidIdentifier(`set_${name}`);
            addCallAccessorsFor(newPath, key, getId, setId);
            locals = [newFieldInitId, getId, setId];
          } else {
            addProxyAccessorsFor(newPath, key, newId, isComputed);
            locals = newFieldInitId;
          }
        } else if (kind === FIELD) {
          const initId = element.scope.parent.generateDeclaredUidIdentifier(`init_${name}`);
          const valuePath = element.get("value");
          valuePath.replaceWith(_core.types.callExpression(_core.types.cloneNode(initId), [_core.types.thisExpression(), valuePath.node].filter(v => v)));
          locals = initId;

          if (isPrivate) {
            privateMethods = extractProxyAccessorsFor(key);
          }
        } else if (isPrivate) {
          locals = element.scope.parent.generateDeclaredUidIdentifier(`call_${name}`);
          const replaceSupers = new _helperReplaceSupers.default({
            constantSuper,
            methodPath: element,
            objectRef: classLocal,
            superRef: path.node.superClass,
            file: state.file,
            refToPreserve: classLocal
          });
          replaceSupers.replace();
          const {
            params,
            body,
            async: isAsync
          } = element.node;
          privateMethods = _core.types.functionExpression(undefined, params.filter(isNotTsParameter), body, isAsync);

          if (kind === GETTER || kind === SETTER) {
            movePrivateAccessor(element, _core.types.cloneNode(key), _core.types.cloneNode(locals), isStatic);
          } else {
            const node = element.node;
            path.node.body.body.unshift(_core.types.classPrivateProperty(key, _core.types.cloneNode(locals), [], node.static));
            decoratedPrivateMethods.add(key.id.name);
            element.remove();
          }
        }

        let nameExpr;

        if (isComputed) {
          nameExpr = _core.types.cloneNode(key);
        } else if (key.type === "PrivateName") {
          nameExpr = _core.types.stringLiteral(key.id.name);
        } else if (key.type === "Identifier") {
          nameExpr = _core.types.stringLiteral(key.name);
        } else {
          nameExpr = _core.types.cloneNode(key);
        }

        elementDecoratorInfo.push({
          kind,
          decorators: decorators.map(d => d.node.expression),
          name: nameExpr,
          isStatic,
          privateMethods,
          locals
        });

        if (kind !== FIELD) {
          if (isStatic) {
            requiresStaticInit = true;
          } else {
            requiresProtoInit = true;
          }
        }

        if (element.node) {
          element.node.decorators = null;
        }

        if (!firstFieldPath && (kind === FIELD || kind === ACCESSOR)) {
          firstFieldPath = element;
        }
      }
    }
  }

  const elementDecorations = generateDecorationExprs(elementDecoratorInfo);

  const classDecorations = _core.types.arrayExpression((classDecorators || []).map(d => d.expression));

  const locals = extractElementLocalAssignments(elementDecoratorInfo);

  if (requiresProtoInit) {
    protoInitLocal = scopeParent.generateDeclaredUidIdentifier("initProto");
    locals.push(protoInitLocal);

    const protoInitCall = _core.types.callExpression(_core.types.cloneNode(protoInitLocal), [_core.types.thisExpression()]);

    if (firstFieldPath) {
      const value = firstFieldPath.get("value");
      const body = [protoInitCall];

      if (value.node) {
        body.push(value.node);
      }

      value.replaceWith(_core.types.sequenceExpression(body));
    } else if (constructorPath) {
      if (path.node.superClass) {
        path.traverse({
          CallExpression: {
            exit(path) {
              if (!path.get("callee").isSuper()) return;
              path.replaceWith(_core.types.callExpression(_core.types.cloneNode(protoInitLocal), [path.node]));
              path.skip();
            }

          }
        });
      } else {
        constructorPath.node.body.body.unshift(_core.types.expressionStatement(protoInitCall));
      }
    } else {
      const body = [_core.types.expressionStatement(protoInitCall)];

      if (path.node.superClass) {
        body.unshift(_core.types.expressionStatement(_core.types.callExpression(_core.types.super(), [_core.types.spreadElement(_core.types.identifier("args"))])));
      }

      path.node.body.body.unshift(_core.types.classMethod("constructor", _core.types.identifier("constructor"), [_core.types.restElement(_core.types.identifier("args"))], _core.types.blockStatement(body)));
    }
  }

  if (requiresStaticInit) {
    staticInitLocal = scopeParent.generateDeclaredUidIdentifier("initStatic");
    locals.push(staticInitLocal);
  }

  if (decoratedPrivateMethods.size > 0) {
    path.traverse({
      PrivateName(path) {
        if (!decoratedPrivateMethods.has(path.node.id.name)) return;
        const parentPath = path.parentPath;
        const parentParentPath = parentPath.parentPath;

        if (parentParentPath.node.type === "AssignmentExpression" && parentParentPath.node.left === parentPath.node || parentParentPath.node.type === "UpdateExpression" || parentParentPath.node.type === "RestElement" || parentParentPath.node.type === "ArrayPattern" || parentParentPath.node.type === "ObjectProperty" && parentParentPath.node.value === parentPath.node && parentParentPath.parentPath.type === "ObjectPattern" || parentParentPath.node.type === "ForOfStatement" && parentParentPath.node.left === parentPath.node) {
          throw path.buildCodeFrameError(`Decorated private methods are not updatable, but "#${path.node.id.name}" is updated via this expression.`);
        }
      }

    });
  }

  let classInitInjected = false;

  const classInitCall = classInitLocal && _core.types.callExpression(_core.types.cloneNode(classInitLocal), []);

  const originalClass = path.node;

  if (classDecorators) {
    locals.push(classLocal, classInitLocal);
    const statics = [];
    let staticBlocks = [];
    path.get("body.body").forEach(element => {
      if (element.isStaticBlock()) {
        staticBlocks.push(element.node);
        element.remove();
        return;
      }

      const isProperty = element.isClassProperty() || element.isClassPrivateProperty();

      if ((isProperty || element.isClassPrivateMethod()) && element.node.static) {
        if (isProperty && staticBlocks.length > 0) {
          const allValues = staticBlocks.map(staticBlockToIIFE);
          if (element.node.value) allValues.push(element.node.value);
          element.node.value = maybeSequenceExpression(allValues);
          staticBlocks = [];
        }

        element.node.static = false;
        statics.push(element.node);
        element.remove();
      }
    });

    if (statics.length > 0 || staticBlocks.length > 0) {
      const staticsClass = _core.template.expression.ast`
        class extends ${state.addHelper("identity")} {}
      `;
      staticsClass.body.body = [_core.types.staticBlock([_core.types.toStatement(path.node, false)]), ...statics];
      const constructorBody = [];

      const newExpr = _core.types.newExpression(staticsClass, []);

      if (staticBlocks.length > 0) {
        constructorBody.push(...staticBlocks.map(staticBlockToIIFE));
      }

      if (classInitCall) {
        classInitInjected = true;
        constructorBody.push(classInitCall);
      }

      if (constructorBody.length > 0) {
        constructorBody.unshift(_core.types.callExpression(_core.types.super(), [_core.types.cloneNode(classLocal)]));
        staticsClass.body.body.push(_core.types.classMethod("constructor", _core.types.identifier("constructor"), [], _core.types.blockStatement([_core.types.expressionStatement(_core.types.sequenceExpression(constructorBody))])));
      } else {
        newExpr.arguments.push(_core.types.cloneNode(classLocal));
      }

      path.replaceWith(newExpr);
    }
  }

  if (!classInitInjected && classInitCall) {
    path.node.body.body.push(_core.types.staticBlock([_core.types.expressionStatement(classInitCall)]));
  }

  originalClass.body.body.unshift(_core.types.staticBlock([_core.types.expressionStatement(_core.types.assignmentExpression("=", _core.types.arrayPattern(locals), _core.types.callExpression(state.addHelper("applyDecs"), [_core.types.thisExpression(), elementDecorations, classDecorations]))), requiresStaticInit && _core.types.expressionStatement(_core.types.callExpression(_core.types.cloneNode(staticInitLocal), [_core.types.thisExpression()]))].filter(Boolean)));
  path.insertBefore(assignments.map(expr => _core.types.expressionStatement(expr)));
  path.scope.crawl();
  return path;
}

function _default({
  assertVersion,
  assumption
}, {
  loose
}) {
  var _assumption;

  assertVersion("^7.16.0");
  const VISITED = new WeakSet();
  const constantSuper = (_assumption = assumption("constantSuper")) != null ? _assumption : loose;
  return {
    name: "proposal-decorators",
    inherits: _pluginSyntaxDecorators.default,
    visitor: {
      "ExportNamedDeclaration|ExportDefaultDeclaration"(path) {
        var _declaration$decorato;

        const {
          declaration
        } = path.node;

        if ((declaration == null ? void 0 : declaration.type) === "ClassDeclaration" && ((_declaration$decorato = declaration.decorators) == null ? void 0 : _declaration$decorato.length) > 0) {
          (0, _helperSplitExportDeclaration.default)(path);
        }
      },

      Class(path, state) {
        if (VISITED.has(path)) return;
        const newPath = transformClass(path, state, constantSuper);
        if (newPath) VISITED.add(newPath);
      }

    }
  };
}