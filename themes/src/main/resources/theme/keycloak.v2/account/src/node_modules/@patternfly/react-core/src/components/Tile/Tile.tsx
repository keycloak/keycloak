import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tile/tile';
import { css } from '@patternfly/react-styles';

export interface TileProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the banner */
  children?: React.ReactNode;
  /** Additional classes added to the banner */
  className?: string;
  /** Title of the tile */
  title: string;
  /** Icon in the tile title */
  icon?: React.ReactNode;
  /** Flag indicating if the tile is selected */
  isSelected?: boolean;
  /** Flag indicating if the tile is disabled */
  isDisabled?: boolean;
  /** Flag indicating if the tile header is stacked */
  isStacked?: boolean;
  /** Flag indicating if the stacked tile icon is large */
  isDisplayLarge?: boolean;
}

export const Tile: React.FunctionComponent<TileProps> = ({
  children,
  title,
  icon,
  isStacked,
  isSelected,
  isDisabled,
  isDisplayLarge,
  className,
  ...props
}: TileProps) => (
  <div
    role="option"
    aria-selected={isSelected}
    {...(isDisabled && { 'aria-disabled': isDisabled })}
    className={css(
      styles.tile,
      isSelected && styles.modifiers.selected,
      isDisabled && styles.modifiers.disabled,
      isDisplayLarge && styles.modifiers.displayLg,
      className
    )}
    tabIndex={0}
    {...props}
  >
    <div className={css(styles.tileHeader, isStacked && styles.modifiers.stacked)}>
      {icon && <div className={css(styles.tileIcon)}>{icon}</div>}
      <div className={css(styles.tileTitle)}>{title}</div>
    </div>
    {children && <div className={css(styles.tileBody)}>{children}</div>}
  </div>
);
Tile.displayName = 'Tile';
