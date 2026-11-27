import { useFormContext } from "react-hook-form";

import { DefaultValue } from "../../../components/key-value-form/KeyValueInput";
import { AttributeComponentTypes } from "./AttributeComponentTypes";

export const AttributeValidations = () => {
  const { getValues, setValue } = useFormContext();

  return (
    <AttributeComponentTypes
      fieldName="validations"
      componentType="org.keycloak.validate.Validator"
      name="Validator"
      i18n={{
        add: "addValidator",
        selectLabel: "validatorType",
        empty: "emptyValidators",
        colName: "validatorColNames.colName",
        colConfig: "validatorColNames.colConfig",
        deleteTitle: "deleteValidatorConfirmTitle",
        deleteMessage: "deleteValidatorConfirmMsg",
        deleteMessageParam: "validatorName",
      }}
      onAdd={(newValidator) => {
        const annotations: DefaultValue[] = getValues("annotations");
        if (
          newValidator.id === "options" &&
          !annotations.find((a) => a.key === "inputType")
        ) {
          setValue("annotations", [
            ...annotations,
            { key: "inputType", value: "select" },
          ]);
        }
      }}
    />
  );
};
