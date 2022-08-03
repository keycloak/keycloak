import type { ComponentProps } from "./components";
import { ClientSelect } from "../client/ClientSelect";

export const ClientSelectComponent = (props: ComponentProps) => {
  return (
    <ClientSelect
      {...props}
      name={`config.${props.name}`}
      namespace="dynamic"
    />
  );
};
