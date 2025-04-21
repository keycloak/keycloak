import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';
import accessibleStyles from '@patternfly/react-styles/css/utilities/Accessibility/accessibility';
import { ApplicationLauncherIcon } from './ApplicationLauncherIcon';
import { ApplicationLauncherText } from './ApplicationLauncherText';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import { ApplicationLauncherItemContext } from './ApplicationLauncherItemContext';
export const ApplicationLauncherContent = ({ children }) => (React.createElement(ApplicationLauncherItemContext.Consumer, null, ({ isExternal, icon }) => (React.createElement(React.Fragment, null,
    icon && React.createElement(ApplicationLauncherIcon, null, icon),
    icon ? React.createElement(ApplicationLauncherText, null, children) : children,
    isExternal && (React.createElement(React.Fragment, null,
        React.createElement("span", { className: css(styles.appLauncherMenuItemExternalIcon) },
            React.createElement(ExternalLinkAltIcon, null)),
        React.createElement("span", { className: css(accessibleStyles.screenReader) }, "(opens new window)")))))));
ApplicationLauncherContent.displayName = 'ApplicationLauncherContent';
//# sourceMappingURL=ApplicationLauncherContent.js.map