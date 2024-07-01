import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  FormErrorText,
  HelpItem,
  KeycloakSelect,
  SelectControl,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  Button,
  Divider,
  FormGroup,
  Grid,
  GridItem,
  Radio,
  SelectOption,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { GlobeRouteIcon } from "@patternfly/react-icons";
import { isEqual } from "lodash-es";
import { useEffect, useState } from "react";
import {
  Controller,
  FormProvider,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import { FormAccess } from "../../../components/form/FormAccess";
import { KeycloakSpinner } from "../../../components/keycloak-spinner/KeycloakSpinner";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { useFetch } from "../../../utils/useFetch";
import { useParams } from "../../../utils/useParams";
import useToggle from "../../../utils/useToggle";
import { USERNAME_EMAIL } from "../../NewAttributeSettings";
import { AttributeParams } from "../../routes/Attribute";
import {
  AddTranslationsDialog,
  TranslationsType,
} from "./AddTranslationsDialog";
import { DefaultSwitchControl } from "../../../components/SwitchControl";

import "../../realm-settings-section.css";

const REQUIRED_FOR = [
  { label: "requiredForLabel.both", value: ["admin", "user"] },
  { label: "requiredForLabel.users", value: ["user"] },
  { label: "requiredForLabel.admins", value: ["admin"] },
] as const;

type TranslationForm = {
  locale: string;
  value: string;
};

type Translations = {
  key: string;
  translations: TranslationForm[];
};

export type AttributeGeneralSettingsProps = {
  onHandlingTranslationData: (data: Translations) => void;
  onHandlingGeneratedDisplayName: (displayName: string) => void;
};

export const AttributeGeneralSettings = ({
  onHandlingTranslationData,
  onHandlingGeneratedDisplayName,
}: AttributeGeneralSettingsProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realmRepresentation: realm } = useRealm();
  const form = useFormContext();
  const [clientScopes, setClientScopes] =
    useState<ClientScopeRepresentation[]>();
  const [config, setConfig] = useState<UserProfileConfig>();
  const [selectEnabledWhenOpen, setSelectEnabledWhenOpen] = useState(false);
  const [selectRequiredForOpen, setSelectRequiredForOpen] = useState(false);
  const [addTranslationsModalOpen, toggleModal] = useToggle();
  const { attributeName } = useParams<AttributeParams>();
  const editMode = attributeName ? true : false;
  const [newAttributeName, setNewAttributeName] = useState("");
  const [generatedDisplayName, setGeneratedDisplayName] = useState("");
  const [type, setType] = useState<TranslationsType>();
  const [translationsData, setTranslationsData] = useState<Translations>({
    key: "",
    translations: [],
  });
  const displayNameRegex = /\$\{([^}]+)\}/;

  const handleAttributeNameChange = (
    _event: React.FormEvent<HTMLInputElement>,
    value: string,
  ) => {
    setNewAttributeName(value);
    const newDisplayName =
      value !== "" && realm?.internationalizationEnabled
        ? "${profile.attributes." + `${value}}`
        : "";
    setGeneratedDisplayName(newDisplayName);
  };

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

  const attributeDisplayName = useWatch({
    control: form.control,
    name: "displayName",
  });

  const displayNamePatternMatch = displayNameRegex.test(attributeDisplayName);

  useFetch(() => adminClient.clientScopes.find(), setClientScopes, []);
  useFetch(() => adminClient.users.getProfile(), setConfig, []);

  const handleTranslationsData = (translationsData: Translations) => {
    onHandlingTranslationData(translationsData);
  };

  const handleGeneratedDisplayName = (displayName: string) => {
    onHandlingGeneratedDisplayName(displayName);
  };

  useEffect(() => {
    handleTranslationsData(translationsData);
    handleGeneratedDisplayName(generatedDisplayName);
  }, [translationsData, generatedDisplayName]);

  if (!clientScopes) {
    return <KeycloakSpinner />;
  }

  function setHasSelector(hasSelector: boolean) {
    form.setValue("hasSelector", hasSelector);
  }

  function setHasRequiredScopes(hasRequiredScopes: boolean) {
    form.setValue("hasRequiredScopes", hasRequiredScopes);
  }

  const handleTranslationsAdded = (translationsData: Translations) => {
    setTranslationsData(translationsData);
  };

  const handleToggleDialog = () => {
    toggleModal();
    handleTranslationsData(translationsData);
    handleGeneratedDisplayName(generatedDisplayName);
  };

  const formattedAttributeDisplayName = attributeDisplayName?.substring(
    2,
    attributeDisplayName.length - 1,
  );

  return (
    <>
      {addTranslationsModalOpen && (
        <AddTranslationsDialog
          translationKey={
            editMode
              ? formattedAttributeDisplayName
              : `profile.attributes.${newAttributeName}`
          }
          translations={translationsData}
          type={type ?? "displayName"}
          onTranslationsAdded={handleTranslationsAdded}
          toggleDialog={handleToggleDialog}
          onCancel={() => {
            toggleModal();
          }}
        />
      )}
      <FormAccess role="manage-realm" isHorizontal>
        <FormProvider {...form}>
          <FormGroup
            label={t("attributeName")}
            labelIcon={
              <HelpItem
                helpText={t("upAttributeNameHelp")}
                fieldLabelId="attributeName"
              />
            }
            fieldId="kc-attribute-name"
            isRequired
          >
            <TextInput
              isRequired
              id="kc-attribute-name"
              defaultValue=""
              data-testid="attribute-name"
              isDisabled={editMode}
              validated={form.formState.errors.name ? "error" : "default"}
              {...form.register("name", { required: true })}
              onChange={handleAttributeNameChange}
            />
            {form.formState.errors.name && (
              <FormErrorText message={t("validateAttributeName")} />
            )}
          </FormGroup>
          <FormGroup
            label={t("attributeDisplayName")}
            labelIcon={
              <HelpItem
                helpText={t("attributeDisplayNameHelp")}
                fieldLabelId="attributeDisplayName"
              />
            }
            fieldId="kc-attribute-display-name"
          >
            <Grid hasGutter>
              <GridItem span={realm?.internationalizationEnabled ? 11 : 12}>
                <TextInput
                  id="kc-attribute-display-name"
                  data-testid="attribute-display-name"
                  isDisabled={
                    (realm?.internationalizationEnabled &&
                      newAttributeName !== "") ||
                    (editMode && displayNamePatternMatch)
                  }
                  value={
                    editMode
                      ? attributeDisplayName
                      : realm?.internationalizationEnabled
                        ? generatedDisplayName
                        : undefined
                  }
                  {...form.register("displayName")}
                />
                {generatedDisplayName && (
                  <Alert
                    className="pf-v5-u-mt-sm"
                    variant="info"
                    isInline
                    isPlain
                    title={t("addAttributeTranslationInfo")}
                  />
                )}
              </GridItem>
              {realm?.internationalizationEnabled && (
                <GridItem span={1}>
                  <Button
                    variant="link"
                    className="pf-m-plain kc-attribute-display-name-iconBtn"
                    data-testid="addAttributeTranslationBtn"
                    aria-label={t("addAttributeTranslationBtn")}
                    isDisabled={!newAttributeName && !editMode}
                    onClick={() => {
                      setType("displayName");
                      toggleModal();
                    }}
                    icon={<GlobeRouteIcon />}
                  />
                </GridItem>
              )}
            </Grid>
          </FormGroup>
          <DefaultSwitchControl
            name="multivalued"
            label={t("multivalued")}
            labelIcon={t("multivaluedHelp")}
          />
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
                              <SelectOption
                                key={option.name}
                                value={option.name}
                              >
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
        </FormProvider>
      </FormAccess>
    </>
  );
};
