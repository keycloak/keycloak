import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { canUseDOM } from '../../helpers';
import { KEY_CODES } from '../../helpers/constants';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { Backdrop } from '../Backdrop';
import { Bullseye } from '../../layouts/Bullseye';
import { WizardHeader } from './WizardHeader';
import { WizardFooterInternal } from './WizardFooterInternal';
import { WizardToggle } from './WizardToggle';
import { WizardNav } from './WizardNav';
import { WizardNavItem } from './WizardNavItem';
import { WizardContextProvider } from './WizardContext';
// Can't use ES6 imports :(
// The types for it are also wrong, we should probably ditch this dependency.
import { FocusTrap } from '../../helpers';
export class Wizard extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleKeyClicks", event => {
      if (event.keyCode === KEY_CODES.ESCAPE_KEY) {
        if (this.state.isNavOpen) {
          this.setState({
            isNavOpen: !this.state.isNavOpen
          });
        } else if (this.props.isOpen) {
          this.props.onClose();
        }
      }
    });

    _defineProperty(this, "toggleSiblingsFromScreenReaders", hide => {
      const {
        appendTo
      } = this.props;
      const target = this.getElement(appendTo);
      const bodyChildren = target.children;

      for (const child of Array.from(bodyChildren)) {
        if (child !== this.container) {
          hide ? child.setAttribute('aria-hidden', '' + hide) : child.removeAttribute('aria-hidden');
        }
      }
    });

    _defineProperty(this, "onNext", () => {
      const {
        onNext,
        onClose,
        onSave
      } = this.props;
      const {
        currentStep
      } = this.state;
      const flattenedSteps = this.getFlattenedSteps();
      const maxSteps = flattenedSteps.length;

      if (currentStep >= maxSteps) {
        // Hit the save button at the end of the wizard
        if (onSave) {
          return onSave();
        }

        return onClose();
      } else {
        const newStep = currentStep + 1;
        this.setState({
          currentStep: newStep
        });
        const {
          id: prevId,
          name: prevName
        } = flattenedSteps[currentStep - 1];
        const {
          id,
          name
        } = flattenedSteps[newStep - 1];
        return onNext && onNext({
          id,
          name
        }, {
          prevId,
          prevName
        });
      }
    });

    _defineProperty(this, "onBack", () => {
      const {
        onBack
      } = this.props;
      const {
        currentStep
      } = this.state;
      const flattenedSteps = this.getFlattenedSteps();

      if (flattenedSteps.length < currentStep) {
        // Previous step was removed, just update the currentStep state
        const adjustedStep = flattenedSteps.length;
        this.setState({
          currentStep: adjustedStep
        });
      } else {
        const newStep = currentStep - 1 <= 0 ? 0 : currentStep - 1;
        this.setState({
          currentStep: newStep
        });
        const {
          id: prevId,
          name: prevName
        } = flattenedSteps[newStep];
        const {
          id,
          name
        } = flattenedSteps[newStep - 1];
        return onBack && onBack({
          id,
          name
        }, {
          prevId,
          prevName
        });
      }
    });

    _defineProperty(this, "goToStep", step => {
      const {
        onGoToStep
      } = this.props;
      const {
        currentStep
      } = this.state;
      const flattenedSteps = this.getFlattenedSteps();
      const maxSteps = flattenedSteps.length;

      if (step < 1) {
        step = 1;
      } else if (step > maxSteps) {
        step = maxSteps;
      }

      this.setState({
        currentStep: step,
        isNavOpen: false
      });
      const {
        id: prevId,
        name: prevName
      } = flattenedSteps[currentStep - 1];
      const {
        id,
        name
      } = flattenedSteps[step - 1];
      return onGoToStep && onGoToStep({
        id,
        name
      }, {
        prevId,
        prevName
      });
    });

    _defineProperty(this, "goToStepById", stepId => {
      const flattenedSteps = this.getFlattenedSteps();
      let step;

      for (let i = 0; i < flattenedSteps.length; i++) {
        if (flattenedSteps[i].id === stepId) {
          step = i + 1;
          break;
        }
      }

      if (step) {
        this.setState({
          currentStep: step
        });
      }
    });

    _defineProperty(this, "goToStepByName", stepName => {
      const flattenedSteps = this.getFlattenedSteps();
      let step;

      for (let i = 0; i < flattenedSteps.length; i++) {
        if (flattenedSteps[i].name === stepName) {
          step = i + 1;
          break;
        }
      }

      if (step) {
        this.setState({
          currentStep: step
        });
      }
    });

    _defineProperty(this, "getFlattenedSteps", () => {
      const {
        steps
      } = this.props;
      const flattenedSteps = [];

      for (const step of steps) {
        if (step.steps) {
          for (const childStep of step.steps) {
            flattenedSteps.push(childStep);
          }
        } else {
          flattenedSteps.push(step);
        }
      }

      return flattenedSteps;
    });

    _defineProperty(this, "getFlattenedStepsIndex", (flattenedSteps, stepName) => {
      for (let i = 0; i < flattenedSteps.length; i++) {
        if (flattenedSteps[i].name === stepName) {
          return i + 1;
        }
      }

      return 0;
    });

    _defineProperty(this, "initSteps", steps => {
      // Set default Step values
      for (let i = 0; i < steps.length; i++) {
        if (steps[i].steps) {
          for (let j = 0; j < steps[i].steps.length; j++) {
            steps[i].steps[j] = Object.assign({
              canJumpTo: true
            }, steps[i].steps[j]);
          }
        }

        steps[i] = Object.assign({
          canJumpTo: true
        }, steps[i]);
      }

      return steps;
    });

    _defineProperty(this, "getElement", appendTo => {
      if (typeof appendTo === 'function') {
        return appendTo();
      }

      return appendTo || document.body;
    });

    const newId = Wizard.currentId++;
    this.isModal = !props.isInPage;

    if (this.isModal) {
      this.titleId = `pf-wizard-title-${newId}`;
      this.descriptionId = `pf-wizard-description-${newId}`;
    }

    this.state = {
      currentStep: this.props.startAtStep && Number.isInteger(this.props.startAtStep) ? this.props.startAtStep : 1,
      isNavOpen: false
    };
  }

  componentDidMount() {
    const {
      appendTo
    } = this.props;
    const target = this.getElement(appendTo);

    if (this.isModal) {
      if (this.container) {
        target.appendChild(this.container);
      }

      this.toggleSiblingsFromScreenReaders(true);
      target.addEventListener('keydown', this.handleKeyClicks, false);
    }
  }

  componentWillUnmount() {
    const {
      appendTo
    } = this.props;
    const target = this.getElement(appendTo);

    if (this.isModal) {
      if (this.container) {
        target.removeChild(this.container);
      }

      this.toggleSiblingsFromScreenReaders(false);
      target.removeEventListener('keydown', this.handleKeyClicks, false);
    }
  }

  render() {
    if (this.isModal) {
      if (!canUseDOM) {
        return null;
      }

      if (!this.container) {
        this.container = document.createElement('div');
      }
    }

    const _this$props = this.props,
          {
      /* eslint-disable @typescript-eslint/no-unused-vars */
      isOpen,
      isInPage,
      isFullHeight,
      isFullWidth,
      width,
      height,
      title,
      description,
      onClose,
      onSave,
      onBack,
      onNext,
      onGoToStep,
      className,
      steps,
      startAtStep,
      nextButtonText = 'Next',
      backButtonText = 'Back',
      cancelButtonText = 'Cancel',
      ariaLabelCloseButton = 'Close',
      ariaLabelNav,
      hasBodyPadding,
      footer,
      isCompactNav,
      appendTo
      /* eslint-enable @typescript-eslint/no-unused-vars */

    } = _this$props,
          rest = _objectWithoutProperties(_this$props, ["isOpen", "isInPage", "isFullHeight", "isFullWidth", "width", "height", "title", "description", "onClose", "onSave", "onBack", "onNext", "onGoToStep", "className", "steps", "startAtStep", "nextButtonText", "backButtonText", "cancelButtonText", "ariaLabelCloseButton", "ariaLabelNav", "hasBodyPadding", "footer", "isCompactNav", "appendTo"]);

    const {
      currentStep
    } = this.state;
    const flattenedSteps = this.getFlattenedSteps();
    const adjustedStep = flattenedSteps.length < currentStep ? flattenedSteps.length : currentStep;
    const activeStep = flattenedSteps[adjustedStep - 1];
    const computedSteps = this.initSteps(steps);
    const firstStep = activeStep === flattenedSteps[0];
    const isValid = activeStep && activeStep.enableNext !== undefined ? activeStep.enableNext : true;
    const setFullWidth = isFullWidth || width;
    const setFullHeight = isFullHeight || height;

    const nav = isWizardNavOpen => React.createElement(WizardNav, {
      isOpen: isWizardNavOpen,
      ariaLabel: ariaLabelNav
    }, computedSteps.map((step, index) => {
      if (step.isFinishedStep) {
        // Don't show finished step in the side nav
        return;
      }

      let enabled;
      let navItemStep;

      if (step.steps) {
        let hasActiveChild = false;
        let canJumpToParent = false;

        for (const subStep of step.steps) {
          if (activeStep.name === subStep.name) {
            // one of the children matches
            hasActiveChild = true;
          }

          if (subStep.canJumpTo) {
            canJumpToParent = true;
          }
        }

        navItemStep = this.getFlattenedStepsIndex(flattenedSteps, step.steps[0].name);
        return React.createElement(WizardNavItem, {
          key: index,
          text: step.name,
          isCurrent: hasActiveChild,
          isDisabled: !canJumpToParent,
          step: navItemStep,
          onNavItemClick: this.goToStep
        }, React.createElement(WizardNav, {
          returnList: true
        }, step.steps.map((childStep, indexChild) => {
          if (childStep.isFinishedStep) {
            // Don't show finished step in the side nav
            return;
          }

          navItemStep = this.getFlattenedStepsIndex(flattenedSteps, childStep.name);
          enabled = childStep.canJumpTo;
          return React.createElement(WizardNavItem, {
            key: `child_${indexChild}`,
            text: childStep.name,
            isCurrent: activeStep.name === childStep.name,
            isDisabled: !enabled,
            step: navItemStep,
            onNavItemClick: this.goToStep
          });
        })));
      }

      navItemStep = this.getFlattenedStepsIndex(flattenedSteps, step.name);
      enabled = step.canJumpTo;
      return React.createElement(WizardNavItem, {
        key: index,
        text: step.name,
        isCurrent: activeStep.name === step.name,
        isDisabled: !enabled,
        step: navItemStep,
        onNavItemClick: this.goToStep
      });
    }));

    const context = {
      goToStepById: this.goToStepById,
      goToStepByName: this.goToStepByName,
      onNext: this.onNext,
      onBack: this.onBack,
      onClose,
      activeStep
    };

    if (this.isModal && !isOpen) {
      return null;
    }

    const wizard = React.createElement(WizardContextProvider, {
      value: context
    }, React.createElement("div", _extends({}, rest, {
      className: css(styles.wizard, !this.isModal && styles.modifiers.inPage, isCompactNav && 'pf-m-compact-nav', activeStep.isFinishedStep && 'pf-m-finished', setFullWidth && styles.modifiers.fullWidth, setFullHeight && styles.modifiers.fullHeight, className)
    }, this.isModal && {
      role: 'dialog',
      'aria-modal': 'true',
      'aria-labelledby': this.titleId,
      'aria-describedby': description ? this.descriptionId : undefined
    }), this.isModal && React.createElement(WizardHeader, {
      titleId: this.titleId,
      descriptionId: this.descriptionId,
      onClose: onClose,
      title: title,
      description: description,
      ariaLabelCloseButton: ariaLabelCloseButton
    }), React.createElement(WizardToggle, {
      isNavOpen: this.state.isNavOpen,
      onNavToggle: isNavOpen => this.setState({
        isNavOpen
      }),
      nav: nav,
      steps: steps,
      activeStep: activeStep,
      hasBodyPadding: hasBodyPadding
    }, footer || React.createElement(WizardFooterInternal, {
      onNext: this.onNext,
      onBack: this.onBack,
      onClose: onClose,
      isValid: isValid,
      firstStep: firstStep,
      activeStep: activeStep,
      nextButtonText: activeStep.nextButtonText || nextButtonText,
      backButtonText: backButtonText,
      cancelButtonText: cancelButtonText
    }))));
    return this.isModal ? ReactDOM.createPortal(React.createElement(FocusTrap, {
      focusTrapOptions: {
        clickOutsideDeactivates: true
      }
    }, React.createElement(Backdrop, null, React.createElement(Bullseye, null, wizard))), this.container) : wizard;
  }

}

