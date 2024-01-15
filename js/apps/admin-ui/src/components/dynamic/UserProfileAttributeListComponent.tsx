import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { FormGroup } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";
import { convertToName } from "./DynamicComponents";
import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import type { ComponentProps } from "./components";
import { KeySelect, FetchCallback } from "../key-value-form/KeySelect";

type FetchCallbackWithValue = FetchCallback & {
  fieldValue: string;
};
export const UserProfileAttributeListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  required = false,
}: ComponentProps) => {
  const { t } = useTranslation();
  const {
    control,
    formState: { errors },
  } = useFormContext();

  const [config, setConfig] = useState<UserProfileConfig>();
  const convertedName = convertToName(name!);

  const [custom, setCustom] = useState(true);
  const fetchCallback: FetchCallbackWithValue = {
    fieldValue: "",
    custom,
    setCustom,
  };
  const updateFetchCallback: (
    currentValue: string,
  ) => FetchCallbackWithValue = (currentValue) => {
    fetchCallback.fieldValue = currentValue;
    return fetchCallback;
  };

  useFetch(
    () => adminClient.users.getProfile(),
    (cfg) => {
      setConfig(cfg);

      const fieldIncluded = cfg.attributes
        ?.map((option) => option.name!)
        .includes(fetchCallback.fieldValue);
      setCustom(!fieldIncluded);
    },
    [],
  );

  const convert = (config?: UserProfileConfig) => {
    if (!config?.attributes) return [];

    return config.attributes.map((option) => ({
      key: option.name!,
      label: option.name!,
    }));
  };

  return (
    <FormGroup
      label={t(label!)}
      isRequired={required}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={label!} />}
      fieldId={convertedName!}
      validated={errors[convertedName!] ? "error" : "default"}
      helperTextInvalid={t("required")}
    >
      <Controller
        name={convertedName!}
        defaultValue={defaultValue || ""}
        control={control}
        rules={required ? { required: true } : {}}
        render={({ field }) => (
          <KeySelect
            name={convertedName}
            rules={required ? { required: true } : {}}
            selectItems={convert(config)}
            fetchCallback={updateFetchCallback(field.value)}
          />
        )}
      />
    </FormGroup>
  );
};
