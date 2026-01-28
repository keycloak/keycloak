import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  FormErrorText,
  HelpItem,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useEffect } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { AttributeForm } from "../components/key-value-form/AttributeForm";
import { keyValueToArray } from "../components/key-value-form/key-value-convert";
import { MultiLineInput } from "../components/multi-line-input/MultiLineInput";

export type OrganizationFormType = AttributeForm &
  Omit<OrganizationRepresentation, "domains" | "attributes"> & {
    domains?: string[];
  };

export const convertToOrg = (
  org: OrganizationFormType,
): OrganizationRepresentation => ({
  ...org,
  domains: org.domains?.map((d) => ({ name: d, verified: false })),
  attributes: keyValueToArray(org.attributes),
});

type OrganizationFormProps = {
  readOnly?: boolean;
};

export const OrganizationForm = ({
  readOnly = false,
}: OrganizationFormProps) => {
  const { t } = useTranslation();
  const {
    setValue,
    formState: { errors },
  } = useFormContext();
  const name = useWatch({ name: "name" });

  useEffect(() => {
    if (!readOnly) {
      setValue("alias", name);
    }
  }, [name, readOnly]);

  return (
    <>
      <TextControl
        label={t("name")}
        name="name"
        rules={{ required: t("required") }}
      />
      <TextControl
        label={t("alias")}
        name="alias"
        labelIcon={t("organizationAliasHelp")}
        isDisabled={readOnly}
      />
      <FormGroup
        label={t("domain")}
        fieldId="domain"
        labelIcon={
          <HelpItem
            helpText={t("organizationDomainHelp")}
            fieldLabelId="domain"
          />
        }
      >
        <MultiLineInput
          id="domain"
          name="domains"
          aria-label={t("domain")}
          addButtonLabel="addDomain"
        />
        {errors?.["domains"]?.message && (
          <FormErrorText message={errors["domains"].message.toString()} />
        )}
      </FormGroup>
      <TextControl
        label={t("redirectUrl")}
        name="redirectUrl"
        labelIcon={t("organizationRedirectUrlHelp")}
      />
      <TextAreaControl name="description" label={t("description")} />
    </>
  );
};
