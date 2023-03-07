import { useEffect, useRef } from "react";

export default function useSetTimeout() {
  const didUnmountRef = useRef(false);
  const { current: scheduledTimers } = useRef(new Set<number>());

  useEffect(
    () => () => {
      didUnmountRef.current = true;
      clearAll();
    },
    []
  );

  function clearAll() {
    scheduledTimers.forEach((timer) => clearTimeout(timer));
    scheduledTimers.clear();
  }

  return function scheduleTimeout(callback: () => void, delay: number) {
    if (didUnmountRef.current) {
      throw new Error("Can't schedule a timeout on an unmounted component.");
    }

    const timer = Number(setTimeout(handleCallback, delay));

    scheduledTimers.add(timer);

    function handleCallback() {
      scheduledTimers.delete(timer);
      callback();
    }

    return function cancelTimeout() {
      clearTimeout(timer);
      scheduledTimers.delete(timer);
    };
  };
}
