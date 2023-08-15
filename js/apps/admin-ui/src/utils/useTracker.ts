import { useEffect, useState } from "react";
import { container } from "../main";

const defaultEventName = "default";

export function trackPromise<T>(
  promise: Promise<T>,
  name?: string,
): Promise<T> {
  const emit = () => {
    const event = new Event(name || defaultEventName);
    container?.dispatchEvent(event);
  };
  const handler = (result: T) => {
    emit();
    return result;
  };

  emit();
  return promise.then(handler, handler);
}

export function usePromiseTracker(name: string | undefined = defaultEventName) {
  const [promiseInProgress, setPromiseInProgress] = useState(false);
  useEffect(() => {
    container?.addEventListener(
      name,
      () => setPromiseInProgress(!promiseInProgress),
      false,
    );
  }, [name, promiseInProgress]);

  return { promiseInProgress };
}
