import React from 'react';
import { Alert, AlertGroup, AlertVariant, ToggleGroup, ToggleGroupItem } from '@patternfly/react-core';

interface AlertInfo {
  title: string;
  variant: AlertVariant;
  key: number;
}

export const AsyncLiveRegionAlert: React.FunctionComponent = () => {
  const [alerts, setAlerts] = React.useState<AlertInfo[]>([]);
  const [isActive, setIsActive] = React.useState(false);
  const getUniqueId: () => number = () => new Date().getTime();

  const addAlert = (alertInfo: AlertInfo) => {
    setAlerts(prevAlertInfo => [...prevAlertInfo, alertInfo]);
  };

  React.useEffect(() => {
    let timer = null;
    if (isActive) {
      timer = setInterval(() => {
        addAlert({
          title: `Async alert number ${alerts.length + 1}`,
          variant: AlertVariant.info,
          key: getUniqueId()
        });
      }, 4500);
    } else {
      clearInterval(timer);
    }

    return () => {
      clearInterval(timer);
    };
  }, [isActive, alerts]);

  return (
    <React.Fragment>
      <ToggleGroup aria-label="Toggle asynchronous alerts">
        <ToggleGroupItem
          text="Async alerts on"
          buttonId="async-alerts-on"
          isSelected={isActive}
          onChange={() => setIsActive(true)}
        />
        <ToggleGroupItem
          text="Async alerts off"
          buttonId="async-alerts-off"
          isSelected={!isActive}
          onChange={() => setIsActive(false)}
        />
      </ToggleGroup>
      <AlertGroup isLiveRegion>
        {alerts.map(({ title, variant, key }) => (
          <Alert variant={variant} title={title} key={key} />
        ))}
      </AlertGroup>
    </React.Fragment>
  );
};
