(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "react"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("react"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.react);
    global.undefined = mod.exports;
  }
})(this, function (exports, _react) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.KeyTypes = exports.SelectDirection = exports.SelectVariant = exports.SelectConsumer = exports.SelectProvider = exports.SelectContext = undefined;

  var React = _interopRequireWildcard(_react);

  function _getRequireWildcardCache() {
    if (typeof WeakMap !== "function") return null;
    var cache = new WeakMap();

    _getRequireWildcardCache = function () {
      return cache;
    };

    return cache;
  }

  function _interopRequireWildcard(obj) {
    if (obj && obj.__esModule) {
      return obj;
    }

    var cache = _getRequireWildcardCache();

    if (cache && cache.has(obj)) {
      return cache.get(obj);
    }

    var newObj = {};

    if (obj != null) {
      var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor;

      for (var key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null;

          if (desc && (desc.get || desc.set)) {
            Object.defineProperty(newObj, key, desc);
          } else {
            newObj[key] = obj[key];
          }
        }
      }
    }

    newObj.default = obj;

    if (cache) {
      cache.set(obj, newObj);
    }

    return newObj;
  }

  const SelectContext = exports.SelectContext = React.createContext(null);
  const SelectProvider = exports.SelectProvider = SelectContext.Provider;
  const SelectConsumer = exports.SelectConsumer = SelectContext.Consumer;
  let SelectVariant = exports.SelectVariant = undefined;

  (function (SelectVariant) {
    SelectVariant["single"] = "single";
    SelectVariant["checkbox"] = "checkbox";
    SelectVariant["typeahead"] = "typeahead";
    SelectVariant["typeaheadMulti"] = "typeaheadmulti";
  })(SelectVariant || (exports.SelectVariant = SelectVariant = {}));

  let SelectDirection = exports.SelectDirection = undefined;

  (function (SelectDirection) {
    SelectDirection["up"] = "up";
    SelectDirection["down"] = "down";
  })(SelectDirection || (exports.SelectDirection = SelectDirection = {}));

  const KeyTypes = exports.KeyTypes = {
    Tab: 'Tab',
    Space: ' ',
    Escape: 'Escape',
    Enter: 'Enter',
    ArrowUp: 'ArrowUp',
    ArrowDown: 'ArrowDown'
  };
});
//# sourceMappingURL=selectConstants.js.map