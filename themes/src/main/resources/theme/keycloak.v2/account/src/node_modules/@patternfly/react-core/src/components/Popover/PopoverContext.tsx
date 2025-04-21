import * as React from 'react';

interface PopoverContextProps {
  headerComponent?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}

export const PopoverContext = React.createContext<Partial<PopoverContextProps>>({});
