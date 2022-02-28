'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = getImplicitRole;

var _ariaQuery = require('aria-query');

var _implicitRoles = require('./implicitRoles');

var _implicitRoles2 = _interopRequireDefault(_implicitRoles);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Returns an element's implicit role given its attributes and type.
 * Some elements only have an implicit role when certain props are defined.
 */
function getImplicitRole(type, attributes) {
  var implicitRole = void 0;
  if (_implicitRoles2.default[type]) {
    implicitRole = _implicitRoles2.default[type](attributes);
  }
  if (_ariaQuery.roles.has(implicitRole)) {
    return implicitRole;
  }
  return null;
}