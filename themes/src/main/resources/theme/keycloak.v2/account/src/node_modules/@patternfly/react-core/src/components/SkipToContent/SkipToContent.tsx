import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/SkipToContent/skip-to-content';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import { PickOptional } from '../../helpers/typeUtils';

export interface SkipToContentProps extends React.HTMLProps<HTMLAnchorElement> {
  /** The skip to content link. */
  href: string;
  /** Content to display within the skip to content component, typically a string. */
  children?: React.ReactNode;
  /** Additional styles to apply to the skip to content component. */
  className?: string;
  /** Forces the skip to content to display. This is primarily for demonstration purposes and would not normally be used. */
  show?: boolean;
}

export class SkipToContent extends React.Component<SkipToContentProps> {
  static displayName = 'SkipToContent';
  static defaultProps: PickOptional<SkipToContentProps> = {
    show: false
  };
  componentRef = React.createRef<HTMLAnchorElement>();

  componentDidMount() {
    if (this.props.show && this.componentRef.current) {
      this.componentRef.current.focus();
    }
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { children, className, href, show, type, ...rest } = this.props;
    return (
      <a
        {...rest}
        className={css(buttonStyles.button, buttonStyles.modifiers.primary, styles.skipToContent, className)}
        ref={this.componentRef}
        href={href}
      >
        {children}
      </a>
    );
  }
}
