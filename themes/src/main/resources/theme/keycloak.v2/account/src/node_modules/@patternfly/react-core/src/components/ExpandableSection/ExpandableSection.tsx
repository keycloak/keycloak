import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ExpandableSection/expandable-section';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { PickOptional } from '../../helpers/typeUtils';

export interface ExpandableSectionProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Expandable Component */
  children?: React.ReactNode;
  /** Additional classes added to the Expandable Component */
  className?: string;
  /** Flag to indicate if the content is expanded */
  isExpanded?: boolean;
  /** Text that appears in the attached toggle */
  toggleText?: string;
  /** Text that appears in the attached toggle when expanded (will override toggleText if both are specified; used for uncontrolled expandable with dynamic toggle text) */
  toggleTextExpanded?: string;
  /** Text that appears in the attached toggle when collapsed (will override toggleText if both are specified; used for uncontrolled expandable with dynamic toggle text) */
  toggleTextCollapsed?: string;
  /** React node that appears in the attached toggle in place of toggle text */
  toggleContent?: React.ReactNode;
  /** Callback function to toggle the expandable content. Detached expandable sections should use the onToggle property of ExpandableSectionToggle. */
  onToggle?: (isExpanded: boolean) => void;
  /** Forces active state */
  isActive?: boolean;
  /** Indicates the expandable section has a detached toggle */
  isDetached?: boolean;
  /** ID of the content of the expandable section */
  contentId?: string;
  /** Display size variant. Set to large for disclosure styling. */
  displaySize?: 'default' | 'large';
  /** Flag to indicate the width of the component is limited. Set to true for disclosure styling. */
  isWidthLimited?: boolean;
  /** Flag to indicate if the content is indented */
  isIndented?: boolean;
}

interface ExpandableSectionState {
  isExpanded: boolean;
}

export class ExpandableSection extends React.Component<ExpandableSectionProps, ExpandableSectionState> {
  static displayName = 'ExpandableSection';
  constructor(props: ExpandableSectionProps) {
    super(props);

    this.state = {
      isExpanded: props.isExpanded
    };
  }

  static defaultProps: PickOptional<ExpandableSectionProps> = {
    className: '',
    toggleText: '',
    toggleTextExpanded: '',
    toggleTextCollapsed: '',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle: (isExpanded): void => undefined,
    isActive: false,
    isDetached: false,
    displaySize: 'default',
    isWidthLimited: false,
    isIndented: false,
    contentId: ''
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
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      isActive,
      className,
      toggleText,
      toggleTextExpanded,
      toggleTextCollapsed,
      toggleContent,
      children,
      isExpanded,
      isDetached,
      displaySize,
      isWidthLimited,
      isIndented,
      contentId,
      ...props
    } = this.props;
    let onToggle = onToggleProp;
    let propOrStateIsExpanded = isExpanded;

    // uncontrolled
    if (isExpanded === undefined) {
      propOrStateIsExpanded = this.state.isExpanded;
      onToggle = isOpen => {
        this.setState({ isExpanded: isOpen }, () => onToggleProp(this.state.isExpanded));
      };
    }

    const computedToggleText = this.calculateToggleText(
      toggleText,
      toggleTextExpanded,
      toggleTextCollapsed,
      propOrStateIsExpanded
    );

    return (
      <div
        {...props}
        className={css(
          styles.expandableSection,
          propOrStateIsExpanded && styles.modifiers.expanded,
          isActive && styles.modifiers.active,
          isDetached && styles.modifiers.detached,
          displaySize === 'large' && styles.modifiers.displayLg,
          isWidthLimited && styles.modifiers.limitWidth,
          isIndented && styles.modifiers.indented,
          className
        )}
      >
        {!isDetached && (
          <button
            className={css(styles.expandableSectionToggle)}
            type="button"
            aria-expanded={propOrStateIsExpanded}
            onClick={() => onToggle(!propOrStateIsExpanded)}
          >
            <span className={css(styles.expandableSectionToggleIcon)}>
              <AngleRightIcon aria-hidden />
            </span>
            <span className={css(styles.expandableSectionToggleText)}>{toggleContent || computedToggleText}</span>
          </button>
        )}
        <div className={css(styles.expandableSectionContent)} hidden={!propOrStateIsExpanded} id={contentId}>
          {children}
        </div>
      </div>
    );
  }
}
