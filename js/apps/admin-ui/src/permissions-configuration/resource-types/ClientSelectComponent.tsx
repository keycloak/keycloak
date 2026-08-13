import { ClientSelect } from "../../components/client/ClientSelect";
import { ComponentProps } from "../../components/dynamic/components";

export const ClientSelectComponent = (props: ComponentProps) => (
  <ClientSelect {...props} clientKey="id" />
);
