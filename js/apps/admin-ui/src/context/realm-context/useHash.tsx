import { useEffect, useState } from "react";

export const useHash = () => {
  const [hash, setHash] = useState(location.hash);

  useEffect(() => {
    const orgPushState = window.history.pushState;
    window.history.pushState = new Proxy(window.history.pushState, {
      apply: (func, target, args) => {
        const url = new URL(args[2], window.location.origin);
        setHash(url.hash.substring(1));
        return Reflect.apply(func, target, args);
      },
    });
    return () => {
      window.history.pushState = orgPushState;
    };
  }, []);
  return decodeURIComponent(hash);
};
