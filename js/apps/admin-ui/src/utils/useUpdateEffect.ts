import { DependencyList, EffectCallback, useEffect, useRef } from "react";

/**
 * A `useEffect` hook that only triggers on updates, not on initial render.
 */
export function useUpdateEffect(effect: EffectCallback, deps?: DependencyList) {
  const didMount = useRef(false);

  useEffect(() => {
    if (didMount.current) {
      return effect();
    }

    didMount.current = true;
  }, deps);
}
