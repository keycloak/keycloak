import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';
import { WizardBody } from './WizardBody';
export const WizardToggle = ({
  isNavOpen,
  onNavToggle,
  nav,
  steps,
  activeStep,
  children,
  hasBodyPadding = true
}) => {
  let activeStepIndex;
  let activeStepName;
  let activeStepSubName;

  for (let i = 0; i < steps.length; i++) {
    if (activeStep.id && steps[i].id === activeStep.id || steps[i].name === activeStep.name) {
      activeStepIndex = i + 1;
      activeStepName = steps[i].name;
      break;
    } else if (steps[i].steps) {
      for (const step of steps[i].steps) {
        if (activeStep.id && step.id === activeStep.id || step.name === activeStep.name) {
          activeStepIndex = i + 1;
          activeStepName = steps[i].name;
          activeStepSubName = step.name;
          break;
        }
      }
    }
  }

  return React.createElement(React.Fragment, null, React.createElement("button", {
    onClick: () => onNavToggle(!isNavOpen),
    className: css(styles.wizardToggle, isNavOpen && 'pf-m-expanded'),
    "aria-expanded": isNavOpen
  }, React.createElement("ol", {
    className: css(styles.wizardToggleList)
  }, React.createElement("li", {
    className: css(styles.wizardToggleListItem)
  }, React.createElement("span", {
    className: css(styles.wizardToggleNum)
  }, activeStepIndex), " ", activeStepName, activeStepSubName && React.createElement(AngleRightIcon, {
    className: css(styles.wizardToggleSeparator),
    "aria-hidden": "true"
  })), activeStepSubName && React.createElement("li", {
    className: css(styles.wizardToggleListItem)
  }, activeStepSubName)), React.createElement(CaretDownIcon, {
    className: css(styles.wizardToggleIcon),
    "aria-hidden": "true"
  })), React.createElement("div", {
    className: css(styles.wizardOuterWrap)
  }, React.createElement("div", {
    className: css(styles.wizardInnerWrap)
  }, nav(isNavOpen), React.createElement(WizardBody, {
    hasBodyPadding: hasBodyPadding
  }, activeStep.component)), children));
};
WizardToggle.propTypes = {
  nav: _pt.func.isRequired,
  steps: _pt.arrayOf(_pt.any).isRequired,
  activeStep: _pt.any.isRequired,
  children: _pt.node.isRequired,
  hasBodyPadding: _pt.bool.isRequired,
  isNavOpen: _pt.bool.isRequired,
  onNavToggle: _pt.func.isRequired
};
//# sourceMappingURL=WizardToggle.js.map