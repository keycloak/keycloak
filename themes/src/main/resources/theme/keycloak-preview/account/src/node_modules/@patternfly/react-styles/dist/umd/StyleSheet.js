(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "emotion", "./utils"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("emotion"), require("./utils"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.emotion, global.utils);
    global.undefined = mod.exports;
  }
})(this, function (exports, _emotion, _utils) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.StyleSheet = undefined;
  exports.css = css;

  function ownKeys(object, enumerableOnly) {
    var keys = Object.keys(object);

    if (Object.getOwnPropertySymbols) {
      var symbols = Object.getOwnPropertySymbols(object);
      if (enumerableOnly) symbols = symbols.filter(function (sym) {
        return Object.getOwnPropertyDescriptor(object, sym).enumerable;
      });
      keys.push.apply(keys, symbols);
    }

    return keys;
  }

  function _objectSpread(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i] != null ? arguments[i] : {};

      if (i % 2) {
        ownKeys(source, true).forEach(function (key) {
          _defineProperty(target, key, source[key]);
        });
      } else if (Object.getOwnPropertyDescriptors) {
        Object.defineProperties(target, Object.getOwnPropertyDescriptors(source));
      } else {
        ownKeys(source).forEach(function (key) {
          Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key));
        });
      }
    }

    return target;
  }

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  const StyleSheet = exports.StyleSheet = {
    create(styleObj) {
      const keys = Object.keys(styleObj);

      if (keys.length > 0) {
        return keys.reduce((prev, key) => _objectSpread({}, prev, {
          [key]: (0, _emotion.css)(styleObj[key])
        }), {});
      }

      return (0, _emotion.css)(styleObj);
    },

    parse(input) {
      const classes = (0, _utils.getCSSClasses)(input);

      if (!classes) {
        return {};
      }

      return classes.reduce((map, className) => {
        const key = (0, _utils.formatClassName)(className);

        if (map[key]) {
          return map;
        }

        const value = (0, _utils.createStyleDeclaration)(className, input);

        if ((0, _utils.isModifier)(className)) {
          map.modifiers[key] = value;
        } else {
          map[key] = value;
        }

        return map;
      }, {
        modifiers: {},
        inject: () => (0, _emotion.injectGlobal)(input),
        raw: input
      });
    }

  };
  /**
   * @param {Array} styles - Array of styles
   */

  function css(...styles) {
    const filteredStyles = [];
    styles.forEach(style => {
      if ((0, _utils.isValidStyleDeclaration)(style)) {
        // remove global injection of styles in favor of require(css) in the component
        // style.__inject();
        filteredStyles.push((0, _utils.getClassName)(style));
        return;
      }

      filteredStyles.push(style);
    });
    return (0, _emotion.cx)(...filteredStyles);
  }
});
//# sourceMappingURL=StyleSheet.js.map