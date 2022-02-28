"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.getSize = exports.defaultProps = exports.propTypes = exports.IconSize = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var IconSize = {
  sm: 'sm',
  md: 'md',
  lg: 'lg',
  xl: 'xl'
};
exports.IconSize = IconSize;
var propTypes = {
  color: _propTypes["default"].string,
  size: _propTypes["default"].oneOf(Object.keys(IconSize)),
  title: _propTypes["default"].string,
  noVerticalAlign: _propTypes["default"].bool
};
exports.propTypes = propTypes;
var defaultProps = {
  color: 'currentColor',
  size: IconSize.sm,
  title: null,
  noVerticalAlign: false
};
exports.defaultProps = defaultProps;

var getSize = function getSize(size) {
  switch (size) {
    case IconSize.sm:
      return '1em';

    case IconSize.md:
      return '1.5em';

    case IconSize.lg:
      return '2em';

    case IconSize.xl:
      return '3em';

    default:
      return '1em';
  }
};

exports.getSize = getSize;
//# sourceMappingURL=common.js.map