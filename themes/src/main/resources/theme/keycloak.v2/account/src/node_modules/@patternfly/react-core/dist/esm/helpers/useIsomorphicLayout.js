import * as React from 'react';
import { canUseDOM } from './util';
/**
 * Small wrapper around `useLayoutEffect` to get rid of the warning on SSR envs
 */
export const useIsomorphicLayoutEffect = canUseDOM ? React.useLayoutEffect : React.useEffect;
//# sourceMappingURL=useIsomorphicLayout.js.map