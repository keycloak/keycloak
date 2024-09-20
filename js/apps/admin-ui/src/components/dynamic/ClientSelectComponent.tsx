import type { ComponentProps } from "./components";
import { ClientSelect } from "../client/ClientSelect";
import { convertToName } from "./DynamicComponents";

export const ClientSelectComponent = (props: ComponentProps) => (
  <ClientSelect {...props} name={convertToName(props.name!)} />
);
