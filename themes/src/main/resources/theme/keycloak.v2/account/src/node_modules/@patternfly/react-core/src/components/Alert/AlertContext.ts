import * as React from 'react';

interface AlertContext {
  title: React.ReactNode;
  variantLabel?: string;
}

export const AlertContext = React.createContext<AlertContext>(null);
