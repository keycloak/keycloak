import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { KeycloakSelect } from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import useToggle from "../../../utils/useToggle";

export type ComponentTypeSelectProps = {
  /** Key of the provider type in the server info `componentTypes`. */
  componentType: string;
  /** Ids of the components that are already selected and are not offered again. */
  selected: string[];
  onChange: (component: ComponentTypeRepresentation) => void;
  /** Translation key of the field label. */
  labelKey: string;
  /** Id of the select toggle and the form group field. */
  fieldId: string;
};

export const ComponentTypeSelect = ({
  componentType,
  selected,
  onChange,
  labelKey,
  fieldId,
}: ComponentTypeSelectProps) => {
  const { t } = useTranslation();
  const { componentTypes } = useServerInfo();
  const options = useMemo(
    () =>
      (componentTypes?.[componentType] || []).filter(
        ({ id }) => !selected.includes(id),
      ),
    [componentTypes, componentType, selected],
  );
  const [open, toggle] = useToggle();
  const [value, setValue] = useState<ComponentTypeRepresentation>();

  return (
    <FormGroup label={t(labelKey)} fieldId={fieldId}>
      <KeycloakSelect
        toggleId={fieldId}
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
        {options.map((option) => (
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
