import React, { DependencyList, useState } from "react";
import { Spinner } from "@patternfly/react-core";

import { useFetch } from "../../context/auth/AdminClient";

type DataLoaderProps<T> = {
  loader: () => Promise<T>;
  deps?: DependencyList;
  children: ((arg: T) => any) | React.ReactNode;
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
  return (
    <div className="pf-u-text-align-center">
      <Spinner />
    </div>
  );
}
