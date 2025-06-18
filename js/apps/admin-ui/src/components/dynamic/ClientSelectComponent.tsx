import type { ComponentProps } from "./components";
import { ClientSelect } from "../client/ClientSelect";

export const ClientSelectComponent = (props: ComponentProps) => (
  <ClientSelect {...props} name={props.convertToName(props.name!)} />
);
