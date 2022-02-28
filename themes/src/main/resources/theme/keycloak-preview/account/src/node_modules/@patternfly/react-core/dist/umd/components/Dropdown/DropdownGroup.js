(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "./dropdownConstants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("./dropdownConstants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.dropdownConstants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _dropdownConstants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DropdownGroup = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

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

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  const DropdownGroup = exports.DropdownGroup = ({
    children = null,
    className = '',
    label = ''
  }) => React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
    sectionClass,
    sectionTitleClass,
    sectionComponent
  }) => {
    const SectionComponent = sectionComponent;
    return React.createElement(SectionComponent, {
      className: (0, _reactStyles.css)(sectionClass, className)
    }, label && React.createElement("h1", {
      className: (0, _reactStyles.css)(sectionTitleClass),
      "aria-hidden": true
    }, label), React.createElement("ul", {
      role: "none"
    }, children));
  });

  DropdownGroup.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    label: _propTypes2.default.node
  };
});
//# sourceMappingURL=DropdownGroup.js.map