import * as React from 'react';

/** This is a custom React hook in a format suggest by Dan Abramov in a blog post here:
 * https://overreacted.io/making-setinterval-declarative-with-react-hooks/. It allows setInterval to be used
 * declaratively in functional React components.
 */

export function useInterval(callback: () => void, delay: number | null) {
  const savedCallback = React.useRef(() => {});

  React.useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  React.useEffect(() => {
    function tick() {
      savedCallback.current();
    }

    if (delay !== null) {
      const id = setInterval(tick, delay);
      return () => clearInterval(id);
    }
  }, [delay]);
}
