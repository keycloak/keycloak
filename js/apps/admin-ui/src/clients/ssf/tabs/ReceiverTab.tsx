import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  NumberControl,
  ScrollForm,
  SelectControl,
  SelectVariant,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Checkbox,
  Chip,
  FormGroup,
  Label,
  SelectOption,
  Split,
  SplitItem,
  Stack,
  StackItem,
  Text,
  TextContent,
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  InfoCircleIcon,
  MinusCircleIcon,
  PauseCircleIcon,
  TimesCircleIcon,
} from "@patternfly/react-icons";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../../components/form/FormAccess";
import { MultiLineInput } from "../../../components/multi-line-input/MultiLineInput";
import {
  AddRoleButton,
  AddRoleMappingModal,
  FilterType,
} from "../../../components/role-mapping/AddRoleMappingModal";
import { ServiceRole } from "../../../components/role-mapping/RoleMapping";
import { DefaultSwitchControl } from "../../../components/SwitchControl";
import { TimeSelector } from "../../../components/time-selector/TimeSelector";
import { convertAttributeNameToForm } from "../../../util";
import type { FormFields, SaveOptions } from "../../ClientDetails";
import type { SsfClientStream } from "./StreamTab";

const splitSupportedEvents = (value: unknown): string[] => {
  if (!value || typeof value !== "string") {
    return [];
  }
  return value
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
};

/**
 * Splits a {@code ssf.allowedDeliveryMethods} client attribute value
 * into its canonical lowercase tokens. The server uses
 * {@code ##}-separated storage (see {@code Constants.CFG_DELIMITER});
 * empty / unset attribute returns an empty array which the UI treats
 * as "both push and poll allowed".
 */
const splitDeliveryMethods = (value: unknown): string[] => {
  if (!value || typeof value !== "string") {
    return [];
  }
  return value
    .split("##")
    .map((s) => s.trim().toLowerCase())
    .filter((s) => s.length > 0);
};

export type ReceiverTabProps = {
  client: ClientRepresentation;
  clientStream: SsfClientStream | null;
  defaultSupportedEvents: string;
  availableSupportedEvents: string[];
  nativelyEmittedEvents: string[];
  defaultUserSubjectFormat: string;
  save: (options?: SaveOptions) => void;
  reset: () => void;
};