_defineProperty(Wizard, "propTypes", {
  isOpen: _pt.bool,
  isInPage: _pt.bool,
  isCompactNav: _pt.bool,
  isFullHeight: _pt.bool,
  isFullWidth: _pt.bool,
  width: _pt.oneOfType([_pt.number, _pt.string]),
  height: _pt.oneOfType([_pt.number, _pt.string]),
  title: _pt.string,
  description: _pt.string,
  onClose: _pt.func,
  onGoToStep: _pt.func,
  className: _pt.string,
  steps: _pt.arrayOf(_pt.shape({
    id: _pt.oneOfType([_pt.string, _pt.number]),
    name: _pt.string.isRequired,
    component: _pt.any,
    isFinishedStep: _pt.bool,
    canJumpTo: _pt.bool,
    steps: _pt.arrayOf(_pt.shape({
      id: _pt.oneOfType([_pt.string, _pt.number]),
      name: _pt.string.isRequired,
      component: _pt.any,
      isFinishedStep: _pt.bool,
      canJumpTo: _pt.bool,
      steps: _pt.arrayOf(_pt.shape({
        id: _pt.oneOfType([_pt.string, _pt.number]),
        name: _pt.string.isRequired,
        component: _pt.any,
        isFinishedStep: _pt.bool,
        canJumpTo: _pt.bool,
        steps: _pt.arrayOf(_pt.object),
        nextButtonText: _pt.string,
        enableNext: _pt.bool,
        hideCancelButton: _pt.bool,
        hideBackButton: _pt.bool
      })),
      nextButtonText: _pt.string,
      enableNext: _pt.bool,
      hideCancelButton: _pt.bool,
      hideBackButton: _pt.bool
    })),
    nextButtonText: _pt.string,
    enableNext: _pt.bool,
    hideCancelButton: _pt.bool,
    hideBackButton: _pt.bool
  })).isRequired,
  startAtStep: _pt.number,
  ariaLabelNav: _pt.string,
  hasBodyPadding: _pt.bool,
  footer: _pt.node,
  onSave: _pt.func,
  onNext: _pt.func,
  onBack: _pt.func,
  nextButtonText: _pt.string,
  backButtonText: _pt.string,
  cancelButtonText: _pt.string,
  ariaLabelCloseButton: _pt.string,
  appendTo: _pt.oneOfType([_pt.any, _pt.func])
});

_defineProperty(Wizard, "currentId", 0);

_defineProperty(Wizard, "defaultProps", {
  isOpen: false,
  isInPage: false,
  isCompactNav: false,
  isFullHeight: false,
  isFullWidth: false,
  title: '',
  description: '',
  className: '',
  startAtStep: 1,
  nextButtonText: 'Next',
  backButtonText: 'Back',
  cancelButtonText: 'Cancel',
  ariaLabelCloseButton: 'Close',
  ariaLabelNav: 'Steps',
  hasBodyPadding: true,
  onBack: null,
  onNext: null,
  onGoToStep: null,
  width: null,
  height: null,
  footer: null,
  onClose: () => undefined,
  appendTo: null
});
//# sourceMappingURL=Wizard.js.map