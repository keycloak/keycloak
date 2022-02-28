import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { canUseDOM } from '../../helpers';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Backdrop/backdrop';
import { KEY_CODES } from '../../helpers/constants';
import { ModalContent } from './ModalContent';
export class Modal extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "id", '');

    _defineProperty(this, "handleEscKeyClick", event => {
      if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.props.isOpen) {
        this.props.onClose();
      }
    });

    _defineProperty(this, "getElement", appendTo => {
      let target;

      if (typeof appendTo === 'function') {
        target = appendTo();
      } else {
        target = appendTo;
      }

      return target;
    });

    _defineProperty(this, "toggleSiblingsFromScreenReaders", hide => {
      const {
        appendTo
      } = this.props;
      const target = this.getElement(appendTo);
      const bodyChildren = target.children;

      for (const child of Array.from(bodyChildren)) {
        if (child !== this.state.container) {
          hide ? child.setAttribute('aria-hidden', '' + hide) : child.removeAttribute('aria-hidden');
        }
      }
    });

    const newId = Modal.currentId++;
    this.id = `pf-modal-${newId}`;
    this.state = {
      container: undefined
    };
  }

  componentDidMount() {
    const {
      appendTo
    } = this.props;
    const target = this.getElement(appendTo);
    const container = document.createElement('div');
    this.setState({
      container
    });
    target.appendChild(container);
    target.addEventListener('keydown', this.handleEscKeyClick, false);

    if (this.props.isOpen) {
      target.classList.add(css(styles.backdropOpen));
    } else {
      target.classList.remove(css(styles.backdropOpen));
    }
  }

  componentDidUpdate() {
    const {
      appendTo
    } = this.props;
    const target = this.getElement(appendTo);

    if (this.props.isOpen) {
      target.classList.add(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(true);
    } else {
      target.classList.remove(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(false);
    }
  }

  componentWillUnmount() {
    const {
      appendTo
    } = this.props;
    const target = this.getElement(appendTo);

    if (this.state.container) {
      target.removeChild(this.state.container);
    }

    target.removeEventListener('keydown', this.handleEscKeyClick, false);
    target.classList.remove(css(styles.backdropOpen));
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _this$props = this.props,
          {
      appendTo
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["appendTo"]);

    const {
      container
    } = this.state;

    if (!canUseDOM || !container) {
      return null;
    }

    return ReactDOM.createPortal(React.createElement(ModalContent, _extends({}, props, {
      title: this.props.title,
      id: this.id,
      ariaDescribedById: this.props.ariaDescribedById
    })), container);
  }

}

_defineProperty(Modal, "propTypes", {
  children: _pt.node.isRequired,
  className: _pt.string,
  isOpen: _pt.bool,
  header: _pt.node,
  title: _pt.string.isRequired,
  hideTitle: _pt.bool,
  showClose: _pt.bool,
  ariaDescribedById: _pt.string,
  footer: _pt.node,
  actions: _pt.any,
  isFooterLeftAligned: _pt.bool,
  onClose: _pt.func,
  width: _pt.oneOfType([_pt.number, _pt.string]),
  isLarge: _pt.bool,
  isSmall: _pt.bool,
  appendTo: _pt.oneOfType([_pt.any, _pt.func]),
  disableFocusTrap: _pt.bool,
  description: _pt.node
});

_defineProperty(Modal, "currentId", 0);

_defineProperty(Modal, "defaultProps", {
  className: '',
  isOpen: false,
  hideTitle: false,
  showClose: true,
  ariaDescribedById: '',
  actions: [],
  isFooterLeftAligned: false,
  onClose: () => undefined,
  isLarge: false,
  isSmall: false,
  appendTo: typeof document !== 'undefined' && document.body || null
});
//# sourceMappingURL=Modal.js.map