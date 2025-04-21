import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';

export interface ContextSelectorMenuListProps {
  /** Content rendered inside the Context Selector Menu */
  children?: React.ReactNode;
  /** Classess applied to root element of Context Selector menu */
  className?: string;
  /** Flag to indicate if Context Selector menu is opened */
  isOpen?: boolean;
}

export class ContextSelectorMenuList extends React.Component<ContextSelectorMenuListProps> {
  static displayName = 'ContextSelectorMenuList';
  static defaultProps: ContextSelectorMenuListProps = {
    children: null as React.ReactNode,
    className: '',
    isOpen: true
  };

  refsCollection = [] as any;

  sendRef = (index: number, ref: any) => {
    this.refsCollection[index] = ref;
  };

  extendChildren() {
    return React.Children.map(this.props.children, (child, index) =>
      React.cloneElement(child as React.ReactElement<any>, {
        sendRef: this.sendRef,
        index
      })
    );
  }

  render = () => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { className, isOpen, children, ...props } = this.props;
    return (
      <ul className={css(styles.contextSelectorMenuList, className)} hidden={!isOpen} role="menu" {...props}>
        {this.extendChildren()}
      </ul>
    );
  };
}
