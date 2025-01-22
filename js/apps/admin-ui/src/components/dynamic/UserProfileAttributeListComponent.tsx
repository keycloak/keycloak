import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  FormErrorText,
  HelpItem,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { KeySelect } from "../key-value-form/KeySelect";
import type { ComponentProps } from "./components";

export const UserProfileAttributeListComponent = ({
  name,
  label,
  helpText,
  required = false,
  convertToName,
}: ComponentProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const {
    formState: { errors },
  } = useFormContext();

  const [config, setConfig] = useState<UserProfileConfig>();
  const convertedName = convertToName(name!);

  useFetch(
    () => adminClient.users.getProfile(),
    (cfg) => setConfig(cfg),
    [],
  );

  const convert = (config?: UserProfileConfig) => {
    if (!config?.attributes) return [];

    return config.attributes.map((option) => ({
      key: option.name!,
      label: option.name!,
    }));
  };

  if (!config) return null;

  return (
    <FormGroup
      label={t(label!)}
      isRequired={required}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={label!} />}
      fieldId={convertedName!}
    >
      <KeySelect
        name={convertedName}
        rules={required ? { required: true } : {}}
        selectItems={convert(config)}
      />
      {errors[convertedName!] && <FormErrorText message={t("required")} />}
    </FormGroup>
  );
};
