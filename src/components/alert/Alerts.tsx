import React, {
  useState,
  FunctionComponent,
  useContext,
  Dispatch,
  SetStateAction,
} from 'react';
import { AlertType } from './AlertPanel';
import { AlertVariant } from '@patternfly/react-core';

const AlertsContext = React.createContext<
  [AlertType[], Dispatch<SetStateAction<AlertType[]>>] | undefined
>(undefined);

export function withAlerts(WrappedComponent: FunctionComponent) {
  return function (props: any) {
    const state = useState<AlertType[]>([]);

    return (
      <AlertsContext.Provider value={state}>
        <WrappedComponent {...props} />
      </AlertsContext.Provider>
    );
  };
}

export function useAlerts(): [
  AlertType[],
  (message: string, type: AlertVariant) => void,
  (key: number) => void
] {
  const createId = () => new Date().getTime();
  const [alerts, setAlerts] = useContext(AlertsContext)!;

  const hideAlert = (key: number) => {
    setAlerts([...alerts.filter((el) => el.key !== key)]);
  };

  const add = (message: string, type: AlertVariant) => {
    const key = createId();
    setAlerts([...alerts, { key, message, variant: type }]);
    if (type !== AlertVariant.danger) {
      setTimeout(() => hideAlert(key), 8000);
    }
  };

  return [alerts, add, hideAlert];
}
