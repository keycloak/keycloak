"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = wrapFunction;

var _helperFunctionName = require("@babel/helper-function-name");

var _template = require("@babel/template");

var _t = require("@babel/types");

const {
  blockStatement,
  callExpression,
  functionExpression,
  isAssignmentPattern,
  isRestElement,
  returnStatement
} = _t;

const buildAnonymousExpressionWrapper = _template.default.expression(`
  (function () {
    var REF = FUNCTION;
    return function NAME(PARAMS) {
      return REF.apply(this, arguments);
    };
  })()
`);

const buildNamedExpressionWrapper = _template.default.expression(`
  (function () {
    var REF = FUNCTION;
    function NAME(PARAMS) {
      return REF.apply(this, arguments);
    }
    return NAME;
  })()
`);

const buildDeclarationWrapper = _template.default.statements(`
  function NAME(PARAMS) { return REF.apply(this, arguments); }
  function REF() {
    REF = FUNCTION;
    return REF.apply(this, arguments);
  }
`);

function classOrObjectMethod(path, callId) {
  const node = path.node;
  const body = node.body;
  const container = functionExpression(null, [], blockStatement(body.body), true);
  body.body = [returnStatement(callExpression(callExpression(callId, [container]), []))];
  node.async = false;
  node.generator = false;
  path.get("body.body.0.argument.callee.arguments.0").unwrapFunctionEnvironment();
}

function plainFunction(path, callId, noNewArrows, ignoreFunctionLength) {
  const node = path.node;
  const isDeclaration = path.isFunctionDeclaration();
  const functionId = node.id;
  const wrapper = isDeclaration ? buildDeclarationWrapper : functionId ? buildNamedExpressionWrapper : buildAnonymousExpressionWrapper;

  if (path.isArrowFunctionExpression()) {
    path.arrowFunctionToExpression({
      noNewArrows
    });
  }

  node.id = null;

  if (isDeclaration) {
    node.type = "FunctionExpression";
  }

  const built = callExpression(callId, [node]);
  const params = [];

  for (const param of node.params) {
    if (isAssignmentPattern(param) || isRestElement(param)) {
      break;
    }

    params.push(path.scope.generateUidIdentifier("x"));
  }

  const container = wrapper({
    NAME: functionId || null,
    REF: path.scope.generateUidIdentifier(functionId ? functionId.name : "ref"),
    FUNCTION: built,
    PARAMS: params
  });

  if (isDeclaration) {
    path.replaceWith(container[0]);
    path.insertAfter(container[1]);
  } else {
    const retFunction = container.callee.body.body[1].argument;

    if (!functionId) {
      (0, _helperFunctionName.default)({
        node: retFunction,
        parent: path.parent,
        scope: path.scope
      });
    }

    if (!retFunction || retFunction.id || !ignoreFunctionLength && params.length) {
      path.replaceWith(container);
    } else {
      path.replaceWith(built);
    }
  }
}

function wrapFunction(path, callId, noNewArrows = true, ignoreFunctionLength = false) {
  if (path.isMethod()) {
    classOrObjectMethod(path, callId);
  } else {
    plainFunction(path, callId, noNewArrows, ignoreFunctionLength);
  }
}