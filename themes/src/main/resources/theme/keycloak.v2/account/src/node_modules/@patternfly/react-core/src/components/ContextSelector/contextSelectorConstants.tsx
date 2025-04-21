import * as React from 'react';

export const ContextSelectorContext = React.createContext({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onSelect: (event: any, value: React.ReactNode): any => undefined
});
