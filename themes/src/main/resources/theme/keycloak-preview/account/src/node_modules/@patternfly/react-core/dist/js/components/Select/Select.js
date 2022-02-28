"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Select = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _select = _interopRequireDefault(require("@patternfly/react-styles/css/components/Select/select"));

var _badge = _interopRequireDefault(require("@patternfly/react-styles/css/components/Badge/badge"));

var _formControl = _interopRequireDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));

var _button = _interopRequireDefault(require("@patternfly/react-styles/css/components/Button/button"));

var _reactStyles = require("@patternfly/react-styles");

var _timesCircleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/times-circle-icon"));

var _SelectMenu = require("./SelectMenu");

var _SelectOption = require("./SelectOption");

var _SelectToggle = require("./SelectToggle");

var _selectConstants = require("./selectConstants");

var _ChipGroup = require("../ChipGroup");

var _util = require("../../helpers/util");

var _withOuia = require("../withOuia");

var _Divider = require("../Divider");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance"); }

function _iterableToArray(iter) { if (Symbol.iterator in Object(iter) || Object.prototype.toString.call(iter) === "[object Arguments]") return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = new Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

// seed for the aria-labelledby ID
var currentId = 0;

var Select =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Select, _React$Component);

  function Select() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, Select);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(Select)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "parentRef", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "filterRef", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "refCollection", []);

    _defineProperty(_assertThisInitialized(_this), "state", {
      openedOnEnter: false,
      typeaheadInputValue: null,
      typeaheadActiveChild: null,
      typeaheadFilteredChildren: React.Children.toArray(_this.props.children),
      typeaheadCurrIndex: -1,
      creatableValue: ''
    });

    _defineProperty(_assertThisInitialized(_this), "componentDidUpdate", function (prevProps, prevState) {
      if (_this.props.hasInlineFilter) {
        _this.refCollection[0] = _this.filterRef.current;
      }

      if (!prevState.openedOnEnter && _this.state.openedOnEnter) {
        _this.refCollection[0].focus();
      }

      if (prevProps.children !== _this.props.children) {
        _this.setState({
          typeaheadFilteredChildren: React.Children.toArray(_this.props.children)
        });
      }

      if (prevProps.selections !== _this.props.selections && _this.props.variant === _selectConstants.SelectVariant.typeahead) {
        _this.setState({
          typeaheadInputValue: _this.props.selections
        });
      }
    });

    _defineProperty(_assertThisInitialized(_this), "onEnter", function () {
      _this.setState({
        openedOnEnter: true
      });
    });

    _defineProperty(_assertThisInitialized(_this), "onClose", function () {
      _this.setState({
        openedOnEnter: false,
        typeaheadInputValue: null,
        typeaheadActiveChild: null,
        typeaheadFilteredChildren: React.Children.toArray(_this.props.children),
        typeaheadCurrIndex: -1
      });
    });

    _defineProperty(_assertThisInitialized(_this), "onChange", function (e) {
      var _this$props = _this.props,
          onFilter = _this$props.onFilter,
          isCreatable = _this$props.isCreatable,
          onCreateOption = _this$props.onCreateOption,
          createText = _this$props.createText,
          noResultsFoundText = _this$props.noResultsFoundText,
          children = _this$props.children;
      var typeaheadFilteredChildren;

      if (onFilter) {
        typeaheadFilteredChildren = onFilter(e) || children;
      } else {
        var input;

        try {
          input = new RegExp(e.target.value.toString(), 'i');
        } catch (err) {
          input = new RegExp(e.target.value.toString().replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
        }

        typeaheadFilteredChildren = e.target.value.toString() !== '' ? React.Children.toArray(_this.props.children).filter(function (child) {
          return _this.getDisplay(child.props.value.toString(), 'text').search(input) === 0;
        }) : React.Children.toArray(_this.props.children);
      }

      if (!typeaheadFilteredChildren) {
        typeaheadFilteredChildren = [];
      }

      if (typeaheadFilteredChildren.length === 0) {
        !isCreatable && typeaheadFilteredChildren.push(React.createElement(_SelectOption.SelectOption, {
          isDisabled: true,
          key: 0,
          value: noResultsFoundText,
          isNoResultsOption: true
        }));
      }

      if (isCreatable && e.target.value !== '') {
        var newValue = e.target.value;
        typeaheadFilteredChildren.push(React.createElement(_SelectOption.SelectOption, {
          key: 0,
          value: newValue,
          onClick: function onClick() {
            return onCreateOption && onCreateOption(newValue);
          }
        }, createText, " \"", newValue, "\""));
      }

      _this.setState({
        typeaheadInputValue: e.target.value,
        typeaheadCurrIndex: -1,
        typeaheadFilteredChildren: typeaheadFilteredChildren,
        typeaheadActiveChild: null,
        creatableValue: e.target.value
      });

      _this.refCollection = [];
    });

    _defineProperty(_assertThisInitialized(_this), "onClick", function (e) {
      e.stopPropagation();
    });

    _defineProperty(_assertThisInitialized(_this), "clearSelection", function (e) {
      e.stopPropagation();

      _this.setState({
        typeaheadInputValue: null,
        typeaheadActiveChild: null,
        typeaheadFilteredChildren: React.Children.toArray(_this.props.children),
        typeaheadCurrIndex: -1
      });
    });

    _defineProperty(_assertThisInitialized(_this), "sendRef", function (ref, index) {
      _this.refCollection[index] = ref;
    });

    _defineProperty(_assertThisInitialized(_this), "handleArrowKeys", function (index, position) {
      (0, _util.keyHandler)(index, 0, position, _this.refCollection, _this.refCollection);
    });

    _defineProperty(_assertThisInitialized(_this), "handleFocus", function () {
      if (!_this.props.isExpanded) {
        _this.props.onToggle(true);
      }
    });

    _defineProperty(_assertThisInitialized(_this), "handleTypeaheadKeys", function (position) {
      var _this$props2 = _this.props,
          isExpanded = _this$props2.isExpanded,
          isCreatable = _this$props2.isCreatable,
          createText = _this$props2.createText;
      var _this$state = _this.state,
          typeaheadActiveChild = _this$state.typeaheadActiveChild,
          typeaheadCurrIndex = _this$state.typeaheadCurrIndex;

      if (isExpanded) {
        if (position === 'enter' && (typeaheadActiveChild || _this.refCollection[0])) {
          _this.setState({
            typeaheadInputValue: typeaheadActiveChild && typeaheadActiveChild.innerText || _this.refCollection[0].innerText
          });

          if (typeaheadActiveChild) {
            typeaheadActiveChild.click();
          } else {
            _this.refCollection[0].click();
          }
        } else {
          var nextIndex;

          if (typeaheadCurrIndex === -1 && position === 'down') {
            nextIndex = 0;
          } else if (typeaheadCurrIndex === -1 && position === 'up') {
            nextIndex = _this.refCollection.length - 1;
          } else {
            nextIndex = (0, _util.getNextIndex)(typeaheadCurrIndex, position, _this.refCollection);
          }

          _this.setState({
            typeaheadCurrIndex: nextIndex,
            typeaheadActiveChild: _this.refCollection[nextIndex],
            typeaheadInputValue: isCreatable && _this.refCollection[nextIndex].innerText.includes(createText) ? _this.state.creatableValue : _this.refCollection[nextIndex].innerText
          });
        }
      }
    });

    _defineProperty(_assertThisInitialized(_this), "getDisplay", function (value) {
      var type = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 'node';

      if (!value) {
        return;
      }

      var _this$props3 = _this.props,
          children = _this$props3.children,
          isGrouped = _this$props3.isGrouped;
      var item = children.filter(function (child) {
        return child.props.value !== undefined && child.props.value.toString() === value.toString();
      })[0];

      if (isGrouped) {
        item = children.reduce(function (acc, curr) {
          return [].concat(_toConsumableArray(acc), _toConsumableArray(React.Children.toArray(curr.props.children)));
        }, []).filter(function (child) {
          return child.props.value.toString() === value.toString();
        })[0];
      }

      if (item) {
        if (item && item.props.children) {
          if (type === 'node') {
            return item.props.children;
          }

          return _this.findText(item);
        }

        return item.props.value.toString();
      }

      return value;
    });

    _defineProperty(_assertThisInitialized(_this), "findText", function (item) {
      if (!item.props || !item.props.children) {
        if (typeof item !== 'string') {
          return '';
        }

        return item;
      }

      if (typeof item.props.children === 'string') {
        return item.props.children;
      }

      var multi = [];
      React.Children.toArray(item.props.children).forEach(function (child) {
        return multi.push(_this.findText(child));
      });
      return multi.join('');
    });

    return _this;
  }

  _createClass(Select, [{
    key: "extendTypeaheadChildren",
    value: function extendTypeaheadChildren(typeaheadActiveChild) {
      var _this2 = this;

      return this.state.typeaheadFilteredChildren.map(function (child) {
        return React.cloneElement(child, {
          isFocused: typeaheadActiveChild && (typeaheadActiveChild.innerText === _this2.getDisplay(child.props.value.toString(), 'text') || _this2.props.isCreatable && typeaheadActiveChild.innerText === "{createText} \"".concat(child.props.value, "\""))
        });
      });
    }
  }, {
    key: "render",
    value: function render() {
      var _this3 = this;

      /* eslint-disable @typescript-eslint/no-unused-vars */
      var _this$props4 = this.props,
          children = _this$props4.children,
          className = _this$props4.className,
          customContent = _this$props4.customContent,
          variant = _this$props4.variant,
          direction = _this$props4.direction,
          onToggle = _this$props4.onToggle,
          onSelect = _this$props4.onSelect,
          onClear = _this$props4.onClear,
          onFilter = _this$props4.onFilter,
          onCreateOption = _this$props4.onCreateOption,
          toggleId = _this$props4.toggleId,
          isExpanded = _this$props4.isExpanded,
          isGrouped = _this$props4.isGrouped,
          isPlain = _this$props4.isPlain,
          isDisabled = _this$props4.isDisabled,
          isCreatable = _this$props4.isCreatable,
          selections = _this$props4.selections,
          isCheckboxSelectionBadgeHidden = _this$props4.isCheckboxSelectionBadgeHidden,
          ariaLabelledBy = _this$props4.ariaLabelledBy,
          ariaLabelTypeAhead = _this$props4.ariaLabelTypeAhead,
          ariaLabelClear = _this$props4.ariaLabelClear,
          ariaLabelToggle = _this$props4.ariaLabelToggle,
          ariaLabelRemove = _this$props4.ariaLabelRemove,
          ariaLabel = _this$props4['aria-label'],
          placeholderText = _this$props4.placeholderText,
          width = _this$props4.width,
          maxHeight = _this$props4.maxHeight,
          toggleIcon = _this$props4.toggleIcon,
          ouiaContext = _this$props4.ouiaContext,
          ouiaId = _this$props4.ouiaId,
          createText = _this$props4.createText,
          noResultsFoundText = _this$props4.noResultsFoundText,
          hasInlineFilter = _this$props4.hasInlineFilter,
          props = _objectWithoutProperties(_this$props4, ["children", "className", "customContent", "variant", "direction", "onToggle", "onSelect", "onClear", "onFilter", "onCreateOption", "toggleId", "isExpanded", "isGrouped", "isPlain", "isDisabled", "isCreatable", "selections", "isCheckboxSelectionBadgeHidden", "ariaLabelledBy", "ariaLabelTypeAhead", "ariaLabelClear", "ariaLabelToggle", "ariaLabelRemove", "aria-label", "placeholderText", "width", "maxHeight", "toggleIcon", "ouiaContext", "ouiaId", "createText", "noResultsFoundText", "hasInlineFilter"]);
      /* eslint-enable @typescript-eslint/no-unused-vars */


      var _this$state2 = this.state,
          openedOnEnter = _this$state2.openedOnEnter,
          typeaheadInputValue = _this$state2.typeaheadInputValue,
          typeaheadActiveChild = _this$state2.typeaheadActiveChild,
          typeaheadFilteredChildren = _this$state2.typeaheadFilteredChildren;
      var selectToggleId = toggleId || "pf-toggle-id-".concat(currentId++);
      var childPlaceholderText = null;

      if (!customContent) {
        if (!selections && !placeholderText) {
          var childPlaceholder = React.Children.toArray(children.filter(function (child) {
            return child.props.isPlaceholder === true;
          }));
          childPlaceholderText = childPlaceholder[0] && this.getDisplay(childPlaceholder[0].props.value, 'node') || children[0] && this.getDisplay(children[0].props.value, 'node');
        }
      }

      var hasOnClear = onClear !== Select.defaultProps.onClear;
      var hasAnySelections = selections && (Array.isArray(selections) ? selections.length > 0 ? true : false : selections !== '');
      var clearBtn = React.createElement("button", {
        className: (0, _reactStyles.css)(_button["default"].button, _button["default"].modifiers.plain, _select["default"].selectToggleClear),
        onClick: function onClick(e) {
          _this3.clearSelection(e);

          onClear(e);
        },
        "aria-label": ariaLabelClear,
        type: "button",
        disabled: isDisabled
      }, React.createElement(_timesCircleIcon["default"], {
        "aria-hidden": true
      }));
      var selectedChips = null;

      if (variant === _selectConstants.SelectVariant.typeaheadMulti) {
        selectedChips = React.createElement(_ChipGroup.ChipGroup, null, selections && selections.map(function (item) {
          var isItemDisabled = React.Children.toArray(children.filter(function (child) {
            return child.props.value === item;
          }))[0].props.isDisabled;
          return React.createElement(_ChipGroup.Chip, _extends({
            key: item,
            onClick: function onClick(e) {
              return onSelect(e, item);
            },
            closeBtnAriaLabel: ariaLabelRemove
          }, isItemDisabled && {
            isReadOnly: true
          }), _this3.getDisplay(item, 'node'));
        }));
      }

      var filterWithChildren = children;

      if (hasInlineFilter) {
        var filterBox = React.createElement(React.Fragment, null, React.createElement("div", {
          key: "inline-filter",
          className: (0, _reactStyles.css)(_select["default"].selectMenuInput)
        }, React.createElement("input", {
          key: "inline-filter-input",
          type: "search",
          className: (0, _reactStyles.css)(_formControl["default"].formControl, _formControl["default"].modifiers.search),
          onChange: this.onChange,
          onKeyDown: function onKeyDown(event) {
            if (event.key === _selectConstants.KeyTypes.ArrowUp) {
              _this3.handleArrowKeys(0, 'up');
            } else if (event.key === _selectConstants.KeyTypes.ArrowDown) {
              _this3.handleArrowKeys(0, 'down');
            }
          },
          ref: this.filterRef,
          autoComplete: "off"
        })), React.createElement(_Divider.Divider, {
          key: "inline-filter-divider"
        }));
        this.refCollection[0] = this.filterRef.current;
        filterWithChildren = [filterBox].concat(_toConsumableArray(typeaheadFilteredChildren)).map(function (option, index) {
          return React.cloneElement(option, {
            key: index
          });
        });
      }

      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_select["default"].select, isExpanded && _select["default"].modifiers.expanded, direction === _selectConstants.SelectDirection.up && _select["default"].modifiers.top, className),
        ref: this.parentRef,
        style: {
          width: width
        }
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Select',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }), React.createElement(_selectConstants.SelectContext.Provider, {
        value: {
          onSelect: onSelect,
          onClose: this.onClose,
          variant: variant
        }
      }, React.createElement(_SelectToggle.SelectToggle, {
        id: selectToggleId,
        parentRef: this.parentRef,
        isExpanded: isExpanded,
        isPlain: isPlain,
        onToggle: onToggle,
        onEnter: this.onEnter,
        onClose: this.onClose,
        ariaLabelledBy: "".concat(ariaLabelledBy || '', " ").concat(selectToggleId),
        variant: variant,
        ariaLabelToggle: ariaLabelToggle,
        handleTypeaheadKeys: this.handleTypeaheadKeys,
        isDisabled: isDisabled,
        hasClearButton: hasOnClear
      }, customContent && React.createElement("div", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleWrapper)
      }, toggleIcon && React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleIcon)
      }, toggleIcon), React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleText)
      }, placeholderText)), variant === _selectConstants.SelectVariant.single && !customContent && React.createElement("div", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleWrapper)
      }, toggleIcon && React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleIcon)
      }, toggleIcon), React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleText)
      }, this.getDisplay(selections, 'node') || placeholderText || childPlaceholderText), hasOnClear && hasAnySelections && clearBtn), variant === _selectConstants.SelectVariant.checkbox && !customContent && React.createElement(React.Fragment, null, React.createElement("div", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleWrapper)
      }, toggleIcon && React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleIcon)
      }, toggleIcon), React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleText)
      }, placeholderText), !isCheckboxSelectionBadgeHidden && selections && Array.isArray(selections) && selections.length > 0 && React.createElement("div", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleBadge)
      }, React.createElement("span", {
        className: (0, _reactStyles.css)(_badge["default"].badge, _badge["default"].modifiers.read)
      }, selections.length))), hasOnClear && hasAnySelections && clearBtn), variant === _selectConstants.SelectVariant.typeahead && !customContent && React.createElement(React.Fragment, null, React.createElement("div", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleWrapper)
      }, toggleIcon && React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleIcon)
      }, toggleIcon), React.createElement("input", {
        className: (0, _reactStyles.css)(_formControl["default"].formControl, _select["default"].selectToggleTypeahead),
        "aria-activedescendant": typeaheadActiveChild && typeaheadActiveChild.id,
        id: "".concat(selectToggleId, "-select-typeahead"),
        "aria-label": ariaLabelTypeAhead,
        placeholder: placeholderText,
        value: typeaheadInputValue !== null ? typeaheadInputValue : this.getDisplay(selections, 'text') || '',
        type: "text",
        onClick: this.onClick,
        onChange: this.onChange,
        onFocus: this.handleFocus,
        autoComplete: "off",
        disabled: isDisabled
      })), (selections || typeaheadInputValue) && clearBtn), variant === _selectConstants.SelectVariant.typeaheadMulti && !customContent && React.createElement(React.Fragment, null, React.createElement("div", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleWrapper)
      }, toggleIcon && React.createElement("span", {
        className: (0, _reactStyles.css)(_select["default"].selectToggleIcon)
      }, toggleIcon), selections && Array.isArray(selections) && selections.length > 0 && selectedChips, React.createElement("input", {
        className: (0, _reactStyles.css)(_formControl["default"].formControl, _select["default"].selectToggleTypeahead),
        "aria-activedescendant": typeaheadActiveChild && typeaheadActiveChild.id,
        id: "".concat(selectToggleId, "-select-multi-typeahead-typeahead"),
        "aria-label": ariaLabelTypeAhead,
        placeholder: placeholderText,
        value: typeaheadInputValue !== null ? typeaheadInputValue : '',
        type: "text",
        onChange: this.onChange,
        onClick: this.onClick,
        onFocus: this.handleFocus,
        autoComplete: "off",
        disabled: isDisabled
      })), (selections && Array.isArray(selections) && selections.length > 0 || typeaheadInputValue) && clearBtn)), customContent && isExpanded && React.createElement(_SelectMenu.SelectMenu, _extends({}, props, {
        selected: selections,
        openedOnEnter: openedOnEnter,
        "aria-label": ariaLabel,
        "aria-labelledby": ariaLabelledBy,
        sendRef: this.sendRef,
        keyHandler: this.handleArrowKeys,
        maxHeight: maxHeight,
        isCustomContent: true
      }), customContent), variant === _selectConstants.SelectVariant.single && isExpanded && !customContent && React.createElement(_SelectMenu.SelectMenu, _extends({}, props, {
        isGrouped: isGrouped,
        selected: selections,
        openedOnEnter: openedOnEnter,
        "aria-label": ariaLabel,
        "aria-labelledby": ariaLabelledBy,
        sendRef: this.sendRef,
        keyHandler: this.handleArrowKeys,
        maxHeight: maxHeight
      }), children), variant === _selectConstants.SelectVariant.checkbox && isExpanded && !customContent && React.createElement(_SelectMenu.SelectMenu, _extends({}, props, {
        checked: selections ? selections : [],
        "aria-label": ariaLabel,
        "aria-labelledby": ariaLabelledBy,
        isGrouped: isGrouped,
        sendRef: this.sendRef,
        keyHandler: this.handleArrowKeys,
        maxHeight: maxHeight,
        hasInlineFilter: hasInlineFilter
      }), filterWithChildren), (variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti) && isExpanded && !customContent && React.createElement(_SelectMenu.SelectMenu, _extends({}, props, {
        selected: selections,
        openedOnEnter: openedOnEnter,
        "aria-label": ariaLabel,
        "aria-labelledby": ariaLabelledBy,
        sendRef: this.sendRef,
        keyHandler: this.handleArrowKeys,
        maxHeight: maxHeight
      }), this.extendTypeaheadChildren(typeaheadActiveChild))));
    }
  }]);

  return Select;
}(React.Component);

