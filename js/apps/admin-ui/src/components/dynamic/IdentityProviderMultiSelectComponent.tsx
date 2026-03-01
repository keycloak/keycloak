import type { ComponentProps } from "./components";
import { IdentityProviderSelect } from "../identity-provider/IdentityProviderSelect";

export const IdentityProviderMultiSelectComponent = (props: ComponentProps) => (
  <IdentityProviderSelect
    {...props}
    convertToName={props.convertToName}
    name={props.name!}
  />
);
