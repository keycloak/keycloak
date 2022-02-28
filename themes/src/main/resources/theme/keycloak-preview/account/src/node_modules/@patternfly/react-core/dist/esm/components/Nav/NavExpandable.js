import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import a11yStyles from '@patternfly/react-styles/css/utilities/Accessibility/accessibility';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import { getUniqueId } from '../../helpers/util';
import { NavContext } from './Nav';
export class NavExpandable extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "id", this.props.id || getUniqueId());

    _defineProperty(this, "state", {
      expandedState: this.props.isExpanded
    });

    _defineProperty(this, "onExpand", (e, val) => {
      if (this.props.onExpand) {
        this.props.onExpand(e, val);
      } else {
        this.setState({
          expandedState: val
        });
      }
    });

    _defineProperty(this, "handleToggle", (e, onToggle) => {
      // Item events can bubble up, ignore those
      if (e.target.getAttribute('data-component') !== 'pf-nav-expandable') {
        return;
      }

      const {
        groupId
      } = this.props;
      const {
        expandedState
      } = this.state;
      onToggle(e, groupId, !expandedState);
      this.onExpand(e, !expandedState);
    });
  }

  componentDidMount() {
    this.setState({
      expandedState: this.props.isExpanded
    });
  }

  componentDidUpdate(prevProps) {
    if (this.props.isExpanded !== prevProps.isExpanded) {
      this.setState({
        expandedState: this.props.isExpanded
      });
    }
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _this$props = this.props,
          {
      id,
      title,
      srText,
      children,
      className,
      isActive,
      groupId,
      isExpanded,
      onExpand
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["id", "title", "srText", "children", "className", "isActive", "groupId", "isExpanded", "onExpand"]);

    const {
      expandedState
    } = this.state;
    return React.createElement(NavContext.Consumer, null, context => React.createElement("li", _extends({
      className: css(styles.navItem, expandedState && styles.modifiers.expanded, isActive && styles.modifiers.current, className),
      onClick: e => this.handleToggle(e, context.onToggle)
    }, props), React.createElement("a", {
      "data-component": "pf-nav-expandable",
      className: css(styles.navLink),
      id: srText ? null : this.id,
      href: "#",
      onClick: e => e.preventDefault(),
      onMouseDown: e => e.preventDefault(),
      "aria-expanded": expandedState
    }, title, React.createElement("span", {
      className: css(styles.navToggle)
    }, React.createElement(AngleRightIcon, {
      "aria-hidden": "true"
    }))), React.createElement("section", {
      className: css(styles.navSubnav),
      "aria-labelledby": this.id,
      hidden: expandedState ? null : true
    }, srText && React.createElement("h2", {
      className: css(a11yStyles.screenReader),
      id: this.id
    }, srText), React.createElement("ul", {
      className: css(styles.navSimpleList)
    }, children))));
  }

}

_defineProperty(NavExpandable, "propTypes", {
  title: _pt.string.isRequired,
  srText: _pt.string,
  isExpanded: _pt.bool,
  children: _pt.node,
  className: _pt.string,
  groupId: _pt.oneOfType([_pt.string, _pt.number]),
  isActive: _pt.bool,
  id: _pt.string,
  onExpand: _pt.func
});

_defineProperty(NavExpandable, "defaultProps", {
  srText: '',
  isExpanded: false,
  children: '',
  className: '',
  groupId: null,
  isActive: false,
  id: ''
});
//# sourceMappingURL=NavExpandable.js.map