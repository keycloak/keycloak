import { ChipGroupProps, SelectProps } from "@patternfly/react-core";
import { SingleSelect } from "./SingleSelect";
import { TypeaheadSelect } from "./TypeaheadSelect";

export type Variant = `${SelectVariant}`;

export enum SelectVariant {
  single = "single",
  typeahead = "typeahead",
  typeaheadMulti = "typeaheadMulti",
}

export const propertyToString = (prop: string | number | undefined) =>
  typeof prop === "number" ? prop + "px" : prop;

export type KeycloakSelectProps<> = Omit<
  SelectProps,
  "name" | "toggle" | "selected" | "onClick" | "onSelect" | "variant"
> & {
  toggleId?: string;
  onFilter?: (value: string) => void;
  onClear?: () => void;
  variant?: Variant;
  isDisabled?: boolean;
  menuAppendTo?: string;
  maxHeight?: string | number;
  width?: string | number;
  toggleIcon?: React.ReactElement;
  direction?: "up" | "down";
  placeholderText?: string;
  onSelect?: (value: string | number | object) => void;
  onToggle: (val: boolean) => void;
  selections?: string | string[] | number | number[];
  validated?: "success" | "warning" | "error" | "default";
  typeAheadAriaLabel?: string;
  chipGroupProps?: Omit<ChipGroupProps, "children" | "ref">;
  chipGroupComponent?: React.ReactNode;
  footer?: React.ReactNode;
};
export const KeycloakSelect = ({
  variant = SelectVariant.single,
  ...rest
}: KeycloakSelectProps) => {
  if (variant === SelectVariant.single) {
    return <SingleSelect {...rest} />;
  } else {
    return <TypeaheadSelect {...rest} variant={variant} />;
  }
};
