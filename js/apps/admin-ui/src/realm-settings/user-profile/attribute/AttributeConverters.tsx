import { AttributeComponentTypes } from "./AttributeComponentTypes";

export const AttributeConverters = () => (
  <AttributeComponentTypes
    fieldName="converters"
    componentType="org.keycloak.convert.Converter"
    name="Converter"
    i18n={{
      add: "addConverter",
      selectLabel: "converterType",
      empty: "emptyConverters",
      colName: "converterColNames.colName",
      colConfig: "converterColNames.colConfig",
      deleteTitle: "deleteConverterConfirmTitle",
      deleteMessage: "deleteConverterConfirmMsg",
      deleteMessageParam: "converterName",
    }}
  />
);
