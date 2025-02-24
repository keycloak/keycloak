import { useEffect, useState } from "react";

export const useHash = () => {
  const [hash, setHash] = useState(location.hash);

  useEffect(() => {
    const orgPushState = window.history.pushState;
    window.history.pushState = new Proxy(window.history.pushState, {
      apply: (func, target, args) => {
        const hash = new URL(args[2]).hash;
        if (hash && document.getElementById(hash)) {
          setTimeout(() => {
            document
              .getElementById(hash)
              ?.scrollIntoView({ behavior: "smooth", block: "start" });
          }, 100);
        } else {
          setHash(hash);
        }
        return Reflect.apply(func, target, args);
      },
    });
    return () => {
      window.history.pushState = orgPushState;
    };
  }, []);
  return hash;
};
