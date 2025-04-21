import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Title } from '../Title';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
export const WizardHeader = ({ onClose = () => undefined, title, description, hideClose, closeButtonAriaLabel, titleId, descriptionComponent: Component = 'p', descriptionId }) => (React.createElement("div", { className: css(styles.wizardHeader) },
    !hideClose && (React.createElement(Button, { variant: "plain", className: css(styles.wizardClose), "aria-label": closeButtonAriaLabel, onClick: onClose },
        React.createElement(TimesIcon, { "aria-hidden": "true" }))),
    React.createElement(Title, { headingLevel: "h2", size: "3xl", className: css(styles.wizardTitle), "aria-label": title, id: titleId }, title || React.createElement(React.Fragment, null, "\u00A0")),
    description && (React.createElement(Component, { className: css(styles.wizardDescription), id: descriptionId }, description))));
WizardHeader.displayName = 'WizardHeader';
//# sourceMappingURL=WizardHeader.js.map