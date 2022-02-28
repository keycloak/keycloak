import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';

import { fillTemplate } from '../../helpers';
import { ToggleTemplateProps } from './ToggleTemplate';
import { DropdownToggle } from '../Dropdown';

export interface OptionsToggleProps extends React.HTMLProps<HTMLDivElement> {
  /** The type or title of the items being paginated */
  itemsTitle?: string;
  /** The text to be displayed on the Options Toggle */
  optionsToggle?: string;
  /** The Title of the Pagination Options Menu */
  itemsPerPageTitle?: string;
  /** The first index of the items being paginated */
  firstIndex?: number;
  /** The last index of the items being paginated */
  lastIndex?: number;
  /** The total number of items being paginated */
  itemCount?: number;
  /** Id added to the title of the Pagination Options Menu */
  widgetId?: string;
  /** showToggle */
  showToggle?: boolean;
  /** Event function that fires when user clicks the Options Menu toggle */
  onToggle?: (isOpen: boolean) => void;
  /** Flag indicating if the Options Menu dropdown is open or not */
  isOpen?: boolean;
  /** Flag indicating if the Options Menu is disabled */
  isDisabled?: boolean;
  /** */
  parentRef?: HTMLElement;
  /** This will be shown in pagination toggle span. You can use firstIndex, lastIndex, itemCount, itemsTitle props. */
  toggleTemplate?: ((props: ToggleTemplateProps) => React.ReactElement) | string;
  /** Callback for toggle open on keyboard entry */
  onEnter?: () => void;
}

let toggleId = 0;
export const OptionsToggle: React.FunctionComponent<OptionsToggleProps> = ({
  itemsTitle = 'items',
  optionsToggle = 'Select',
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  itemsPerPageTitle = 'Items per page',
  firstIndex = 0,
  lastIndex = 0,
  itemCount = 0,
  widgetId = '',
  showToggle = true,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onToggle = (_isOpen: boolean) => undefined as any,
  isOpen = false,
  isDisabled = false,
  parentRef = null,
  toggleTemplate: ToggleTemplate = '',
  onEnter = null
}: OptionsToggleProps) => (
  <div
    className={css(
      styles.optionsMenuToggle,
      isDisabled && styles.modifiers.disabled,
      styles.modifiers.plain,
      styles.modifiers.text
    )}
  >
    {showToggle && (
      <React.Fragment>
        <span className={css(styles.optionsMenuToggleText)}>
          {typeof ToggleTemplate === 'string' ? (
            fillTemplate(ToggleTemplate, { firstIndex, lastIndex, itemCount, itemsTitle })
          ) : (
            <ToggleTemplate
              firstIndex={firstIndex}
              lastIndex={lastIndex}
              itemCount={itemCount}
              itemsTitle={itemsTitle}
            />
          )}
        </span>
        <DropdownToggle
          onEnter={onEnter}
          aria-label={optionsToggle}
          onToggle={onToggle}
          isDisabled={isDisabled || itemCount <= 0}
          isOpen={isOpen}
          id={`${widgetId}-toggle-${toggleId++}`}
          className={styles.optionsMenuToggleButton}
          parentRef={parentRef}
        ></DropdownToggle>
      </React.Fragment>
    )}
  </div>
);
