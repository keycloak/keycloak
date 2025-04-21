"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = transpileEnum;
exports.translateEnumValues = translateEnumValues;

var _core = require("@babel/core");

var _assert = require("assert");

function transpileEnum(path, t) {
  const {
    node
  } = path;

  if (node.declare) {
    path.remove();
    return;
  }

  const name = node.id.name;
  const fill = enumFill(path, t, node.id);

  switch (path.parent.type) {
    case "BlockStatement":
    case "ExportNamedDeclaration":
    case "Program":
      {
        path.insertAfter(fill);

        if (seen(path.parentPath)) {
          path.remove();
        } else {
          const isGlobal = t.isProgram(path.parent);
          path.scope.registerDeclaration(path.replaceWith(makeVar(node.id, t, isGlobal ? "var" : "let"))[0]);
        }

        break;
      }

    default:
      throw new Error(`Unexpected enum parent '${path.parent.type}`);
  }

  function seen(parentPath) {
    if (parentPath.isExportDeclaration()) {
      return seen(parentPath.parentPath);
    }

    if (parentPath.getData(name)) {
      return true;
    } else {
      parentPath.setData(name, true);
      return false;
    }
  }
}

function makeVar(id, t, kind) {
  return t.variableDeclaration(kind, [t.variableDeclarator(id)]);
}

const buildEnumWrapper = (0, _core.template)(`
  (function (ID) {
    ASSIGNMENTS;
  })(ID || (ID = {}));
`);
const buildStringAssignment = (0, _core.template)(`
  ENUM["NAME"] = VALUE;
`);
const buildNumericAssignment = (0, _core.template)(`
  ENUM[ENUM["NAME"] = VALUE] = "NAME";
`);

const buildEnumMember = (isString, options) => (isString ? buildStringAssignment : buildNumericAssignment)(options);

function enumFill(path, t, id) {
  const x = translateEnumValues(path, t);
  const assignments = x.map(([memberName, memberValue]) => buildEnumMember(t.isStringLiteral(memberValue), {
    ENUM: t.cloneNode(id),
    NAME: memberName,
    VALUE: memberValue
  }));
  return buildEnumWrapper({
    ID: t.cloneNode(id),
    ASSIGNMENTS: assignments
  });
}

function ReferencedIdentifier(expr, state) {
  const {
    seen,
    path,
    t
  } = state;
  const name = expr.node.name;

  if (seen.has(name) && !expr.scope.hasOwnBinding(name)) {
    expr.replaceWith(t.memberExpression(t.cloneNode(path.node.id), t.cloneNode(expr.node)));
    expr.skip();
  }
}

const enumSelfReferenceVisitor = {
  ReferencedIdentifier
};

function translateEnumValues(path, t) {
  const seen = new Map();
  let constValue = -1;
  let lastName;
  return path.get("members").map(memberPath => {
    const member = memberPath.node;
    const name = t.isIdentifier(member.id) ? member.id.name : member.id.value;
    const initializer = member.initializer;
    let value;

    if (initializer) {
      constValue = evaluate(initializer, seen);

      if (constValue !== undefined) {
        seen.set(name, constValue);

        if (typeof constValue === "number") {
          value = t.numericLiteral(constValue);
        } else {
          _assert(typeof constValue === "string");

          value = t.stringLiteral(constValue);
        }
      } else {
        const initializerPath = memberPath.get("initializer");

        if (initializerPath.isReferencedIdentifier()) {
          ReferencedIdentifier(initializerPath, {
            t,
            seen,
            path
          });
        } else {
          initializerPath.traverse(enumSelfReferenceVisitor, {
            t,
            seen,
            path
          });
        }

        value = initializerPath.node;
        seen.set(name, undefined);
      }
    } else if (typeof constValue === "number") {
      constValue += 1;
      value = t.numericLiteral(constValue);
      seen.set(name, constValue);
    } else if (typeof constValue === "string") {
      throw path.buildCodeFrameError("Enum member must have initializer.");
    } else {
      const lastRef = t.memberExpression(t.cloneNode(path.node.id), t.stringLiteral(lastName), true);
      value = t.binaryExpression("+", t.numericLiteral(1), lastRef);
      seen.set(name, undefined);
    }

    lastName = name;
    return [name, value];
  });
}

function evaluate(expr, seen) {
  return evalConstant(expr);

  function evalConstant(expr) {
    switch (expr.type) {
      case "StringLiteral":
        return expr.value;

      case "UnaryExpression":
        return evalUnaryExpression(expr);

      case "BinaryExpression":
        return evalBinaryExpression(expr);

      case "NumericLiteral":
        return expr.value;

      case "ParenthesizedExpression":
        return evalConstant(expr.expression);

      case "Identifier":
        return seen.get(expr.name);

      case "TemplateLiteral":
        if (expr.quasis.length === 1) {
          return expr.quasis[0].value.cooked;
        }

      default:
        return undefined;
    }
  }

  function evalUnaryExpression({
    argument,
    operator
  }) {
    const value = evalConstant(argument);

    if (value === undefined) {
      return undefined;
    }

    switch (operator) {
      case "+":
        return value;

      case "-":
        return -value;

      case "~":
        return ~value;

      default:
        return undefined;
    }
  }

  function evalBinaryExpression(expr) {
    const left = evalConstant(expr.left);

    if (left === undefined) {
      return undefined;
    }

    const right = evalConstant(expr.right);

    if (right === undefined) {
      return undefined;
    }

    switch (expr.operator) {
      case "|":
        return left | right;

      case "&":
        return left & right;

      case ">>":
        return left >> right;

      case ">>>":
        return left >>> right;

      case "<<":
        return left << right;

      case "^":
        return left ^ right;

      case "*":
        return left * right;

      case "/":
        return left / right;

      case "+":
        return left + right;

      case "-":
        return left - right;

      case "%":
        return left % right;

      default:
        return undefined;
    }
  }
}