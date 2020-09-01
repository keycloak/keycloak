import React, { useEffect, useState } from "react";
import { Spinner } from "@patternfly/react-core";

type DataLoaderProps<T> = {
  loader: () => Promise<T>;
  deps?: any[];
  children: ((arg: T) => any) | React.ReactNode;
};

export function DataLoader<T>(props: DataLoaderProps<T>) {
  const [data, setData] = useState<{ result: T } | undefined>(undefined);
  useEffect(() => {
    setData(undefined);
    const loadData = async () => {
      const result = await props.loader();
      setData({ result });
    };

    loadData();
  }, [props]);

  if (data) {
    if (props.children instanceof Function) {
      return props.children(data.result);
    }
    return props.children;
  }
  return (
    <div style={{ textAlign: "center" }}>
      <Spinner />
    </div>
  );
}
