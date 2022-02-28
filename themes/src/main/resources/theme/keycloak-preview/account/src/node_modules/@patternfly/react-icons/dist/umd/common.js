(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.getSize = exports.defaultProps = exports.propTypes = exports.IconSize = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  const IconSize = exports.IconSize = {
    sm: 'sm',
    md: 'md',
    lg: 'lg',
    xl: 'xl'
  };
  const propTypes = exports.propTypes = {
    color: _propTypes2.default.string,
    size: _propTypes2.default.oneOf(Object.keys(IconSize)),
    title: _propTypes2.default.string,
    noVerticalAlign: _propTypes2.default.bool
  };
  const defaultProps = exports.defaultProps = {
    color: 'currentColor',
    size: IconSize.sm,
    title: null,
    noVerticalAlign: false
  };

  const getSize = exports.getSize = size => {
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
});
//# sourceMappingURL=common.js.map