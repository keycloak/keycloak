(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dom", "../../helpers", "../../helpers/constants", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Wizard/wizard", "../Backdrop", "../../layouts/Bullseye", "./WizardHeader", "./WizardFooterInternal", "./WizardToggle", "./WizardNav", "./WizardNavItem", "./WizardContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dom"), require("../../helpers"), require("../../helpers/constants"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Wizard/wizard"), require("../Backdrop"), require("../../layouts/Bullseye"), require("./WizardHeader"), require("./WizardFooterInternal"), require("./WizardToggle"), require("./WizardNav"), require("./WizardNavItem"), require("./WizardContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDom, global.helpers, global.constants, global.reactStyles, global.wizard, global.Backdrop, global.Bullseye, global.WizardHeader, global.WizardFooterInternal, global.WizardToggle, global.WizardNav, global.WizardNavItem, global.WizardContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDom, _helpers, _constants, _reactStyles, _wizard, _Backdrop, _Bullseye, _WizardHeader, _WizardFooterInternal, _WizardToggle, _WizardNav, _WizardNavItem, _WizardContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Wizard = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var ReactDOM = _interopRequireWildcard(_reactDom);

  var _wizard2 = _interopRequireDefault(_wizard);

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

  class Wizard extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "handleKeyClicks", event => {
        if (event.keyCode === _constants.KEY_CODES.ESCAPE_KEY) {
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
        if (!_helpers.canUseDOM) {
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

      const nav = isWizardNavOpen => React.createElement(_WizardNav.WizardNav, {
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
          return React.createElement(_WizardNavItem.WizardNavItem, {
            key: index,
            text: step.name,
            isCurrent: hasActiveChild,
            isDisabled: !canJumpToParent,
            step: navItemStep,
            onNavItemClick: this.goToStep
          }, React.createElement(_WizardNav.WizardNav, {
            returnList: true
          }, step.steps.map((childStep, indexChild) => {
            if (childStep.isFinishedStep) {
              // Don't show finished step in the side nav
              return;
            }

            navItemStep = this.getFlattenedStepsIndex(flattenedSteps, childStep.name);
            enabled = childStep.canJumpTo;
            return React.createElement(_WizardNavItem.WizardNavItem, {
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
        return React.createElement(_WizardNavItem.WizardNavItem, {
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

      const wizard = React.createElement(_WizardContext.WizardContextProvider, {
        value: context
      }, React.createElement("div", _extends({}, rest, {
        className: (0, _reactStyles.css)(_wizard2.default.wizard, !this.isModal && _wizard2.default.modifiers.inPage, isCompactNav && 'pf-m-compact-nav', activeStep.isFinishedStep && 'pf-m-finished', setFullWidth && _wizard2.default.modifiers.fullWidth, setFullHeight && _wizard2.default.modifiers.fullHeight, className)
      }, this.isModal && {
        role: 'dialog',
        'aria-modal': 'true',
        'aria-labelledby': this.titleId,
        'aria-describedby': description ? this.descriptionId : undefined
      }), this.isModal && React.createElement(_WizardHeader.WizardHeader, {
        titleId: this.titleId,
        descriptionId: this.descriptionId,
        onClose: onClose,
        title: title,
        description: description,
        ariaLabelCloseButton: ariaLabelCloseButton
      }), React.createElement(_WizardToggle.WizardToggle, {
        isNavOpen: this.state.isNavOpen,
        onNavToggle: isNavOpen => this.setState({
          isNavOpen
        }),
        nav: nav,
        steps: steps,
        activeStep: activeStep,
        hasBodyPadding: hasBodyPadding
      }, footer || React.createElement(_WizardFooterInternal.WizardFooterInternal, {
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
      return this.isModal ? ReactDOM.createPortal(React.createElement(_helpers.FocusTrap, {
        focusTrapOptions: {
          clickOutsideDeactivates: true
        }
      }, React.createElement(_Backdrop.Backdrop, null, React.createElement(_Bullseye.Bullseye, null, wizard))), this.container) : wizard;
    }

  }

  exports.Wizard = Wizard;

  _defineProperty(Wizard, "propTypes", {
    isOpen: _propTypes2.default.bool,
    isInPage: _propTypes2.default.bool,
    isCompactNav: _propTypes2.default.bool,
    isFullHeight: _propTypes2.default.bool,
    isFullWidth: _propTypes2.default.bool,
    width: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string]),
    height: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string]),
    title: _propTypes2.default.string,
    description: _propTypes2.default.string,
    onClose: _propTypes2.default.func,
    onGoToStep: _propTypes2.default.func,
    className: _propTypes2.default.string,
    steps: _propTypes2.default.arrayOf(_propTypes2.default.shape({
      id: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
      name: _propTypes2.default.string.isRequired,
      component: _propTypes2.default.any,
      isFinishedStep: _propTypes2.default.bool,
      canJumpTo: _propTypes2.default.bool,
      steps: _propTypes2.default.arrayOf(_propTypes2.default.shape({
        id: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
        name: _propTypes2.default.string.isRequired,
        component: _propTypes2.default.any,
        isFinishedStep: _propTypes2.default.bool,
        canJumpTo: _propTypes2.default.bool,
        steps: _propTypes2.default.arrayOf(_propTypes2.default.shape({
          id: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
          name: _propTypes2.default.string.isRequired,
          component: _propTypes2.default.any,
          isFinishedStep: _propTypes2.default.bool,
          canJumpTo: _propTypes2.default.bool,
          steps: _propTypes2.default.arrayOf(_propTypes2.default.object),
          nextButtonText: _propTypes2.default.string,
          enableNext: _propTypes2.default.bool,
          hideCancelButton: _propTypes2.default.bool,
          hideBackButton: _propTypes2.default.bool
        })),
        nextButtonText: _propTypes2.default.string,
        enableNext: _propTypes2.default.bool,
        hideCancelButton: _propTypes2.default.bool,
        hideBackButton: _propTypes2.default.bool
      })),
      nextButtonText: _propTypes2.default.string,
      enableNext: _propTypes2.default.bool,
      hideCancelButton: _propTypes2.default.bool,
      hideBackButton: _propTypes2.default.bool
    })).isRequired,
    startAtStep: _propTypes2.default.number,
    ariaLabelNav: _propTypes2.default.string,
    hasBodyPadding: _propTypes2.default.bool,
    footer: _propTypes2.default.node,
    onSave: _propTypes2.default.func,
    onNext: _propTypes2.default.func,
    onBack: _propTypes2.default.func,
    nextButtonText: _propTypes2.default.string,
    backButtonText: _propTypes2.default.string,
    cancelButtonText: _propTypes2.default.string,
    ariaLabelCloseButton: _propTypes2.default.string,
    appendTo: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.func])
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
});
//# sourceMappingURL=Wizard.js.map