"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _convertUnit = _interopRequireDefault(require("./convertUnit"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function isEqual(left, right) {
  return left.type === right.type && left.value === right.value;
}

function isValueType(type) {
  switch (type) {
    case 'LengthValue':
    case 'AngleValue':
    case 'TimeValue':
    case 'FrequencyValue':
    case 'ResolutionValue':
    case 'EmValue':
    case 'ExValue':
    case 'ChValue':
    case 'RemValue':
    case 'VhValue':
    case 'VwValue':
    case 'VminValue':
    case 'VmaxValue':
    case 'PercentageValue':
    case 'Number':
      return true;
  }

  return false;
}

function flip(operator) {
  return operator === '+' ? '-' : '+';
}

function flipValue(node) {
  if (isValueType(node.type)) {
    node.value = -node.value;
  } else if (node.type === 'MathExpression') {
    if (node.operator === '*' || node.operator === '/') {
      node.left = flipValue(node.left);
    } else {
      node.left = flipValue(node.left);
      node.right = flipValue(node.right);
    }
  }

  return node;
}

function reduceAddSubExpression(node, precision) {
  // something + 0 => something
  // something - 0 => something
  if (isValueType(node.right.type) && node.right.value === 0) {
    return node.left;
  } // 0 + something => something


  if (isValueType(node.left.type) && node.left.value === 0 && node.operator === "+") {
    return node.right;
  } // 0 - something => -something


  if (isValueType(node.left.type) && node.left.value === 0 && node.operator === "-" && node.right.type !== "Function") {
    return flipValue(node.right);
  } // value + value
  // value - value


  if (isValueType(node.left.type) && node.left.type === node.right.type) {
    var operator = node.operator;

    var _covertNodesUnits = covertNodesUnits(node.left, node.right, precision),
        left = _covertNodesUnits.left,
        right = _covertNodesUnits.right;

    if (operator === "+") {
      left.value += right.value;
    } else {
      left.value -= right.value;
    }

    return left;
  } // value <op> (expr)


  if (node.right.type === 'MathExpression' && (node.right.operator === '+' || node.right.operator === '-')) {
    // something - (something + something) => something - something - something
    // something - (something - something) => something - something + something
    if ((node.right.operator === '+' || node.right.operator === '-') && node.operator === '-') {
      node.right.operator = flip(node.right.operator);
    }

    if (isValueType(node.left.type)) {
      // value + (value + something) => value + something
      // value + (value - something) => value - something
      // value - (value + something) => value - something
      // value - (value - something) => value + something
      if (node.left.type === node.right.left.type) {
        var _left = node.left,
            _operator = node.operator,
            _right = node.right;
        node.left = reduce({
          type: 'MathExpression',
          operator: _operator,
          left: _left,
          right: _right.left
        });
        node.operator = _right.operator;
        node.right = _right.right;
        return reduce(node, precision);
      } // something + (something + value) => dimension + something
      // something + (something - value) => dimension + something
      // something - (something + value) => dimension - something
      // something - (something - value) => dimension - something


      if (node.left.type === node.right.right.type) {
        var _left2 = node.left,
            _right2 = node.right;
        node.left = reduce({
          type: 'MathExpression',
          operator: _right2.operator,
          left: _left2,
          right: _right2.right
        });
        node.right = _right2.left;
        return reduce(node, precision);
      }
    }
  } // (expr) <op> value


  if (node.left.type === 'MathExpression' && (node.left.operator === '+' || node.left.operator === '-') && isValueType(node.right.type)) {
    // (value + something) + value => value + something
    // (value - something) + value => value - something
    // (value + something) - value => value + something
    // (value - something) - value => value - something
    if (node.right.type === node.left.left.type) {
      var _left3 = node.left,
          _operator2 = node.operator,
          _right3 = node.right;
      _left3.left = reduce({
        type: 'MathExpression',
        operator: _operator2,
        left: _left3.left,
        right: _right3
      }, precision);
      return reduce(_left3, precision);
    } // (something + dimension) + dimension => something + dimension
    // (something - dimension) + dimension => something - dimension
    // (something + dimension) - dimension => something + dimension
    // (something - dimension) - dimension => something - dimension


    if (node.right.type === node.left.right.type) {
      var _left4 = node.left,
          _operator3 = node.operator,
          _right4 = node.right;

      if (_left4.operator === '-') {
        _left4.operator = _operator3 === '-' ? '-' : '+';
        _left4.right = reduce({
          type: 'MathExpression',
          operator: _operator3 === '-' ? '+' : '-',
          left: _right4,
          right: _left4.right
        }, precision);
      } else {
        _left4.right = reduce({
          type: 'MathExpression',
          operator: _operator3,
          left: _left4.right,
          right: _right4
        }, precision);
      }

      if (_left4.right.value < 0) {
        _left4.right.value *= -1;
        _left4.operator = _left4.operator === '-' ? '+' : '-';
      }

      _left4.parenthesized = node.parenthesized;
      return reduce(_left4, precision);
    }
  } // (expr) + (expr) => number
  // (expr) - (expr) => number


  if (node.right.type === 'MathExpression' && node.left.type === 'MathExpression') {
    if (isEqual(node.left.right, node.right.right)) {
      var newNodes = covertNodesUnits(node.left.left, node.right.left, precision);
      node.left = newNodes.left;
      node.right = newNodes.right;
      return reduce(node);
    }

    if (isEqual(node.left.right, node.right.left)) {
      var _newNodes = covertNodesUnits(node.left.left, node.right.right, precision);

      node.left = _newNodes.left;
      node.right = _newNodes.right;
      return reduce(node);
    }
  }

  return node;
}

