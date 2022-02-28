import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Expandable/expandable';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import { PickOptional } from '../../helpers/typeUtils';

export interface ExpandableProps {
  /** Content rendered inside the Expandable Component */
  children: React.ReactNode;
  /** Additional classes added to the Expandable Component */
  className?: string;
  /** Flag to indicate if the content is expanded */
  isExpanded?: boolean;
  /** Text that appears in the toggle */
  toggleText?: string;
  /** Text that appears in the toggle when expanded (will override toggleText if both are specified; used for uncontrolled expandable with dynamic toggle text) */
  toggleTextExpanded?: string;
  /** Text that appears in the toggle when collapsed (will override toggleText if both are specified; used for uncontrolled expandable with dynamic toggle text) */
  toggleTextCollapsed?: string;
  /** Callback function to toggle the expandable content */
  onToggle?: () => void;
  /** Forces focus state */
  isFocused?: boolean;
  /** Forces hover state */
  isHovered?: boolean;
  /** Forces active state */
  isActive?: boolean;
}

interface ExpandableState {
  isExpanded: boolean;
}

export class Expandable extends React.Component<ExpandableProps, ExpandableState> {
  constructor(props: ExpandableProps) {
    super(props);

    this.state = {
      isExpanded: props.isExpanded
    };
  }

  static defaultProps: PickOptional<ExpandableProps> = {
    className: '',
    toggleText: '',
    toggleTextExpanded: '',
    toggleTextCollapsed: '',
    onToggle: (): any => undefined,
    isFocused: false,
    isActive: false,
    isHovered: false
  };

  private calculateToggleText(
    toggleText: string,
    toggleTextExpanded: string,
    toggleTextCollapsed: string,
    propOrStateIsExpanded: boolean
  ) {
    if (propOrStateIsExpanded && toggleTextExpanded !== '') {
      return toggleTextExpanded;
    }
    if (!propOrStateIsExpanded && toggleTextCollapsed !== '') {
      return toggleTextCollapsed;
    }
    return toggleText;
  }

  render() {
    const {
      onToggle: onToggleProp,
      isFocused,
      isHovered,
      isActive,
      className,
      toggleText,
      toggleTextExpanded,
      toggleTextCollapsed,
      children,
      isExpanded,
      ...props
    } = this.props;
    let onToggle = onToggleProp;
    let propOrStateIsExpanded = isExpanded;

    // uncontrolled
    if (isExpanded === undefined) {
      propOrStateIsExpanded = this.state.isExpanded;
      onToggle = () => {
        onToggleProp();
        this.setState(prevState => ({ isExpanded: !prevState.isExpanded }));
      };
    }

    const computedToggleText = this.calculateToggleText(
      toggleText,
      toggleTextExpanded,
      toggleTextCollapsed,
      propOrStateIsExpanded
    );

    return (
      <div {...props} className={css(styles.expandable, propOrStateIsExpanded && styles.modifiers.expanded, className)}>
        <button
          className={css(
            styles.expandableToggle,
            isFocused && styles.modifiers.focus,
            isHovered && styles.modifiers.hover,
            isActive && styles.modifiers.active
          )}
          type="button"
          aria-expanded={propOrStateIsExpanded}
          onClick={onToggle}
        >
          <AngleRightIcon className={css(styles.expandableToggleIcon)} aria-hidden />
          <span>{computedToggleText}</span>
        </button>
        <div className={css(styles.expandableContent)} hidden={!propOrStateIsExpanded}>
          {children}
        </div>
      </div>
    );
  }
}
