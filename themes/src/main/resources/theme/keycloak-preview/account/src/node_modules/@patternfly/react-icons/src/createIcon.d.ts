import { IconProps } from './common';

export interface IconDefinition {
  height: number;
  name: string;
  svgPath: string;
  width: number;
}

export type IconType = React.SFC<IconProps>;

declare const createIcon: (definition: IconDefinition) => IconType;
