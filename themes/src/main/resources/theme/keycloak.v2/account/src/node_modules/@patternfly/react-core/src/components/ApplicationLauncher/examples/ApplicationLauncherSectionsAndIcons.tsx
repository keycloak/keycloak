import React from 'react';
import {
  ApplicationLauncher,
  ApplicationLauncherItem,
  ApplicationLauncherGroup,
  ApplicationLauncherSeparator
} from '@patternfly/react-core';
import pfLogoSm from './pf-logo-small.svg';

const icon = <img src={pfLogoSm} />;

const appLauncherItems: React.ReactElement[] = [
  <ApplicationLauncherGroup key="group 1c">
    <ApplicationLauncherItem key="group 1a" icon={icon}>
      Item without group title
    </ApplicationLauncherItem>
    <ApplicationLauncherSeparator key="separator" />
  </ApplicationLauncherGroup>,
  <ApplicationLauncherGroup label="Group 2" key="group 2c">
    <ApplicationLauncherItem key="group 2a" isExternal icon={icon} component="button">
      Group 2 button
    </ApplicationLauncherItem>
    <ApplicationLauncherItem key="group 2b" isExternal href="#" icon={icon}>
      Group 2 anchor link
    </ApplicationLauncherItem>
    <ApplicationLauncherSeparator key="separator" />
  </ApplicationLauncherGroup>,
  <ApplicationLauncherGroup label="Group 3" key="group 3c">
    <ApplicationLauncherItem key="group 3a" isExternal icon={icon} component="button">
      Group 3 button
    </ApplicationLauncherItem>
    <ApplicationLauncherItem key="group 3b" isExternal href="#" icon={icon}>
      Group 3 anchor link
    </ApplicationLauncherItem>
  </ApplicationLauncherGroup>
];

export const ApplicationLauncherSectionsAndIcons: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => setIsOpen(isOpen);
  const onSelect = (_event: any) => setIsOpen(prevIsOpen => !prevIsOpen);

  return (
    <ApplicationLauncher onSelect={onSelect} onToggle={onToggle} isOpen={isOpen} items={appLauncherItems} isGrouped />
  );
};