_defineProperty(Select, "propTypes", {
  children: _propTypes["default"].arrayOf(_propTypes["default"].element),
  className: _propTypes["default"].string,
  direction: _propTypes["default"].oneOf(['up', 'down']),
  isExpanded: _propTypes["default"].bool,
  isGrouped: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isCreatable: _propTypes["default"].bool,
  createText: _propTypes["default"].string,
  placeholderText: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].node]),
  noResultsFoundText: _propTypes["default"].string,
  selections: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].any, _propTypes["default"].arrayOf(_propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].any]))]),
  isCheckboxSelectionBadgeHidden: _propTypes["default"].bool,
  toggleId: _propTypes["default"].string,
  'aria-label': _propTypes["default"].string,
  ariaLabelledBy: _propTypes["default"].string,
  ariaLabelTypeAhead: _propTypes["default"].string,
  ariaLabelClear: _propTypes["default"].string,
  ariaLabelToggle: _propTypes["default"].string,
  ariaLabelRemove: _propTypes["default"].string,
  onSelect: _propTypes["default"].func,
  onToggle: _propTypes["default"].func.isRequired,
  onClear: _propTypes["default"].func,
  onFilter: _propTypes["default"].func,
  onCreateOption: _propTypes["default"].func
});

_defineProperty(Select, "defaultProps", {
  children: [],
  className: '',
  direction: _selectConstants.SelectDirection.down,
  toggleId: null,
  isExpanded: false,
  isGrouped: false,
  isPlain: false,
  isDisabled: false,
  isCreatable: false,
  'aria-label': '',
  ariaLabelledBy: '',
  ariaLabelTypeAhead: '',
  ariaLabelClear: 'Clear all',
  ariaLabelToggle: 'Options menu',
  ariaLabelRemove: 'Remove',
  selections: '',
  createText: 'Create',
  placeholderText: '',
  noResultsFoundText: 'No results found',
  variant: _selectConstants.SelectVariant.single,
  width: '',
  onClear: function onClear() {
    return undefined;
  },
  onCreateOption: function onCreateOption() {
    return undefined;
  },
  toggleIcon: null,
  onFilter: null,
  customContent: null,
  hasInlineFilter: false
});

var SelectWithOuiaContext = (0, _withOuia.withOuiaContext)(Select);
exports.Select = SelectWithOuiaContext;
//# sourceMappingURL=Select.js.map