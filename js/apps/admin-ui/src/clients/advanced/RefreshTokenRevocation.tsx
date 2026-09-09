import { HelpItem } from "@keycloak/keycloak-ui-shared";
import {
  FormGroup,
  MenuToggle,
  NumberInput,
  Select,
  SelectList,
  SelectOption,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

const REVOKE_REFRESH_TOKEN = convertAttributeNameToForm<FormFields>(
  "attributes.revoke.refresh.token",
);
const REFRESH_TOKEN_MAX_REUSE = convertAttributeNameToForm<FormFields>(
  "attributes.refresh.token.max.reuse",
);

const ENABLED = "true";
const DISABLED = "false";
const INHERITED = "";

const toNumber = (value: unknown, fallback: number) => {
  const number = Number(value);
  return value === "" || value == null || isNaN(number) ? fallback : number;
};

/**
 * Client-level override of the realm settings "Revoke Refresh Token" and "Refresh Token Max Reuse",
 * laid out like {@link TokenLifespan}: a selector to inherit, enable or disable revocation and an inline
 * input for the max reuse that is only shown when revocation is enabled for the client. When revocation
 * is not explicitly enabled, both attributes are cleared so the realm settings apply.
 */
export const RefreshTokenRevocation = () => {
  const { t } = useTranslation();
  const { realmRepresentation: realm } = useRealm();
  const { control, setValue, getValues } = useFormContext();
  const [open, setOpen] = useState(false);

  const realmRevokeRefreshToken = realm.revokeRefreshToken ?? false;
  const realmMaxReuse = realm.refreshTokenMaxReuse ?? 0;

  const options = [
    {
      value: INHERITED,
      label: t("inheritsFromRealmSettings", {
        value: t(realmRevokeRefreshToken ? "enabled" : "disabled"),
      }),
    },
    { value: ENABLED, label: t("enabled") },
    { value: DISABLED, label: t("disabled") },
  ];
  // Only explicit "true"/"false" are overrides, anything else inherits the realm setting (same as the server side)
  const labelFor = (value?: string) =>
    (options.find((option) => option.value === value) ?? options[0]).label;

  return (
    <FormGroup
      label={t("revokeRefreshToken")}
      fieldId="revokeRefreshToken"
      labelIcon={
        <HelpItem
          helpText={t("revokeRefreshTokenClientHelp")}
          fieldLabelId="revokeRefreshToken"
        />
      }
      data-testid="revoke-refresh-token"
    >
      <Controller
        name={REVOKE_REFRESH_TOKEN}
        defaultValue={INHERITED}
        control={control}
        render={({ field }) => {
          const isEnabled = field.value === ENABLED;
          return (
            <Split hasGutter>
              <SplitItem>
                <Select
                  toggle={(ref) => (
                    <MenuToggle
                      ref={ref}
                      id="revokeRefreshToken"
                      onClick={() => setOpen(!open)}
                      isExpanded={open}
                    >
                      {labelFor(field.value)}
                    </MenuToggle>
                  )}
                  isOpen={open}
                  onOpenChange={(isOpen) => setOpen(isOpen)}
                  onSelect={(_, value) => {
                    field.onChange(value);
                    // The max reuse is an override only while revocation is enabled for the client;
                    // seed it with the realm value when enabling, like TokenLifespan does with the lifespan
                    setValue(
                      REFRESH_TOKEN_MAX_REUSE,
                      value === ENABLED
                        ? toNumber(
                            getValues(REFRESH_TOKEN_MAX_REUSE),
                            realmMaxReuse,
                          )
                        : INHERITED,
                      { shouldDirty: true },
                    );
                    setOpen(false);
                  }}
                  selected={field.value}
                >
                  <SelectList>
                    {options.map(({ value, label }) => (
                      <SelectOption key={value} value={value}>
                        {label}
                      </SelectOption>
                    ))}
                  </SelectList>
                </Select>
              </SplitItem>
              <SplitItem hidden={!isEnabled}>
                <Controller
                  name={REFRESH_TOKEN_MAX_REUSE}
                  defaultValue={INHERITED}
                  control={control}
                  render={({ field: maxReuse }) => {
                    const value = toNumber(maxReuse.value, realmMaxReuse);
                    const update = (newValue: number) =>
                      maxReuse.onChange(Math.max(0, newValue));
                    return (
                      <NumberInput
                        id="refreshTokenMaxReuse"
                        inputAriaLabel={t("refreshTokenMaxReuse")}
                        inputProps={{ "data-testid": "refreshTokenMaxReuse" }}
                        unit={
                          <span style={{ whiteSpace: "nowrap" }}>
                            {t("refreshTokenMaxReuseShort")}
                          </span>
                        }
                        unitPosition="before"
                        min={0}
                        value={value}
                        isDisabled={!isEnabled}
                        onPlus={() => update(value + 1)}
                        onMinus={() => update(value - 1)}
                        onChange={(event) =>
                          update(
                            toNumber(
                              (event.target as HTMLInputElement).value,
                              0,
                            ),
                          )
                        }
                      />
                    );
                  }}
                />
              </SplitItem>
            </Split>
          );
        }}
      />
    </FormGroup>
  );
};