export const ReceiverTab = ({
  client,
  clientStream,
  defaultSupportedEvents,
  availableSupportedEvents,
  nativelyEmittedEvents,
  defaultUserSubjectFormat,
  save,
  reset,
}: ReceiverTabProps) => {
  const { t } = useTranslation();
  const { control, watch, setValue } = useFormContext<FormFields>();

  const [supportedEventsOpen, setSupportedEventsOpen] = useState(false);
  const [supportedEventsFilter, setSupportedEventsFilter] = useState("");
  const [emitOnlyEventsOpen, setEmitOnlyEventsOpen] = useState(false);
  const [emitOnlyEventsFilter, setEmitOnlyEventsFilter] = useState("");

  // --- SSF Required Role picker state ---
  const [rolePickerOpen, setRolePickerOpen] = useState(false);
  const [roleFilterType, setRoleFilterType] = useState<FilterType>("clients");

  // --- SSF Emit Events Role picker state ---
  // Independent modal/filter state so both pickers can coexist in the
  // same form without clobbering each other's open state.
  const [emitRolePickerOpen, setEmitRolePickerOpen] = useState(false);
  const [emitRoleFilterType, setEmitRoleFilterType] =
    useState<FilterType>("clients");

  const requiredRoleFieldName = convertAttributeNameToForm<FormFields>(
    "attributes.ssf.requiredRole",
  );

  const emitEventsRoleFieldName = convertAttributeNameToForm<FormFields>(
    "attributes.ssf.emitEventsRole",
  );

  const parseRoleValue = (value: string | undefined) => {
    if (!value?.includes(".")) return ["", value || ""];
    return value.split(".");
  };

  // DefaultSwitchControl with stringify persists "true" / "false" —
  // compare as string so the delay-millis input toggles in sync with
  // the switch rather than interpreting the raw boolean.
  const ssfAutoVerifyStream = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.autoVerifyStream"),
  );
  // DefaultSwitchControl with stringify persists "true" / "false" —
  // compare as string so the role picker toggles in sync with the
  // switch rather than interpreting the raw boolean.
  const ssfAllowEmitEvents = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.allowEmitEvents"),
  );

  // Drive the emit-only multi-select options off the live value of
  // supportedEvents so adding / removing a supported event immediately
  // adjusts what the operator can mark emit-only. No standalone
  // registry list — the emit-only set is a strict subset.
  const ssfSupportedEvents = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.supportedEvents"),
  );
  const ssfEmitOnlyEvents = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.emitOnlyEvents"),
  );

  const supportedEventsSelected = splitSupportedEvents(
    ssfSupportedEvents ?? defaultSupportedEvents,
  );
  const emitOnlyEventsSelected = splitSupportedEvents(ssfEmitOnlyEvents).filter(
    (e) => supportedEventsSelected.includes(e),
  );

  // Allowed delivery methods is stored as a ##-separated client attribute
  // matching ssf.allowedDeliveryMethods on the server. Empty / unset means
  // "both push and poll allowed" (transmitter default), which is also what
  // the checkboxes render as the initial state on a freshly registered
  // receiver. The two checkboxes drive the same form field via setValue —
  // the conditional reveal of "Valid push URLs" below keys off whether
  // "push" is selected here.
  const allowedDeliveryMethodsField = convertAttributeNameToForm<FormFields>(
    "attributes.ssf.allowedDeliveryMethods",
  );
  const ssfAllowedDeliveryMethodsRaw = watch(allowedDeliveryMethodsField);
  const allowedDeliveryMethodTokens = splitDeliveryMethods(
    ssfAllowedDeliveryMethodsRaw,
  );
  const pushDeliveryAllowed =
    allowedDeliveryMethodTokens.length === 0 ||
    allowedDeliveryMethodTokens.includes("push");
  const pollDeliveryAllowed =
    allowedDeliveryMethodTokens.length === 0 ||
    allowedDeliveryMethodTokens.includes("poll");

  const updateAllowedDeliveryMethod = (
    method: "push" | "poll",
    checked: boolean,
  ) => {
    const current =
      allowedDeliveryMethodTokens.length === 0
        ? new Set<string>(["push", "poll"])
        : new Set<string>(allowedDeliveryMethodTokens);
    if (checked) {
      current.add(method);
    } else {
      current.delete(method);
    }
    // At least one method must remain selected — silently re-enable the
    // toggled-off method if the user attempted to uncheck both. The
    // server treats blank as "both allowed" too, but driving the user
    // through that path makes the UI ambiguous (cleared → defaults).
    if (current.size === 0) {
      current.add(method);
    }
    setValue(
      allowedDeliveryMethodsField,
      // Canonical wire order push,poll → join in the same order so a
      // round-trip through storage doesn't churn the value.
      ["push", "poll"].filter((m) => current.has(m)).join("##"),
      { shouldDirty: true },
    );
  };

  return (
    <Card isFlat className="pf-v5-u-mt-md">
      <CardHeader>
        <CardTitle>{t("ssfReceiver")}</CardTitle>
      </CardHeader>
      <CardBody>
        <TextContent>
          <Text>{t("ssfReceiverHelp")}</Text>
        </TextContent>
      </CardBody>
      <CardBody>
        <FormAccess
          role="manage-clients"
          fineGrainedAccess={client.access?.configure}
          isHorizontal
        >
          <ScrollForm
            label={t("jumpToSection")}
            sections={[
              {
                title: t("ssfSectionGeneral"),
                panel: (
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      rowGap: "1.5rem",
                    }}
                  >
                    <Text className="pf-v5-u-pb-lg">
                      {t("ssfSectionGeneralHelp")}
                    </Text>
                    <FormGroup
                      label={t("ssfStreamStatusLabel")}
                      fieldId="ssfStreamStatusIndicator"
                    >
                      {!clientStream && (
                        <Label
                          color="grey"
                          icon={<MinusCircleIcon />}
                          data-testid="ssfStreamIndicator.unregistered"
                        >
                          {t("ssfStreamIndicator.unregistered")}
                        </Label>
                      )}
                      {clientStream?.status === "enabled" && (
                        <Label
                          color="green"
                          icon={<CheckCircleIcon />}
                          data-testid="ssfStreamIndicator.enabled"
                        >
                          {t("ssfStreamIndicator.enabled")}
                        </Label>
                      )}
                      {clientStream?.status === "paused" && (
                        <Label
                          color="orange"
                          icon={<PauseCircleIcon />}
                          data-testid="ssfStreamIndicator.paused"
                        >
                          {t("ssfStreamIndicator.paused")}
                        </Label>
                      )}
                      {clientStream?.status === "disabled" && (
                        <Label
                          color="red"
                          icon={<TimesCircleIcon />}
                          data-testid="ssfStreamIndicator.disabled"
                        >
                          {t("ssfStreamIndicator.disabled")}
                        </Label>
                      )}
                      {clientStream && !clientStream.status && (
                        <Label
                          color="blue"
                          icon={<InfoCircleIcon />}
                          data-testid="ssfStreamIndicator.registered"
                        >
                          {t("ssfStreamIndicator.registered")}
                        </Label>
                      )}
                    </FormGroup>
                    <SelectControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.profile",
                      )}
                      label={t("ssfProfile")}
                      labelIcon={t("ssfProfileHelp")}
                      controller={{
                        defaultValue: "SSF_1_0",
                      }}
                      options={[
                        { key: "SSF_1_0", value: t("ssfProfile.SSF_1_0") },
                        {
                          key: "SSE_CAEP",
                          value: t("ssfProfile.SSE_CAEP"),
                        },
                      ]}
                    />
                    <TextAreaControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.description",
                      )}
                      label={t("ssfDescription")}
                      labelIcon={t("ssfDescriptionHelp")}
                      rules={{
                        maxLength: {
                          value: 255,
                          message: t("maxLength", { length: 255 }),
                        },
                      }}
                    />
                    <TextControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.streamAudience",
                      )}
                      label={t("ssfStreamAudience")}
                      labelIcon={t("ssfStreamAudienceHelp")}
                    />
                    <SelectControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.defaultSubjects",
                      )}
                      label={t("ssfDefaultSubjects")}
                      labelIcon={t("ssfDefaultSubjectsHelp")}
                      controller={{
                        defaultValue: "NONE",
                      }}
                      options={[
                        {
                          key: "ALL",
                          value: t("ssfDefaultSubjects.ALL"),
                        },
                        {
                          key: "NONE",
                          value: t("ssfDefaultSubjects.NONE"),
                        },
                      ]}
                    />
                    <SelectControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.userSubjectFormat",
                      )}
                      label={t("ssfUserSubjectFormat")}
                      labelIcon={t("ssfUserSubjectFormatHelp")}
                      controller={{
                        defaultValue: defaultUserSubjectFormat,
                      }}
                      options={[
                        {
                          key: "iss_sub",
                          value: t("ssfUserSubjectFormat.iss_sub"),
                        },
                        {
                          key: "email",
                          value: t("ssfUserSubjectFormat.email"),
                        },
                        {
                          key: "complex.iss_sub+tenant",
                          value: t(
                            "ssfUserSubjectFormat.complex.iss_sub+tenant",
                          ),
                        },
                        {
                          key: "complex.email+tenant",
                          value: t("ssfUserSubjectFormat.complex.email+tenant"),
                        },
                      ]}
                    />
                    <DefaultSwitchControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.autoNotifyOnLogin",
                      )}
                      label={t("ssfAutoNotifyOnLogin")}
                      labelIcon={t("ssfAutoNotifyOnLoginHelp")}
                      stringify
                    />
                    <FormGroup
                      label={t("ssfSubjectRemovalGrace")}
                      fieldId="ssfSubjectRemovalGrace"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfSubjectRemovalGraceHelp")}
                          fieldLabelId="ssfSubjectRemovalGrace"
                        />
                      }
                    >
                      <Controller
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.subjectRemovalGraceSeconds",
                        )}
                        defaultValue=""
                        control={control}
                        render={({ field }) => (
                          <TimeSelector
                            data-testid="ssfSubjectRemovalGrace"
                            value={field.value!}
                            onChange={field.onChange}
                            units={["second", "minute", "hour", "day"]}
                          />
                        )}
                      />
                    </FormGroup>
                    <DefaultSwitchControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.autoVerifyStream",
                      )}
                      label={t("ssfAutoVerifyStream")}
                      labelIcon={t("ssfAutoVerifyStreamHelp")}
                      stringify
                    />
                    {ssfAutoVerifyStream === "true" && (
                      <NumberControl
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.verificationDelayMillis",
                        )}
                        label={t("ssfVerificationDelay")}
                        labelIcon={t("ssfVerificationDelayHelp")}
                        controller={{
                          defaultValue: 1500,
                          rules: {
                            min: 0,
                          },
                        }}
                      />
                    )}
                    <NumberControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.minVerificationInterval",
                      )}
                      label={t("ssfMinVerificationInterval")}
                      labelIcon={t("ssfMinVerificationIntervalHelp")}
                      controller={{
                        defaultValue: "",
                        rules: {
                          min: 0,
                        },
                      }}
                    />
                    <FormGroup
                      label={t("ssfMaxEventAge")}
                      fieldId="ssfMaxEventAge"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfMaxEventAgeHelp")}
                          fieldLabelId="ssfMaxEventAge"
                        />
                      }
                    >
                      <Controller
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.maxEventAgeSeconds",
                        )}
                        defaultValue=""
                        control={control}
                        render={({ field }) => (
                          <TimeSelector
                            data-testid="ssfMaxEventAge"
                            value={field.value!}
                            onChange={field.onChange}
                            units={["second", "minute", "hour", "day"]}
                          />
                        )}
                      />
                    </FormGroup>
                    <FormGroup
                      label={t("ssfInactivityTimeout")}
                      fieldId="ssfInactivityTimeout"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfInactivityTimeoutHelp")}
                          fieldLabelId="ssfInactivityTimeout"
                        />
                      }
                    >
                      <Controller
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.inactivityTimeoutSeconds",
                        )}
                        defaultValue=""
                        control={control}
                        render={({ field }) => (
                          <TimeSelector
                            data-testid="ssfInactivityTimeout"
                            value={field.value!}
                            onChange={field.onChange}
                            units={["minute", "hour", "day"]}
                          />
                        )}
                      />
                    </FormGroup>
                  </div>
                ),
              },
              {
                title: t("ssfSectionAuthentication"),
                panel: (
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      rowGap: "1.5rem",
                    }}
                  >
                    <Text className="pf-v5-u-pb-lg">
                      {t("ssfSectionAuthenticationHelp")}
                    </Text>
                    <DefaultSwitchControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.requireServiceAccount",
                      )}
                      label={t("ssfRequireServiceAccount")}
                      labelIcon={t("ssfRequireServiceAccountHelp")}
                      stringify
                    />
                    <FormGroup
                      label={t("ssfRequiredRole")}
                      fieldId="ssfRequiredRole"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfRequiredRoleHelp")}
                          fieldLabelId="ssfRequiredRole"
                        />
                      }
                    >
                      <Controller
                        name={requiredRoleFieldName}
                        defaultValue=""
                        control={control}
                        render={({ field }) => (
                          <Split>
                            {rolePickerOpen && (
                              <AddRoleMappingModal
                                id="ssfRequiredRolePicker"
                                type="roles"
                                filterType={roleFilterType}
                                name="ssfRequiredRole"
                                onAssign={(rows) => {
                                  const row = rows[0];
                                  const value = row.client?.clientId
                                    ? `${row.client.clientId}.${row.role.name}`
                                    : row.role.name;
                                  field.onChange(value);
                                  setRolePickerOpen(false);
                                }}
                                onClose={() => setRolePickerOpen(false)}
                                isRadio
                              />
                            )}
                            {field.value && field.value !== "" && (
                              <SplitItem>
                                <Chip
                                  textMaxWidth="500px"
                                  onClick={() => field.onChange("")}
                                >
                                  <ServiceRole
                                    role={{
                                      name: parseRoleValue(field.value)[1],
                                    }}
                                    client={{
                                      clientId: parseRoleValue(field.value)[0],
                                    }}
                                  />
                                </Chip>
                              </SplitItem>
                            )}
                            <SplitItem>
                              <AddRoleButton
                                label="selectRole.label"
                                onFilerTypeChange={(type) => {
                                  setRoleFilterType(type);
                                  setRolePickerOpen(true);
                                }}
                                variant="secondary"
                                data-testid="ssfRequiredRoleSelect"
                                isDisabled={false}
                              />
                            </SplitItem>
                          </Split>
                        )}
                      />
                    </FormGroup>
                  </div>
                ),
              },
              {
                title: t("ssfSectionDelivery"),
                panel: (
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      rowGap: "1.5rem",
                    }}
                  >
                    <Text className="pf-v5-u-pb-lg">
                      {t("ssfSectionDeliveryHelp")}
                    </Text>
                    <FormGroup
                      label={t("ssfAllowedDeliveryMethods")}
                      fieldId="ssfAllowedDeliveryMethods"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfAllowedDeliveryMethodsHelp")}
                          fieldLabelId="ssfAllowedDeliveryMethods"
                        />
                      }
                    >
                      <Stack hasGutter>
                        <StackItem>
                          <Checkbox
                            id="ssfAllowedDeliveryMethodsPush"
                            data-testid="ssfAllowedDeliveryMethods.push"
                            label={t("ssfAllowedDeliveryMethods.push")}
                            description={t(
                              "ssfAllowedDeliveryMethods.pushHelp",
                            )}
                            isChecked={pushDeliveryAllowed}
                            onChange={(_event, checked) =>
                              updateAllowedDeliveryMethod("push", checked)
                            }
                          />
                        </StackItem>
                        <StackItem>
                          <Checkbox
                            id="ssfAllowedDeliveryMethodsPoll"
                            data-testid="ssfAllowedDeliveryMethods.poll"
                            label={t("ssfAllowedDeliveryMethods.poll")}
                            description={t(
                              "ssfAllowedDeliveryMethods.pollHelp",
                            )}
                            isChecked={pollDeliveryAllowed}
                            onChange={(_event, checked) =>
                              updateAllowedDeliveryMethod("poll", checked)
                            }
                          />
                        </StackItem>
                      </Stack>
                    </FormGroup>
                    {pushDeliveryAllowed && (
                      <FormGroup
                        label={t("ssfValidPushUrls")}
                        fieldId="ssfValidPushUrls"
                        labelIcon={
                          <HelpItem
                            helpText={t("ssfValidPushUrlsHelp")}
                            fieldLabelId="ssfValidPushUrls"
                          />
                        }
                      >
                        <MultiLineInput
                          id="ssfValidPushUrls"
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.validPushUrls",
                          )}
                          aria-label={t("ssfValidPushUrls")}
                          addButtonLabel="ssfValidPushUrls.add"
                          stringify
                        />
                      </FormGroup>
                    )}
                  </div>
                ),
              },
              {
                title: t("ssfSectionEvents"),
                panel: (
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      rowGap: "1.5rem",
                    }}
                  >
                    <Text className="pf-v5-u-pb-lg">
                      {t("ssfSectionEventsHelp")}
                    </Text>
                    <FormGroup
                      label={t("ssfSupportedEvents")}
                      fieldId="ssfSupportedEvents"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfSupportedEventsHelp")}
                          fieldLabelId="ssfSupportedEvents"
                        />
                      }
                    >
                      <KeycloakSelect
                        toggleId="ssfSupportedEvents"
                        data-testid="ssfSupportedEvents"
                        variant={SelectVariant.typeaheadMulti}
                        chipGroupProps={{
                          numChips: 5,
                          expandedText: t("hide"),
                          collapsedText: t("showRemaining"),
                        }}
                        typeAheadAriaLabel={t("ssfSupportedEvents")}
                        onToggle={setSupportedEventsOpen}
                        isOpen={supportedEventsOpen}
                        selections={supportedEventsSelected}
                        onSelect={(value) => {
                          const option = value.toString();
                          if (!option) return;
                          const next = supportedEventsSelected.includes(option)
                            ? supportedEventsSelected.filter(
                                (s) => s !== option,
                              )
                            : [...supportedEventsSelected, option];
                          setValue(
                            convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.supportedEvents",
                            ),
                            next.join(","),
                            { shouldDirty: true },
                          );
                          setSupportedEventsFilter("");
                        }}
                        onClear={() => {
                          setValue(
                            convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.supportedEvents",
                            ),
                            "",
                            { shouldDirty: true },
                          );
                          setSupportedEventsFilter("");
                        }}
                        onFilter={setSupportedEventsFilter}
                      >
                        {availableSupportedEvents
                          .filter((event) =>
                            event
                              .toLowerCase()
                              .includes(supportedEventsFilter.toLowerCase()),
                          )
                          .map((event) => (
                            <SelectOption key={event} value={event}>
                              {event}
                              {nativelyEmittedEvents.includes(event) && (
                                <Label
                                  color="blue"
                                  isCompact
                                  className="pf-v5-u-ml-sm"
                                >
                                  {t("ssfNativelyEmittedBadge")}
                                </Label>
                              )}
                            </SelectOption>
                          ))}
                      </KeycloakSelect>
                    </FormGroup>
                    <DefaultSwitchControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.allowEmitEvents",
                      )}
                      label={t("ssfAllowEmitEvents")}
                      labelIcon={t("ssfAllowEmitEventsHelp")}
                      stringify
                    />
                    <FormGroup
                      label={t("ssfEmitOnlyEvents")}
                      fieldId="ssfEmitOnlyEvents"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfEmitOnlyEventsHelp")}
                          fieldLabelId="ssfEmitOnlyEvents"
                        />
                      }
                    >
                      <KeycloakSelect
                        toggleId="ssfEmitOnlyEvents"
                        data-testid="ssfEmitOnlyEvents"
                        variant={SelectVariant.typeaheadMulti}
                        chipGroupProps={{
                          numChips: 5,
                          expandedText: t("hide"),
                          collapsedText: t("showRemaining"),
                        }}
                        typeAheadAriaLabel={t("ssfEmitOnlyEvents")}
                        onToggle={setEmitOnlyEventsOpen}
                        isOpen={emitOnlyEventsOpen}
                        selections={emitOnlyEventsSelected}
                        onSelect={(value) => {
                          const option = value.toString();
                          if (!option) return;
                          const next = emitOnlyEventsSelected.includes(option)
                            ? emitOnlyEventsSelected.filter((s) => s !== option)
                            : [...emitOnlyEventsSelected, option];
                          setValue(
                            convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.emitOnlyEvents",
                            ),
                            next.join(","),
                            { shouldDirty: true },
                          );
                          setEmitOnlyEventsFilter("");
                        }}
                        onClear={() => {
                          setValue(
                            convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.emitOnlyEvents",
                            ),
                            "",
                            { shouldDirty: true },
                          );
                          setEmitOnlyEventsFilter("");
                        }}
                        onFilter={setEmitOnlyEventsFilter}
                        isDisabled={supportedEventsSelected.length === 0}
                      >
                        {supportedEventsSelected
                          .filter((event) =>
                            event
                              .toLowerCase()
                              .includes(emitOnlyEventsFilter.toLowerCase()),
                          )
                          .map((event) => (
                            <SelectOption key={event} value={event}>
                              {event}
                            </SelectOption>
                          ))}
                      </KeycloakSelect>
                    </FormGroup>
                    {String(ssfAllowEmitEvents) === "true" && (
                      <FormGroup
                        label={t("ssfEmitEventsRole")}
                        fieldId="ssfEmitEventsRole"
                        labelIcon={
                          <HelpItem
                            helpText={t("ssfEmitEventsRoleHelp")}
                            fieldLabelId="ssfEmitEventsRole"
                          />
                        }
                      >
                        <Controller
                          name={emitEventsRoleFieldName}
                          defaultValue=""
                          control={control}
                          render={({ field }) => (
                            <Split>
                              {emitRolePickerOpen && (
                                <AddRoleMappingModal
                                  id="ssfEmitEventsRolePicker"
                                  type="roles"
                                  filterType={emitRoleFilterType}
                                  name="ssfEmitEventsRole"
                                  onAssign={(rows) => {
                                    const row = rows[0];
                                    const value = row.client?.clientId
                                      ? `${row.client.clientId}.${row.role.name}`
                                      : row.role.name;
                                    field.onChange(value);
                                    setEmitRolePickerOpen(false);
                                  }}
                                  onClose={() => setEmitRolePickerOpen(false)}
                                  isRadio
                                />
                              )}
                              {field.value && field.value !== "" && (
                                <SplitItem>
                                  <Chip
                                    textMaxWidth="500px"
                                    onClick={() => field.onChange("")}
                                  >
                                    <ServiceRole
                                      role={{
                                        name: parseRoleValue(field.value)[1],
                                      }}
                                      client={{
                                        clientId: parseRoleValue(
                                          field.value,
                                        )[0],
                                      }}
                                    />
                                  </Chip>
                                </SplitItem>
                              )}
                              <SplitItem>
                                <AddRoleButton
                                  label="selectRole.label"
                                  onFilerTypeChange={(type) => {
                                    setEmitRoleFilterType(type);
                                    setEmitRolePickerOpen(true);
                                  }}
                                  variant="secondary"
                                  data-testid="ssfEmitEventsRoleSelect"
                                  isDisabled={false}
                                />
                              </SplitItem>
                            </Split>
                          )}
                        />
                      </FormGroup>
                    )}
                  </div>
                ),
              },
            ]}
          />
          <ActionGroup>
            <Button
              variant="secondary"
              onClick={() => save()}
              data-testid="ssfReceiverSave"
            >
              {t("save")}
            </Button>
            <Button
              variant="link"
              onClick={reset}
              data-testid="ssfReceiverRevert"
            >
              {t("revert")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </CardBody>
    </Card>
  );
};
