import { ProviderInstanceSelect } from "../provider-instances/ProviderInstanceSelect";

import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

export const ProviderInstanceComponent = (props: ComponentProps) => {
  const providerType = props.options
    ?.findLast((option) => option.startsWith("type/"))
    ?.split("/")[1];
  const isMultiSelect = props.options?.some((option) => option == "multi");

  return (
    <ProviderInstanceSelect
      {...props}
      name={convertToName(props.name!)}
      providerType={providerType!}
      multiSelect={isMultiSelect!}
      getDisplayName={(option) =>
        option.config?.["displayName"]?.[0] || option.id!
      }
    />
  );
};
