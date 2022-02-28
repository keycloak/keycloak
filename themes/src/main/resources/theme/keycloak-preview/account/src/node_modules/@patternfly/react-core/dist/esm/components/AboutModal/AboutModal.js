import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Backdrop/backdrop';
import { canUseDOM } from '../../helpers';
import { KEY_CODES } from '../../helpers/constants';
import { AboutModalContainer } from './AboutModalContainer';
export class AboutModal extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "id", AboutModal.currentId++);

    _defineProperty(this, "ariaLabelledBy", `pf-about-modal-title-${this.id}`);

    _defineProperty(this, "ariaDescribedBy", `pf-about-modal-content-${this.id}`);

    _defineProperty(this, "handleEscKeyClick", event => {
      if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.props.isOpen) {
        this.props.onClose();
      }
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

    _defineProperty(this, "getElement", appendTo => {
      if (typeof appendTo === 'function') {
        return appendTo();
      }

      return appendTo || document.body;
    });

    this.state = {
      container: undefined
    };

    if (props.brandImageSrc && !props.brandImageAlt) {
      // eslint-disable-next-line no-console
      console.error('AboutModal:', 'brandImageAlt is required when a brandImageSrc is specified');
    }
  }

  componentDidMount() {
    const container = document.createElement('div');
    const target = this.getElement(this.props.appendTo);
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
    const target = this.getElement(this.props.appendTo);

    if (this.props.isOpen) {
      target.classList.add(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(true);
    } else {
      target.classList.remove(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(false);
    }
  }

  componentWillUnmount() {
    const target = this.getElement(this.props.appendTo);

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

    return ReactDOM.createPortal(React.createElement(AboutModalContainer, _extends({
      ariaLabelledbyId: this.ariaLabelledBy,
      ariaDescribedById: this.ariaDescribedBy
    }, props)), container);
  }

}

_defineProperty(AboutModal, "propTypes", {
  children: _pt.node.isRequired,
  className: _pt.string,
  isOpen: _pt.bool,
  onClose: _pt.func,
  productName: _pt.string,
  trademark: _pt.string,
  brandImageSrc: _pt.string.isRequired,
  brandImageAlt: _pt.string.isRequired,
  backgroundImageSrc: _pt.string,
  noAboutModalBoxContentContainer: _pt.bool,
  appendTo: _pt.oneOfType([_pt.any, _pt.func]),
  closeButtonAriaLabel: _pt.string
});

_defineProperty(AboutModal, "currentId", 0);

_defineProperty(AboutModal, "defaultProps", {
  className: '',
  isOpen: false,
  onClose: () => undefined,
  productName: '',
  trademark: '',
  backgroundImageSrc: '',
  noAboutModalBoxContentContainer: false,
  appendTo: null
});
//# sourceMappingURL=AboutModal.js.map