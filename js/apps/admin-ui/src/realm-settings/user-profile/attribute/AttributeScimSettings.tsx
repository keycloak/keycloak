import { FormGroup, SelectGroup, SelectOption } from "@patternfly/react-core";
import { useFieldArray, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../../components/form/FormAccess";
import { HelpItem, KeycloakSelect } from "@keycloak/keycloak-ui-shared";
import { useParams } from "../../../utils/useParams";
import type { AttributeParams } from "../../routes/Attribute";
import { useUserProfile } from "../UserProfileContext";

import "../../realm-settings-section.css";
import { useState } from "react";

const SCIM_CORE_ATTRIBUTES = [
  "name.middleName",
  "name.honorificPrefix",
  "name.honorificSuffix",
  "name.formatted",
  "nickName",
  "profileUrl",
  "title",
  "externalId",
  "userType",
  "timezone",
  "preferredLanguage",
];

const SCIM_ENTERPRISE_ATTRIBUTES = [
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:organization",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:division",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.displayName",
];

type ScimAttributeGroup = {
  labelKey: string;
  attributes: string[];
};

const SCIM_ATTRIBUTE_GROUPS: ScimAttributeGroup[] = [
  {
    labelKey: "scimCoreUserSchema",
    attributes: SCIM_CORE_ATTRIBUTES,
  },
  {
    labelKey: "scimEnterpriseUserSchema",
    attributes: SCIM_ENTERPRISE_ATTRIBUTES,
  },
];

export const SCIM_ANNOTATION_KEY = "kc.scim.schema.attribute";

export const AttributeScimSettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const { attributeName } = useParams<AttributeParams>();
  const { config } = useUserProfile();
  const [open, setOpen] = useState(false);
  const [filterValue, setFilterValue] = useState("");
  const { append, update } = useFieldArray({
    control,
    name: "annotations",
  });
  const annotationValues: Array<{ key: string; value?: unknown }> =
    useWatch({ name: "annotations", control, defaultValue: [] }) ?? [];
  const scimIndex = annotationValues.findIndex(
    (a) => a.key === SCIM_ANNOTATION_KEY,
  );
  const scimValue =
    scimIndex >= 0 ? String(annotationValues[scimIndex]?.value ?? "") : "";

  const handleScimChange = (value: string) => {
    if (scimIndex >= 0) {
      update(scimIndex, { key: SCIM_ANNOTATION_KEY, value });
    } else if (value) {
      append({ key: SCIM_ANNOTATION_KEY, value });
    }
  };

  const takenScimAttributes = (config?.attributes ?? [])
    .filter((attr) => attr.name !== attributeName)
    .map((attr) => attr.annotations?.[SCIM_ANNOTATION_KEY] as string)
    .filter(Boolean);

  const availableGroups = SCIM_ATTRIBUTE_GROUPS.map((group) => ({
    ...group,
    attributes: group.attributes.filter(
      (attr) => !takenScimAttributes.includes(attr),
    ),
  }));

  const filteredGroups = availableGroups
    .map((group) => ({
      ...group,
      attributes: filterValue
        ? group.attributes.filter((attr) =>
            attr.toLowerCase().includes(filterValue.toLowerCase()),
          )
        : group.attributes,
    }))
    .filter((group) => group.attributes.length > 0);

  const hasOptions = filteredGroups.some((g) => g.attributes.length > 0);

  return (
    <FormAccess role="manage-realm" isHorizontal>
      <FormGroup
        label={t("scimAttributeMapping")}
        labelIcon={
          <HelpItem
            helpText={t("scimAttributeMappingHelp")}
            fieldLabelId="scimAttributeMapping"
          />
        }
        fieldId="kc-scim-attribute"
      >
        <KeycloakSelect
          isOpen={open}
          onToggle={(b) => setOpen(b)}
          onSelect={(value) => {
            handleScimChange(String(value));
            setFilterValue("");
            setOpen(false);
          }}
          selections={scimValue}
          variant={"typeahead"}
          onFilter={(value) => {
            if (value) {
              handleScimChange(value);
            }
            setFilterValue(value);
          }}
        >
          {hasOptions ? (
            filteredGroups.map((group) => (
              <SelectGroup key={group.labelKey} label={t(group.labelKey)}>
                {group.attributes.map((attr) => (
                  <SelectOption key={attr} value={attr}>
                    {attr}
                  </SelectOption>
                ))}
              </SelectGroup>
            ))
          ) : (
            <SelectOption isDisabled>
              {t("noMatchingScimAttributes")}
            </SelectOption>
          )}
        </KeycloakSelect>
      </FormGroup>
    </FormAccess>
  );
};
