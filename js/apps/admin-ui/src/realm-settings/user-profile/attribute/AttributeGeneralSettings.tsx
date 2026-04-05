import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  HelpItem,
  KeycloakSelect,
  KeycloakSpinner,
  SelectControl,
  SelectVariant,
  TextControl,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Divider,
  FormGroup,
  Radio,
  SelectOption,
  Switch,
} from "@patternfly/react-core";
import { isEqual } from "lodash-es";
import { useState } from "react";
import {
  Controller,
  FormProvider,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import { FormAccess } from "../../../components/form/FormAccess";
import { DefaultSwitchControl } from "../../../components/SwitchControl";
import { useParams } from "../../../utils/useParams";
import { USERNAME_EMAIL } from "../../NewAttributeSettings";
import { AttributeParams } from "../../routes/Attribute";
import { TranslatableField } from "./TranslatableField";

import "../../realm-settings-section.css";
import useLocaleSort, { mapByKey } from "../../../utils/useLocaleSort";

const REQUIRED_FOR = [
  { label: "requiredForLabel.both", value: ["admin", "user"] },
  { label: "requiredForLabel.users", value: ["user"] },
  { label: "requiredForLabel.admins", value: ["admin"] },
] as const;

export const AttributeGeneralSettings = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const form = useFormContext();
  const [clientScopes, setClientScopes] =
    useState<ClientScopeRepresentation[]>();
  const [config, setConfig] = useState<UserProfileConfig>();
  const [selectEnabledWhenOpen, setSelectEnabledWhenOpen] = useState(false);
  const [selectRequiredForOpen, setSelectRequiredForOpen] = useState(false);

  const [enabledWhenSearch, setEnableWhenSearch] = useState("");
  const localeSort = useLocaleSort();

  const { attributeName } = useParams<AttributeParams>();
  const editMode = attributeName ? true : false;

  const hasSelector = useWatch({
    control: form.control,
    name: "hasSelector",
  });

  const hasRequiredScopes = useWatch({
    control: form.control,
    name: "hasRequiredScopes",
  });

  const required = useWatch({
    control: form.control,
    name: "isRequired",
    defaultValue: false,
  });

  useFetch(() => adminClient.clientScopes.find(), setClientScopes, []);
  useFetch(() => adminClient.users.getProfile(), setConfig, []);

  if (!clientScopes) {
    return <KeycloakSpinner />;
  }

  function setHasSelector(hasSelector: boolean) {
    form.setValue("hasSelector", hasSelector);
  }

  function setHasRequiredScopes(hasRequiredScopes: boolean) {
    form.setValue("hasRequiredScopes", hasRequiredScopes);
  }

  const items = () =>
    localeSort(clientScopes, mapByKey("name"))
      .filter(
        (s) => enabledWhenSearch === "" || s.name?.includes(enabledWhenSearch),
      )
      .map((option) => (
        <SelectOption key={option.name} value={option.name}>
          {option.name}
        </SelectOption>
      ));

  const ROOT_ATTRIBUTE = [...USERNAME_EMAIL, "firstName", "lastName"];

  return (
    <FormProvider {...form}>
      <FormAccess role="manage-realm" isHorizontal>
        <TextControl
          name="name"
          label={t("attributeName")}
          labelIcon={t("upAttributeNameHelp")}
          isDisabled={editMode}
          rules={{
            required: t("validateAttributeName"),
          }}
        />
        <FormGroup
          label={t("attributeDisplayName")}
          labelIcon={
            <HelpItem
              helpText={t("attributeDisplayNameHelp")}
              fieldLabelId="attributeDisplayName"
            />
          }
          fieldId="kc-attribute-displayName"
        >
          <TranslatableField
            attributeName="name"
            prefix="profile.attributes"
            fieldName="displayName"
            predefinedAttributes={[
              "username",
              "email",
              "firstName",
              "lastName",
            ]}
          />
        </FormGroup>
        <DefaultSwitchControl
          name="multivalued"
          label={t("multivalued")}
          labelIcon={t("multivaluedHelp")}
        />
        {!ROOT_ATTRIBUTE.includes(attributeName) && (
          <TextControl
            name="defaultValue"
            label={t("defaultValue")}
            labelIcon={t("defaultValueHelp")}
          />
        )}
        <SelectControl
          name="group"
          label={t("attributeGroup")}
          labelIcon={t("attributeGroupHelp")}
          controller={{
            defaultValue: "",
          }}
          options={[
            { key: "", value: t("none") },
            ...(config?.groups?.map((g) => ({
              key: g.name!,
              value: g.name!,
            })) || []),
          ]}
        />
        {!USERNAME_EMAIL.includes(attributeName) && (
          <>
            <Divider />
            <FormGroup
              label={t("enabledWhen")}
              labelIcon={
                <HelpItem
                  helpText={t("enabledWhenTooltip")}
                  fieldLabelId="enabled-when"
                />
              }
              fieldId="enabledWhen"
              hasNoPaddingTop
            >
              <Radio
                id="always"
                data-testid="always"
                isChecked={!hasSelector}
                name="enabledWhen"
                label={t("always")}
                onChange={() => setHasSelector(false)}
                className="pf-v5-u-mb-md"
              />
              <Radio
                id="scopesAsRequested"
                data-testid="scopesAsRequested"
                isChecked={hasSelector}
                name="enabledWhen"
                label={t("scopesAsRequested")}
                onChange={() => setHasSelector(true)}
                className="pf-v5-u-mb-md"
              />
            </FormGroup>
            {hasSelector && (
              <FormGroup fieldId="kc-scope-enabled-when">
                <Controller
                  name="selector.scopes"
                  control={form.control}
                  defaultValue={[]}
                  render={({ field }) => (
                    <KeycloakSelect
                      data-testid="enabled-when-scope-field"
                      variant={SelectVariant.typeaheadMulti}
                      onFilter={(value) => {
                        setEnableWhenSearch(value);
                        return items();
                      }}
                      typeAheadAriaLabel="Select"
                      chipGroupProps={{
                        numChips: 3,
                        expandedText: t("hide"),
                        collapsedText: t("showRemaining"),
                      }}
                      onToggle={(isOpen) => setSelectEnabledWhenOpen(isOpen)}
                      selections={field.value}
                      onSelect={(selectedValue) => {
                        const option = selectedValue.toString();
                        let changedValue = [""];
                        if (field.value) {
                          changedValue = field.value.includes(option)
                            ? field.value.filter(
                                (item: string) => item !== option,
                              )
                            : [...field.value, option];
                        } else {
                          changedValue = [option];
                        }

                        field.onChange(changedValue);
                      }}
                      onClear={() => {
                        field.onChange([]);
                      }}
                      isOpen={selectEnabledWhenOpen}
                      aria-labelledby={"scope"}
                    >
                      {items()}
                    </KeycloakSelect>
                  )}
                />
              </FormGroup>
            )}
          </>
        )}
        {attributeName !== "username" && (
          <>
            <Divider />
            <FormGroup
              label={t("required")}
              labelIcon={
                <HelpItem
                  helpText={t("requiredHelp")}
                  fieldLabelId="required"
                />
              }
              fieldId="kc-required"
              hasNoPaddingTop
            >
              <Controller
                name="isRequired"
                data-testid="required"
                defaultValue={false}
                control={form.control}
                render={({ field }) => (
                  <Switch
                    id={"kc-required"}
                    onChange={field.onChange}
                    isChecked={field.value}
                    label={t("on")}
                    labelOff={t("off")}
                    aria-label={t("required")}
                  />
                )}
              />
            </FormGroup>
            {required && (
              <>
                <FormGroup
                  label={t("requiredFor")}
                  fieldId="requiredFor"
                  hasNoPaddingTop
                >
                  <Controller
                    name="required.roles"
                    data-testid="requiredFor"
                    defaultValue={REQUIRED_FOR[0].value}
                    control={form.control}
                    render={({ field }) => (
                      <div className="kc-requiredFor">
                        {REQUIRED_FOR.map((option) => (
                          <Radio
                            id={option.label}
                            key={option.label}
                            data-testid={option.label}
                            isChecked={isEqual(field.value, option.value)}
                            name="roles"
                            onChange={() => {
                              field.onChange(option.value);
                            }}
                            label={t(option.label)}
                            className="kc-requiredFor-option"
                          />
                        ))}
                      </div>
                    )}
                  />
                </FormGroup>
                <FormGroup
                  label={t("requiredWhen")}
                  labelIcon={
                    <HelpItem
                      helpText={t("requiredWhenTooltip")}
                      fieldLabelId="required-when"
                    />
                  }
                  fieldId="requiredWhen"
                  hasNoPaddingTop
                >
                  <Radio
                    id="requiredAlways"
                    data-testid="requiredAlways"
                    isChecked={!hasRequiredScopes}
                    name="requiredWhen"
                    label={t("always")}
                    onChange={() => setHasRequiredScopes(false)}
                    className="pf-v5-u-mb-md"
                  />
                  <Radio
                    id="requiredScopesAsRequested"
                    data-testid="requiredScopesAsRequested"
                    isChecked={hasRequiredScopes}
                    name="requiredWhen"
                    label={t("scopesAsRequested")}
                    onChange={() => setHasRequiredScopes(true)}
                    className="pf-v5-u-mb-md"
                  />
                </FormGroup>
                {hasRequiredScopes && (
                  <FormGroup fieldId="kc-scope-required-when">
                    <Controller
                      name="required.scopes"
                      control={form.control}
                      defaultValue={[]}
                      render={({ field }) => (
                        <KeycloakSelect
                          data-testid="required-when-scope-field"
                          variant={SelectVariant.typeaheadMulti}
                          typeAheadAriaLabel="Select"
                          chipGroupProps={{
                            numChips: 3,
                            expandedText: t("hide"),
                            collapsedText: t("showRemaining"),
                          }}
                          onToggle={(isOpen) =>
                            setSelectRequiredForOpen(isOpen)
                          }
                          selections={field.value}
                          onSelect={(selectedValue) => {
                            const option = selectedValue.toString();
                            let changedValue = [""];
                            if (field.value) {
                              changedValue = field.value.includes(option)
                                ? field.value.filter(
                                    (item: string) => item !== option,
                                  )
                                : [...field.value, option];
                            } else {
                              changedValue = [option];
                            }
                            field.onChange(changedValue);
                          }}
                          onClear={() => {
                            field.onChange([]);
                          }}
                          isOpen={selectRequiredForOpen}
                          aria-labelledby={"scope"}
                        >
                          {clientScopes.map((option) => (
                            <SelectOption key={option.name} value={option.name}>
                              {option.name}
                            </SelectOption>
                          ))}
                        </KeycloakSelect>
                      )}
                    />
                  </FormGroup>
                )}
              </>
            )}
          </>
        )}
      </FormAccess>
    </FormProvider>
  );
};
