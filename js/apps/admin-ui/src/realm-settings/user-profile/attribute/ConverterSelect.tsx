import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { KeycloakSelect } from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import useToggle from "../../../utils/useToggle";

type ConverterSelectProps = {
  selectedConverters: string[];
  onChange: (converter: ComponentTypeRepresentation) => void;
};

export const ConverterSelect = ({
  selectedConverters,
  onChange,
}: ConverterSelectProps) => {
  const { t } = useTranslation();
  const allConverters: ComponentTypeRepresentation[] =
    useServerInfo().componentTypes?.["org.keycloak.convert.Converter"] || [];
  const converters = useMemo(
    () => allConverters.filter(({ id }) => !selectedConverters.includes(id)),
    [selectedConverters],
  );
  const [open, toggle] = useToggle();
  const [value, setValue] = useState<ComponentTypeRepresentation>();

  return (
    <FormGroup label={t("converterType")} fieldId="converter">
      <KeycloakSelect
        toggleId="converter"
        onToggle={toggle}
        onSelect={(value) => {
          const option = value as ComponentTypeRepresentation;
          onChange(option);
          setValue(option);
          toggle();
        }}
        selections={value?.id}
        variant="single"
        aria-label={t("selectOne")}
        isOpen={open}
        placeholderText={t("choose")}
        menuAppendTo="parent"
        maxHeight={300}
      >
        {converters.map((option) => (
          <SelectOption
            selected={value?.id === option.id}
            key={option.id}
            value={option}
            description={option.helpText}
          >
            {option.id}
          </SelectOption>
        ))}
      </KeycloakSelect>
    </FormGroup>
  );
};
