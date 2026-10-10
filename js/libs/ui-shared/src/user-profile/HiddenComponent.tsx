import { UserProfileFieldProps } from "./UserProfileFields";
import { fieldName } from "./utils";

export const HiddenComponent = ({ form, attribute }: UserProfileFieldProps) => (
  <input
    type="hidden"
    defaultValue={attribute.defaultValue}
    {...form.register(fieldName(attribute.name))}
  />
);
