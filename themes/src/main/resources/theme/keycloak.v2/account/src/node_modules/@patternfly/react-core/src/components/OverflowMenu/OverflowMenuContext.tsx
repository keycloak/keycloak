import * as React from 'react';

export const OverflowMenuContext = React.createContext<{
  isBelowBreakpoint?: boolean;
}>({
  isBelowBreakpoint: false
});
