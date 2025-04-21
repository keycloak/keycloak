import * as React from 'react';

export const DualListSelectorContext = React.createContext<{
  isTree?: boolean;
}>({ isTree: false });

export const DualListSelectorListContext = React.createContext<{
  setFocusedOption?: (id: string) => void;
  isTree?: boolean;
  ariaLabelledBy?: string;
  focusedOption?: string;
  displayOption?: (option: React.ReactNode) => boolean;
  selectedOptions?: string[] | number[];
  id?: string;
  onOptionSelect?: (e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent, index: number, id: string) => void;
  options?: React.ReactNode[];
  isDisabled?: boolean;
}>({});

export const DualListSelectorPaneContext = React.createContext<{
  isChosen: boolean;
}>({ isChosen: false });
