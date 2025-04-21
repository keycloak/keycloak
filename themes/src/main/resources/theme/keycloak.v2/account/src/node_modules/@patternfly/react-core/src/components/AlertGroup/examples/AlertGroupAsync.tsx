import React from 'react';
import {
  Alert,
  AlertProps,
  AlertGroup,
  AlertActionCloseButton,
  AlertVariant,
  InputGroup,
  useInterval
} from '@patternfly/react-core';

export const AlertGroupAsync: React.FunctionComponent = () => {
  const [alerts, setAlerts] = React.useState<Partial<AlertProps>[]>([]);
  const [isRunning, setIsRunning] = React.useState(false);

  const btnClasses = ['pf-c-button', 'pf-m-secondary'].join(' ');

  const getUniqueId = () => new Date().getTime();

  const addAlert = () => {
    setAlerts(prevAlerts => [
      ...prevAlerts,
      {
        title: `Async notification ${prevAlerts.length + 1} was added to the queue.`,
        variant: 'danger',
        key: getUniqueId()
      }
    ]);
  };

  const removeAlert = (key: React.Key) => {
    setAlerts(prevAlerts => [...prevAlerts.filter(alert => alert.key !== key)]);
  };

  const startAsyncAlerts = () => {
    setIsRunning(true);
  };

  const stopAsyncAlerts = () => {
    setIsRunning(false);
  };

  useInterval(addAlert, isRunning ? 4500 : null);

  return (
    <React.Fragment>
      <InputGroup style={{ marginBottom: '16px' }}>
        <button onClick={startAsyncAlerts} type="button" className={btnClasses}>
          Start async alerts
        </button>
        <button onClick={stopAsyncAlerts} type="button" className={btnClasses}>
          Stop async alerts
        </button>
      </InputGroup>
      <AlertGroup isToast isLiveRegion aria-live="assertive">
        {alerts.map(({ title, variant, key }) => (
          <Alert
            variant={AlertVariant[variant]}
            title={title}
            key={key}
            actionClose={
              <AlertActionCloseButton
                title={title as string}
                variantLabel={`${variant} alert`}
                onClose={() => removeAlert(key)}
              />
            }
          />
        ))}
      </AlertGroup>
    </React.Fragment>
  );
};
