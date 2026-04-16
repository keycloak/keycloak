import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";

import { ComponentTypeSelect } from "./ComponentTypeSelect";

type ValidatorSelectProps = {
  selectedValidators: string[];
  onChange: (validator: ComponentTypeRepresentation) => void;
};

export const ValidatorSelect = ({
  selectedValidators,
  onChange,
}: ValidatorSelectProps) => (
  <ComponentTypeSelect
    componentType="org.keycloak.validate.Validator"
    selected={selectedValidators}
    onChange={onChange}
    labelKey="validatorType"
    fieldId="validator"
  />
);
