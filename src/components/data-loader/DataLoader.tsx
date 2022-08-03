import { DependencyList, ReactNode, useState } from "react";

import { useFetch } from "../../context/auth/AdminClient";
import { KeycloakSpinner } from "../keycloak-spinner/KeycloakSpinner";

type DataLoaderProps<T> = {
  loader: () => Promise<T>;
  deps?: DependencyList;
  children: ((arg: T) => any) | ReactNode;
};

export function DataLoader<T>(props: DataLoaderProps<T>) {
  const [data, setData] = useState<T | undefined>();

  useFetch(
    () => props.loader(),
    (result) => setData(result),
    props.deps || []
  );

  if (data) {
    if (props.children instanceof Function) {
      return props.children(data);
    }
    return props.children;
  }
  return <KeycloakSpinner />;
}
