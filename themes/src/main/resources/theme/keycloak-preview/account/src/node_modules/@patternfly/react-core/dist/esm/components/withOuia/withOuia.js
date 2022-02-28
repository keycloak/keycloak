import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { isOUIAEnvironment, getUniqueId, generateOUIAId } from './ouia';
export const OuiaContext = React.createContext(null);

/**
 * @param { React.ComponentClass | React.FunctionComponent } WrappedComponent - React component
 */
export function withOuiaContext(WrappedComponent) {
  /* eslint-disable react/display-name */
  return props => React.createElement(OuiaContext.Consumer, null, value => React.createElement(ComponentWithOuia, {
    consumerContext: value,
    component: WrappedComponent,
    componentProps: props
  }));
  /* eslint-enable react/display-name */
}

class ComponentWithOuia extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOuia: false,
      ouiaId: null
    };
  }
  /**
   * if either consumer set isOuia through context or local storage
   * then force a re-render
   */


  componentDidMount() {
    const {
      isOuia,
      ouiaId
    } = this.state;
    const {
      consumerContext
    } = this.props;
    const isOuiaEnv = isOUIAEnvironment();

    if (consumerContext && consumerContext.isOuia !== undefined && consumerContext.isOuia !== isOuia || isOuiaEnv !== isOuia) {
      this.setState({
        isOuia: consumerContext && consumerContext.isOuia !== undefined ? consumerContext.isOuia : isOuiaEnv,
        ouiaId: consumerContext && consumerContext.ouiaId !== undefined ? consumerContext.ouiaId : generateOUIAId() ? getUniqueId() : ouiaId
      });
    }
  }

  render() {
    const {
      isOuia,
      ouiaId
    } = this.state;
    const {
      component: WrappedComponent,
      componentProps,
      consumerContext
    } = this.props;
    return React.createElement(OuiaContext.Provider, {
      value: {
        isOuia: consumerContext && consumerContext.isOuia || isOuia,
        ouiaId: consumerContext && consumerContext.ouiaId || ouiaId
      }
    }, React.createElement(OuiaContext.Consumer, null, value => React.createElement(WrappedComponent, _extends({}, componentProps, {
      ouiaContext: value
    }))));
  }

}

_defineProperty(ComponentWithOuia, "propTypes", {
  component: _pt.any.isRequired,
  componentProps: _pt.any.isRequired,
  consumerContext: _pt.shape({
    isOuia: _pt.bool,
    ouiaId: _pt.oneOfType([_pt.number, _pt.string])
  })
});
//# sourceMappingURL=withOuia.js.map