import _pt from "prop-types";

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { canUseDOM } from '../../helpers';
import { AlertGroupInline } from './AlertGroupInline';
export class AlertGroup extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "state", {
      container: undefined
    });
  }

  componentDidMount() {
    const container = document.createElement('div');
    const target = this.getTargetElement();
    this.setState({
      container
    });
    target.appendChild(container);
  }

  componentWillUnmount() {
    const target = this.getTargetElement();

    if (this.state.container) {
      target.removeChild(this.state.container);
    }
  }

  getTargetElement() {
    const appendTo = this.props.appendTo;

    if (typeof appendTo === 'function') {
      return appendTo();
    }

    return appendTo || document.body;
  }

  render() {
    const {
      className,
      children,
      isToast
    } = this.props;
    const alertGroup = React.createElement(AlertGroupInline, {
      className: className,
      isToast: isToast
    }, children);

    if (!this.props.isToast) {
      return alertGroup;
    }

    const container = this.state.container;

    if (!canUseDOM || !container) {
      return null;
    }

    return ReactDOM.createPortal(alertGroup, container);
  }

}

_defineProperty(AlertGroup, "propTypes", {
  className: _pt.string,
  children: _pt.node,
  isToast: _pt.bool,
  appendTo: _pt.oneOfType([_pt.any, _pt.func])
});
//# sourceMappingURL=AlertGroup.js.map