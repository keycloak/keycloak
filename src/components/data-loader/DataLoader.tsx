import React, { useEffect, useState } from "react";
import { Spinner } from "@patternfly/react-core";
import { asyncStateFetch } from "../../context/auth/AdminClient";

type DataLoaderProps<T> = {
  loader: () => Promise<T>;
  deps?: any[];
  children: ((arg: Result<T>) => any) | React.ReactNode;
};

type Result<T> = {
  data: T;
  refresh: () => void;
};

export function DataLoader<T>(props: DataLoaderProps<T>) {
  const [data, setData] = useState<{ result: T } | undefined>(undefined);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  useEffect(() => {
    return asyncStateFetch(
      () => props.loader(),
      (result) => setData({ result })
    );
  }, [key]);

  if (data) {
    if (props.children instanceof Function) {
      return props.children({
        data: data.result,
        refresh: refresh,
      });
    }
    return props.children;
  }
  return (
    <div className="pf-u-text-align-center">
      <Spinner />
    </div>
  );
}
