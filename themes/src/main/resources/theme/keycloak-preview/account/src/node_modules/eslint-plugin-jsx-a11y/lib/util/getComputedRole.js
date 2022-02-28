'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = getComputedRole;

var _getExplicitRole = require('./getExplicitRole');

var _getExplicitRole2 = _interopRequireDefault(_getExplicitRole);

var _getImplicitRole = require('./getImplicitRole');

var _getImplicitRole2 = _interopRequireDefault(_getImplicitRole);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Returns an element's computed role, which is
 *
 *  1. The valid value of its explicit role attribute; or
 *  2. The implicit value of its tag.
 */
function getComputedRole(tag, attributes) {
  return (0, _getExplicitRole2.default)(tag, attributes) || (0, _getImplicitRole2.default)(tag, attributes);
}