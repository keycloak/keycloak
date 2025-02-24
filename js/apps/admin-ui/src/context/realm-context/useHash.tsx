import { useEffect, useState } from "react";

export const useHash = () => {
  const [hash, setHash] = useState(location.hash);

  useEffect(() => {
    const orgPushState = window.history.pushState;
    window.history.pushState = new Proxy(window.history.pushState, {
      apply: (func, target, args) => {
        setHash(args[2].substring(1));
        return Reflect.apply(func, target, args);
      },
    });
    return () => {
      window.history.pushState = orgPushState;
    };
  }, []);
  return decodeURIComponent(hash);
};
