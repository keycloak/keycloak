import * as React from 'react';

export enum IconSize {
  sm = 'sm',
  md = 'md',
  lg = 'lg',
  xl = 'xl'
}

export const getSize = (size: IconSize | keyof typeof IconSize) => {
  switch (size) {
    case IconSize.sm:
      return '1em';
    case IconSize.md:
      return '1.5em';
    case IconSize.lg:
      return '2em';
    case IconSize.xl:
      return '3em';
    default:
      return '1em';
  }
};

export interface IconDefinition {
  name?: string;
  width: number;
  height: number;
  svgPath: string;
  xOffset?: number;
  yOffset?: number;
}

export interface SVGIconProps extends Omit<React.HTMLProps<SVGElement>, 'size' | 'ref'> {
  color?: string;
  size?: IconSize | keyof typeof IconSize;
  title?: string;
  noVerticalAlign?: boolean;
}

let currentId = 0;

/**
 * Factory to create Icon class components for consumers
 */
export function createIcon({
  name,
  xOffset = 0,
  yOffset = 0,
  width,
  height,
  svgPath
}: IconDefinition): React.ComponentClass<SVGIconProps> {
  return class SVGIcon extends React.Component<SVGIconProps> {
    static displayName = name;
    static defaultProps = {
      color: 'currentColor',
      size: IconSize.sm,
      noVerticalAlign: false
    };

    id = `icon-title-${currentId++}`;

    render() {
      const { size, color, title, noVerticalAlign, ...props } = this.props;

      const hasTitle = Boolean(title);
      const heightWidth = getSize(size);
      const baseAlign = -0.125 * Number.parseFloat(heightWidth);
      const style = noVerticalAlign ? null : { verticalAlign: `${baseAlign}em` };
      const viewBox = [xOffset, yOffset, width, height].join(' ');

      return (
        <svg
          style={style}
          fill={color}
          height={heightWidth}
          width={heightWidth}
          viewBox={viewBox}
          aria-labelledby={hasTitle ? this.id : null}
          aria-hidden={hasTitle ? null : true}
          role="img"
          {...(props as Omit<React.SVGProps<SVGElement>, 'ref'>)} // Lie.
        >
          {hasTitle && <title id={this.id}>{title}</title>}
          <path d={svgPath} />
        </svg>
      );
    }
  };
}
