import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';
import accessibleStyles from '@patternfly/react-styles/css/utilities/Accessibility/accessibility';
import { ApplicationLauncherIcon } from './ApplicationLauncherIcon';
import { ApplicationLauncherText } from './ApplicationLauncherText';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import { ApplicationLauncherItemContext } from './ApplicationLauncherItemContext';

export interface ApplicationLauncherContentProps {
  /** Main content to be rendered */
  children: React.ReactNode;
}

export const ApplicationLauncherContent: React.FunctionComponent<ApplicationLauncherContentProps> = ({
  children
}: ApplicationLauncherContentProps) => (
  <ApplicationLauncherItemContext.Consumer>
    {({ isExternal, icon }) => (
      <>
        {icon && <ApplicationLauncherIcon>{icon}</ApplicationLauncherIcon>}
        {icon ? <ApplicationLauncherText>{children}</ApplicationLauncherText> : children}
        {isExternal && (
          <>
            <span className={css(styles.appLauncherMenuItemExternalIcon)}>
              <ExternalLinkAltIcon />
            </span>
            <span className={css(accessibleStyles.screenReader)}>(opens new window)</span>
          </>
        )}
      </>
    )}
  </ApplicationLauncherItemContext.Consumer>
);
ApplicationLauncherContent.displayName = 'ApplicationLauncherContent';
