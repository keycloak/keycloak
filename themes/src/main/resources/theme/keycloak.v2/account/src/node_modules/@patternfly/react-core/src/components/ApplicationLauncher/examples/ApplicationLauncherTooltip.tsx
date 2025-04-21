import React from 'react';
import { ApplicationLauncher, ApplicationLauncherItem } from '@patternfly/react-core';

const appLauncherItems: React.ReactElement[] = [
  <ApplicationLauncherItem key="application_1b" href="#" tooltip={<div>Launch Application 1</div>}>
    Application 1 (anchor link)
  </ApplicationLauncherItem>,
  <ApplicationLauncherItem
    key="application_2b"
    component="button"
    tooltip={<div>Launch Application 2</div>}
    tooltipProps={{ position: 'right' }}
    onClick={() => alert('Clicked item 2')}
  >
    Application 2 (onClick)
  </ApplicationLauncherItem>,
  <ApplicationLauncherItem
    key="application_3b"
    component="button"
    tooltip={<div>Launch Application 3</div>}
    tooltipProps={{ position: 'bottom' }}
    onClick={() => alert('Clicked item 3')}
  >
    Application 3 (onClick)
  </ApplicationLauncherItem>
];

export const ApplicationLauncherTooltip: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => setIsOpen(isOpen);
  const onSelect = (_event: any) => setIsOpen(prevIsOpen => !prevIsOpen);

  return <ApplicationLauncher onSelect={onSelect} onToggle={onToggle} isOpen={isOpen} items={appLauncherItems} />;
};
