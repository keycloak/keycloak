import * as React from 'react';

export const ApplicationLauncherContext = React.createContext({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onFavorite: (itemId: string, isFavorite: boolean) => {}
});
