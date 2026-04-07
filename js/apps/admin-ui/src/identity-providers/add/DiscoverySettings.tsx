import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  ExpandableSection,
  FormGroup,
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
} from "@patternfly/react-core";
import { ReactNode, RefObject, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  HelpItem,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { JwksSettings } from "./JwksSettings";

import "./discovery-settings.css";

const PKCE_METHODS = ["plain", "S256"] as const;

// Fields that the background sync job can update.
export const SYNCED_FIELDS = [
  "authorizationUrl",
  "tokenUrl",
  "logoutUrl",
  "userInfoUrl",
  "tokenIntrospectionUrl",
  "issuer",
  "jwksUrl",
] as const;

export type SyncedField = (typeof SYNCED_FIELDS)[number];

type DiscoverySettingsProps = {
  readOnly: boolean;
  isOIDC: boolean;
  reloadEnabled?: boolean;
  includedFields?: SyncedField[];
  onToggleInclude?: (field: SyncedField) => void;
};

type FieldsProps = {
  readOnly: boolean;
  isOIDC: boolean;
  reloadEnabled: boolean;
  includedFields: SyncedField[];
  onToggleInclude: (field: SyncedField) => void;
};

type SyncedFieldControlProps = {
  field: SyncedField;
  fieldLabel: string;
  reloadEnabled: boolean;
  includedFields: SyncedField[];
  onToggleInclude: (field: SyncedField) => void;
  children: ReactNode;
};

const SyncedFieldControl = ({ children }: SyncedFieldControlProps) => children;

type SyncMultiselectProps = {
  id: string;
  selected: SyncedField[];
  onSelect: (field: SyncedField) => void;
};

export const SyncMultiselect = ({
  id,
  selected,
  onSelect,
}: SyncMultiselectProps) => {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);

  const toggle = (ref: RefObject<HTMLButtonElement>) => (
    <MenuToggle
      ref={ref}
      data-testid={`${id}-toggle`}
      onClick={() => setIsOpen((o) => !o)}
      isExpanded={isOpen}
    >
      {selected.length === SYNCED_FIELDS.length
        ? t("allFields")
        : selected.length === 0
          ? t("none")
          : `${selected.length} / ${SYNCED_FIELDS.length}`}
    </MenuToggle>
  );

  return (
    <FormGroup
      label={t("syncTheseFields")}
      fieldId={id}
      labelIcon={
        <HelpItem helpText={t("syncTheseFieldsHelp")} fieldLabelId={id} />
      }
    >
      <Select
        id={id}
        isOpen={isOpen}
        selected={selected}
        onSelect={(_e, value) => onSelect(value as SyncedField)}
        onOpenChange={setIsOpen}
        toggle={toggle}
      >
        <SelectList>
          {SYNCED_FIELDS.map((field) => (
            <SelectOption
              key={field}
              hasCheckbox
              value={field}
              isSelected={selected.includes(field)}
              data-testid={`sync-field-${field}`}
            >
              {t(field)}
            </SelectOption>
          ))}
        </SelectList>
      </Select>
    </FormGroup>
  );
};

