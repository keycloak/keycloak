import _pt from "prop-types";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Title } from '../Title';
import TimesIcon from '@patternfly/react-icons/dist/js/icons/times-icon';
export const WizardHeader = ({
  onClose = () => undefined,
  title,
  description,
  ariaLabelCloseButton,
  titleId,
  descriptionId
}) => React.createElement("div", {
  className: css(styles.wizardHeader)
}, React.createElement(Button, {
  variant: "plain",
  className: css(styles.wizardClose),
  "aria-label": ariaLabelCloseButton,
  onClick: onClose
}, React.createElement(TimesIcon, {
  "aria-hidden": "true"
})), React.createElement(Title, {
  size: "3xl",
  className: css(styles.wizardTitle),
  "aria-label": title,
  id: titleId
}, title || React.createElement(React.Fragment, null, "\xA0")), description && React.createElement("p", {
  className: css(styles.wizardDescription),
  id: descriptionId
}, description));
WizardHeader.propTypes = {
  onClose: _pt.func,
  title: _pt.string.isRequired,
  description: _pt.string,
  ariaLabelCloseButton: _pt.string,
  titleId: _pt.string,
  descriptionId: _pt.string
};
//# sourceMappingURL=WizardHeader.js.map