function reduceDivisionExpression(node) {
  if (!isValueType(node.right.type)) {
    return node;
  }

  if (node.right.type !== 'Number') {
    throw new Error(`Cannot divide by "${node.right.unit}", number expected`);
  }

  if (node.right.value === 0) {
    throw new Error('Cannot divide by zero');
  } // something / value


  if (isValueType(node.left.type)) {
    node.left.value /= node.right.value;
    return node.left;
  }

  return node;
}

function reduceMultiplicationExpression(node) {
  // (expr) * number
  if (node.left.type === 'MathExpression' && node.right.type === 'Number') {
    if (isValueType(node.left.left.type) && isValueType(node.left.right.type)) {
      node.left.left.value *= node.right.value;
      node.left.right.value *= node.right.value;
      return node.left;
    }
  } // something * number


  if (isValueType(node.left.type) && node.right.type === 'Number') {
    node.left.value *= node.right.value;
    return node.left;
  } // number * (expr)


  if (node.left.type === 'Number' && node.right.type === 'MathExpression') {
    if (isValueType(node.right.left.type) && isValueType(node.right.right.type)) {
      node.right.left.value *= node.left.value;
      node.right.right.value *= node.left.value;
      return node.right;
    }
  } // number * something


  if (node.left.type === 'Number' && isValueType(node.right.type)) {
    node.right.value *= node.left.value;
    return node.right;
  }

  return node;
}

function covertNodesUnits(left, right, precision) {
  switch (left.type) {
    case 'LengthValue':
    case 'AngleValue':
    case 'TimeValue':
    case 'FrequencyValue':
    case 'ResolutionValue':
      if (right.type === left.type && right.unit && left.unit) {
        var converted = (0, _convertUnit.default)(right.value, right.unit, left.unit, precision);
        right = {
          type: left.type,
          value: converted,
          unit: left.unit
        };
      }

      return {
        left,
        right
      };

    default:
      return {
        left,
        right
      };
  }
}

function reduce(node, precision) {
  if (node.type === "MathExpression") {
    node.left = reduce(node.left, precision);
    node.right = reduce(node.right, precision);

    switch (node.operator) {
      case "+":
      case "-":
        return reduceAddSubExpression(node, precision);

      case "/":
        return reduceDivisionExpression(node, precision);

      case "*":
        return reduceMultiplicationExpression(node, precision);
    }

    return node;
  }

  return node;
}

var _default = reduce;
exports.default = _default;
module.exports = exports.default;