const Fields = ({
  readOnly,
  isOIDC,
  reloadEnabled,
  includedFields,
  onToggleInclude,
}: FieldsProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext<IdentityProviderRepresentation>();

  const validateSignature = useWatch({
    control,
    name: "config.validateSignature",
  });
  const isPkceEnabled = useWatch({ control, name: "config.pkceEnabled" });
  const jwtAuthorizationGrantEnabled = useWatch({
    control,
    name: "config.jwtAuthorizationGrantEnabled",
  });
  const supportsClientAssertions = useWatch({
    control,
    name: "config.supportsClientAssertions",
  });

  // A field is read-only if the form is globally read-only, OR if sync is
  // enabled and the field is in the inclusion list.
  const isFieldReadOnly = (field: SyncedField) =>
    readOnly || (reloadEnabled && includedFields.includes(field));

  return (
    <div className="pf-v5-c-form pf-m-horizontal">
      <SyncedFieldControl
        field="authorizationUrl"
        fieldLabel={t("authorizationUrl")}
        reloadEnabled={reloadEnabled}
        includedFields={includedFields}
        onToggleInclude={onToggleInclude}
      >
        <TextControl
          name="config.authorizationUrl"
          label={t("authorizationUrl")}
          type="url"
          readOnly={isFieldReadOnly("authorizationUrl")}
          rules={{ required: t("required") }}
        />
      </SyncedFieldControl>
      <SyncedFieldControl
        field="tokenUrl"
        fieldLabel={t("tokenUrl")}
        reloadEnabled={reloadEnabled}
        includedFields={includedFields}
        onToggleInclude={onToggleInclude}
      >
        <TextControl
          name="config.tokenUrl"
          label={t("tokenUrl")}
          type="url"
          readOnly={isFieldReadOnly("tokenUrl")}
          rules={{ required: t("required") }}
        />
      </SyncedFieldControl>
      {isOIDC && (
        <SyncedFieldControl
          field="logoutUrl"
          fieldLabel={t("logoutUrl")}
          reloadEnabled={reloadEnabled}
          includedFields={includedFields}
          onToggleInclude={onToggleInclude}
        >
          <TextControl
            name="config.logoutUrl"
            label={t("logoutUrl")}
            readOnly={isFieldReadOnly("logoutUrl")}
          />
        </SyncedFieldControl>
      )}
      <SyncedFieldControl
        field="userInfoUrl"
        fieldLabel={t("userInfoUrl")}
        reloadEnabled={reloadEnabled}
        includedFields={includedFields}
        onToggleInclude={onToggleInclude}
      >
        <TextControl
          name="config.userInfoUrl"
          label={t("userInfoUrl")}
          readOnly={isFieldReadOnly("userInfoUrl")}
          rules={{ required: isOIDC ? "" : t("required") }}
        />
      </SyncedFieldControl>
      <SyncedFieldControl
        field="tokenIntrospectionUrl"
        fieldLabel={t("tokenIntrospectionUrl")}
        reloadEnabled={reloadEnabled}
        includedFields={includedFields}
        onToggleInclude={onToggleInclude}
      >
        <TextControl
          name="config.tokenIntrospectionUrl"
          label={t("tokenIntrospectionUrl")}
          type="url"
          readOnly={isFieldReadOnly("tokenIntrospectionUrl")}
        />
      </SyncedFieldControl>
      {isOIDC && (
        <SyncedFieldControl
          field="issuer"
          fieldLabel={t("issuer")}
          reloadEnabled={reloadEnabled}
          includedFields={includedFields}
          onToggleInclude={onToggleInclude}
        >
          <TextControl
            name="config.issuer"
            label={t("issuer")}
            readOnly={isFieldReadOnly("issuer")}
          />
        </SyncedFieldControl>
      )}
      {isOIDC && (
        <>
          <DefaultSwitchControl
            name="config.validateSignature"
            label={t("validateSignature")}
            labelIcon={t("validateSignatureHelp")}
            isDisabled={readOnly}
            stringify
          />
          {(validateSignature === "true" ||
            jwtAuthorizationGrantEnabled === "true" ||
            supportsClientAssertions === "true") && (
            <JwksSettings readOnly={isFieldReadOnly("jwksUrl")} />
          )}
        </>
      )}
      <DefaultSwitchControl
        name="config.pkceEnabled"
        label={t("pkceEnabled")}
        isDisabled={readOnly}
        stringify
      />
      {isPkceEnabled === "true" && (
        <SelectControl
          name="config.pkceMethod"
          label={t("pkceMethod")}
          labelIcon={t("pkceMethodHelp")}
          controller={{ defaultValue: PKCE_METHODS[0] }}
          options={PKCE_METHODS.map((option) => ({
            key: option,
            value: t(option),
          }))}
        />
      )}
    </div>
  );
};

export const DiscoverySettings = ({
  readOnly,
  isOIDC,
  reloadEnabled = false,
  includedFields = [],
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onToggleInclude = () => {},
}: DiscoverySettingsProps) => {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <>
      {readOnly && (
        <ExpandableSection
          className="keycloak__discovery-settings__metadata"
          toggleText={isExpanded ? t("hideMetaData") : t("showMetaData")}
          onToggle={(_event, isOpen) => setIsExpanded(isOpen)}
          isExpanded={isExpanded}
        >
          <Fields
            readOnly={readOnly}
            isOIDC={isOIDC}
            reloadEnabled={reloadEnabled}
            includedFields={includedFields}
            onToggleInclude={onToggleInclude}
          />
        </ExpandableSection>
      )}
      {!readOnly && (
        <Fields
          readOnly={readOnly}
          isOIDC={isOIDC}
          reloadEnabled={reloadEnabled}
          includedFields={includedFields}
          onToggleInclude={onToggleInclude}
        />
      )}
    </>
  );
};
