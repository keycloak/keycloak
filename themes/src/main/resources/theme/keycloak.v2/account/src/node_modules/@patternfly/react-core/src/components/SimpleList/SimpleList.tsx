import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SimpleList/simple-list';
import { SimpleListGroup } from './SimpleListGroup';
import { SimpleListItemProps } from './SimpleListItem';

export interface SimpleListProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onSelect'> {
  /** Content rendered inside the SimpleList */
  children?: React.ReactNode;
  /** Additional classes added to the SimpleList container */
  className?: string;
  /** Callback when an item is selected */
  onSelect?: (
    ref: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>,
    props: SimpleListItemProps
  ) => void;
  /** Indicates whether component is controlled by its internal state */
  isControlled?: boolean;
}

export interface SimpleListState {
  /** Ref of the current SimpleListItem */
  currentRef: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>;
}

interface SimpleListContextProps {
  currentRef: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>;
  updateCurrentRef: (
    id: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>,
    props: SimpleListItemProps
  ) => void;
  isControlled: boolean;
}

export const SimpleListContext = React.createContext<Partial<SimpleListContextProps>>({});

export class SimpleList extends React.Component<SimpleListProps, SimpleListState> {
  static displayName = 'SimpleList';
  state = {
    currentRef: null as React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>
  };

  static defaultProps: SimpleListProps = {
    children: null as React.ReactNode,
    className: '',
    isControlled: true
  };

  handleCurrentUpdate = (
    newCurrentRef: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>,
    itemProps: SimpleListItemProps
  ) => {
    this.setState({ currentRef: newCurrentRef });
    const { onSelect } = this.props;
    onSelect && onSelect(newCurrentRef, itemProps);
  };

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { children, className, onSelect, isControlled, ...props } = this.props;

    let isGrouped = false;
    if (children) {
      isGrouped = (React.Children.toArray(children)[0] as React.ReactElement).type === SimpleListGroup;
    }

    return (
      <SimpleListContext.Provider
        value={{
          currentRef: this.state.currentRef,
          updateCurrentRef: this.handleCurrentUpdate,
          isControlled
        }}
      >
        <div className={css(styles.simpleList, className)} {...props}>
          {isGrouped && children}
          {!isGrouped && <ul>{children}</ul>}
        </div>
      </SimpleListContext.Provider>
    );
  }
}
