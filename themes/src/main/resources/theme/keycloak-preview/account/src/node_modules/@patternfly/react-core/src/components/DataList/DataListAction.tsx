import * as React from 'react';
import { css, pickProperties } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { PickOptional } from '../../helpers/typeUtils';

const visibilityModifiers = pickProperties(styles.modifiers, [
  'hidden',
  'hiddenOnSm',
  'hiddenOnMd',
  'hiddenOnLg',
  'hiddenOnXl',
  'hiddenOn_2xl',
  'visibleOnSm',
  'visibleOnMd',
  'visibleOnLg',
  'visibleOnXl',
  'visibleOn_2xl'
]);

// eslint-disable-next-line @typescript-eslint/interface-name-prefix
interface IDataListActionVisibility {
  hidden?: string;
  hiddenOnSm?: string;
  hiddenOnMd?: string;
  hiddenOnLg?: string;
  hiddenOnXl?: string;
  hiddenOn2Xl?: string;
  visibleOnSm?: string;
  visibleOnMd?: string;
  visibleOnLg?: string;
  visibleOnXl?: string;
  visibleOn2Xl?: string;
}

export const DataListActionVisibility: IDataListActionVisibility = Object.keys(visibilityModifiers)
  .map(key => [key.replace('_2xl', '2Xl'), visibilityModifiers[key]])
  .reduce((acc, curr) => ({ ...acc, [curr[0]]: curr[1] }), {});

export interface DataListActionProps extends Omit<React.HTMLProps<HTMLDivElement>, 'children'> {
  /** Content rendered as DataList Action  (e.g <Button> or <Dropdown>) */
  children: React.ReactNode;
  /** Additional classes added to the DataList Action */
  className?: string;
  /** Identify the DataList toggle number */
  id: string;
  /** Adds accessible text to the DataList Action */
  'aria-labelledby': string;
  /** Adds accessible text to the DataList Action */
  'aria-label': string;
}

interface DataListActionState {
  isOpen: boolean;
}

export class DataListAction extends React.Component<DataListActionProps, DataListActionState> {
  static defaultProps: PickOptional<DataListActionProps> = {
    className: ''
  };

  constructor(props: DataListActionProps) {
    super(props);
    this.state = {
      isOpen: false
    };
  }

  onToggle = (isOpen: boolean) => {
    this.setState({ isOpen });
  };

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onSelect = (event: MouseEvent) => {
    this.setState(prevState => ({
      isOpen: !prevState.isOpen
    }));
  };

  render() {
    const {
      children,
      className,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      id,
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
      /* eslint-enable @typescript-eslint/no-unused-vars */
      ...props
    } = this.props;

    return (
      <div className={css(styles.dataListItemAction, className)} {...props}>
        {children}
      </div>
    );
  }
}
