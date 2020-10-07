import React, { useEffect, useState } from "react";
import { Spinner } from "@patternfly/react-core";

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
  const loadData = async () => {
    const result = await props.loader();
    setData({ result });
  };
  useEffect(() => {
    setData(undefined);
    loadData();
  }, [props]);

  if (data) {
    if (props.children instanceof Function) {
      return props.children({
        data: data.result,
        refresh: () => loadData(),
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
