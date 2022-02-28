(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Page/page", "@patternfly/react-styles", "@patternfly/react-tokens/dist/js/global_breakpoint_md", "../../helpers/util"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Page/page"), require("@patternfly/react-styles"), require("@patternfly/react-tokens/dist/js/global_breakpoint_md"), require("../../helpers/util"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.page, global.reactStyles, global.global_breakpoint_md, global.util);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _page, _reactStyles, _global_breakpoint_md, _util) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Page = exports.PageContextConsumer = exports.PageContextProvider = exports.PageLayouts = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _page2 = _interopRequireDefault(_page);

  var _global_breakpoint_md2 = _interopRequireDefault(_global_breakpoint_md);

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

  function _extends() {
    _extends = Object.assign || function (target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];

        for (var key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }

      return target;
    };

    return _extends.apply(this, arguments);
  }

  function _objectWithoutProperties(source, excluded) {
    if (source == null) return {};

    var target = _objectWithoutPropertiesLoose(source, excluded);

    var key, i;

    if (Object.getOwnPropertySymbols) {
      var sourceSymbolKeys = Object.getOwnPropertySymbols(source);

      for (i = 0; i < sourceSymbolKeys.length; i++) {
        key = sourceSymbolKeys[i];
        if (excluded.indexOf(key) >= 0) continue;
        if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue;
        target[key] = source[key];
      }
    }

    return target;
  }

  function _objectWithoutPropertiesLoose(source, excluded) {
    if (source == null) return {};
    var target = {};
    var sourceKeys = Object.keys(source);
    var key, i;

    for (i = 0; i < sourceKeys.length; i++) {
      key = sourceKeys[i];
      if (excluded.indexOf(key) >= 0) continue;
      target[key] = source[key];
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

  let PageLayouts = exports.PageLayouts = undefined;

  (function (PageLayouts) {
    PageLayouts["vertical"] = "vertical";
    PageLayouts["horizontal"] = "horizontal";
  })(PageLayouts || (exports.PageLayouts = PageLayouts = {}));

  const PageContext = React.createContext({});
  const PageContextProvider = exports.PageContextProvider = PageContext.Provider;
  const PageContextConsumer = exports.PageContextConsumer = PageContext.Consumer;

  class Page extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "handleResize", () => {
        const {
          onPageResize
        } = this.props;
        const windowSize = window.innerWidth; // eslint-disable-next-line radix

        const mobileView = windowSize < Number.parseInt(_global_breakpoint_md2.default.value, 10);

        if (onPageResize) {
          onPageResize({
            mobileView,
            windowSize
          });
        } // eslint-disable-next-line @typescript-eslint/no-unused-vars


        this.setState(prevState => ({
          mobileView
        }));
      });

      _defineProperty(this, "onNavToggleMobile", () => {
        this.setState(prevState => ({
          mobileIsNavOpen: !prevState.mobileIsNavOpen
        }));
      });

      _defineProperty(this, "onNavToggleDesktop", () => {
        this.setState(prevState => ({
          desktopIsNavOpen: !prevState.desktopIsNavOpen
        }));
      });

      const {
        isManagedSidebar,
        defaultManagedSidebarIsOpen
      } = props;
      const managedSidebarOpen = !isManagedSidebar ? true : defaultManagedSidebarIsOpen;
      this.state = {
        desktopIsNavOpen: managedSidebarOpen,
        mobileIsNavOpen: false,
        mobileView: false
      };
    }

    componentDidMount() {
      const {
        isManagedSidebar,
        onPageResize
      } = this.props;

      if (isManagedSidebar || onPageResize) {
        window.addEventListener('resize', (0, _util.debounce)(this.handleResize, 250)); // Initial check if should be shown

        this.handleResize();
      }
    }

    componentWillUnmount() {
      const {
        isManagedSidebar,
        onPageResize
      } = this.props;

      if (isManagedSidebar || onPageResize) {
        window.removeEventListener('resize', (0, _util.debounce)(this.handleResize, 250));
      }
    }

    render() {
      const _this$props = this.props,
            {
        breadcrumb,
        className,
        children,
        header,
        sidebar,
        skipToContent,
        role,
        mainContainerId,
        isManagedSidebar,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        defaultManagedSidebarIsOpen,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onPageResize,
        mainAriaLabel
      } = _this$props,
            rest = _objectWithoutProperties(_this$props, ["breadcrumb", "className", "children", "header", "sidebar", "skipToContent", "role", "mainContainerId", "isManagedSidebar", "defaultManagedSidebarIsOpen", "onPageResize", "mainAriaLabel"]);

      const {
        mobileView,
        mobileIsNavOpen,
        desktopIsNavOpen
      } = this.state;
      const context = {
        isManagedSidebar,
        onNavToggle: mobileView ? this.onNavToggleMobile : this.onNavToggleDesktop,
        isNavOpen: mobileView ? mobileIsNavOpen : desktopIsNavOpen
      };
      return React.createElement(PageContextProvider, {
        value: context
      }, React.createElement("div", _extends({}, rest, {
        className: (0, _reactStyles.css)(_page2.default.page, className)
      }), skipToContent, header, sidebar, React.createElement("main", {
        role: role,
        id: mainContainerId,
        className: (0, _reactStyles.css)(_page2.default.pageMain),
        tabIndex: -1,
        "aria-label": mainAriaLabel
      }, breadcrumb && React.createElement("section", {
        className: (0, _reactStyles.css)(_page2.default.pageMainBreadcrumb)
      }, breadcrumb), children)));
    }

  }

  exports.Page = Page;

  _defineProperty(Page, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    header: _propTypes2.default.node,
    sidebar: _propTypes2.default.node,
    skipToContent: _propTypes2.default.element,
    role: _propTypes2.default.string,
    mainContainerId: _propTypes2.default.string,
    isManagedSidebar: _propTypes2.default.bool,
    defaultManagedSidebarIsOpen: _propTypes2.default.bool,
    onPageResize: _propTypes2.default.func,
    breadcrumb: _propTypes2.default.node,
    mainAriaLabel: _propTypes2.default.string
  });

  _defineProperty(Page, "defaultProps", {
    breadcrumb: null,
    children: null,
    className: '',
    header: null,
    sidebar: null,
    skipToContent: null,
    isManagedSidebar: false,
    defaultManagedSidebarIsOpen: true,
    onPageResize: () => null,
    mainContainerId: null,
    role: undefined
  });
});
//# sourceMappingURL=Page.js.map