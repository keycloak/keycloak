import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ChipGroup/chip-group';
import { css } from '@patternfly/react-styles';
import { Chip } from './Chip';
import { fillTemplate } from '../../helpers';

export const ChipGroupContext = React.createContext('');

export interface ChipGroupProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the chip text */
  children?: React.ReactNode;
  /** Additional classes added to the chip item */
  className?: string;
  /** Flag for having the chip group default to expanded */
  defaultIsOpen?: boolean;
  /** Customizable "Show Less" text string */
  expandedText?: string;
  /** Customizeable template string. Use variable "${remaining}" for the overflow chip count. */
  collapsedText?: string;
  /** Flag for grouping with a toolbar & category name */
  withToolbar?: boolean;
  /** Set heading level to the chip item label */
  headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
  /** Set number of chips to show before overflow */
  numChips?: number;
}

interface ChipGroupState {
  isOpen: boolean;
}

export class ChipGroup extends React.Component<ChipGroupProps, ChipGroupState> {
  constructor(props: ChipGroupProps) {
    super(props);
    this.state = {
      isOpen: this.props.defaultIsOpen
    };
  }

  static defaultProps: ChipGroupProps = {
    className: '',
    expandedText: 'Show Less',
    collapsedText: '${remaining} more',
    withToolbar: false,
    defaultIsOpen: false,
    numChips: 3
  };

  toggleCollapse = () => {
    this.setState(prevState => ({
      isOpen: !prevState.isOpen
    }));
  };

  renderToolbarGroup() {
    const { isOpen } = this.state;
    const { headingLevel = 'h4' } = this.props;
    return (
      <ChipGroupContext.Provider value={headingLevel}>
        <InnerChipGroup {...this.props} isOpen={isOpen} onToggleCollapse={this.toggleCollapse} />
      </ChipGroupContext.Provider>
    );
  }

  renderChipGroup() {
    const { className } = this.props;
    const { isOpen } = this.state;
    return (
      <ul className={css(styles.chipGroup, className)}>
        <InnerChipGroup {...this.props} isOpen={isOpen} onToggleCollapse={this.toggleCollapse} />
      </ul>
    );
  }

  render() {
    const { withToolbar, children } = this.props;
    if (React.Children.count(children)) {
      return withToolbar ? this.renderToolbarGroup() : this.renderChipGroup();
    }
    return null;
  }
}

interface InnerChipGroupProps extends ChipGroupProps {
  isOpen: boolean;
  onToggleCollapse: () => void;
}

const InnerChipGroup = (props: InnerChipGroupProps) => {
  const { children, expandedText, isOpen, onToggleCollapse, collapsedText, withToolbar, numChips } = props;

  const collapsedTextResult = fillTemplate(collapsedText as string, {
    remaining: React.Children.count(children) - numChips
  });
  const mappedChildren = React.Children.map(children, c => {
    const child = c as React.ReactElement<any>;
    if (withToolbar) {
      return React.cloneElement(child, {
        children: React.Children.toArray(child.props.children).map((chip: any) =>
          React.cloneElement(chip, { component: 'li' })
        )
      });
    }
    return React.cloneElement(child, {
      component: 'li'
    });
  });
  return (
    <React.Fragment>
      {isOpen ? (
        <React.Fragment>{mappedChildren}</React.Fragment>
      ) : (
        <React.Fragment>
          {mappedChildren.map((child, i) => {
            if (i < numChips) {
              return child;
            }
          })}
        </React.Fragment>
      )}
      {React.Children.count(children) > numChips && (
        <Chip isOverflowChip onClick={onToggleCollapse} component={withToolbar ? 'div' : 'li'}>
          {isOpen ? expandedText : collapsedTextResult}
        </Chip>
      )}
    </React.Fragment>
  );
};
