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
  Chip,
  FormGroup,
  Label,
  SelectOption,
  Split,
  SplitItem,
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
  const { control, watch } = useFormContext<FormFields>();

  const [supportedEventsOpen, setSupportedEventsOpen] = useState(false);
  const [emitOnlyEventsOpen, setEmitOnlyEventsOpen] = useState(false);

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
                      <Controller
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.supportedEvents",
                        )}
                        control={control}
                        defaultValue={defaultSupportedEvents}
                        render={({ field }) => {
                          const selected = splitSupportedEvents(field.value);
                          return (
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
                              selections={selected}
                              onSelect={(value) => {
                                const option = value.toString();
                                if (!option) return;
                                const next = selected.includes(option)
                                  ? selected.filter((s) => s !== option)
                                  : [...selected, option];
                                field.onChange(next.join(","));
                              }}
                              onClear={() => field.onChange("")}
                            >
                              {availableSupportedEvents.map((event) => (
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
                          );
                        }}
                      />
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
                      <Controller
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.emitOnlyEvents",
                        )}
                        control={control}
                        defaultValue=""
                        render={({ field }) => {
                          const supportedEvents =
                            splitSupportedEvents(ssfSupportedEvents);
                          const selected = splitSupportedEvents(
                            field.value,
                          ).filter((e) => supportedEvents.includes(e));
                          return (
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
                              selections={selected}
                              onSelect={(value) => {
                                const option = value.toString();
                                if (!option) return;
                                const next = selected.includes(option)
                                  ? selected.filter((s) => s !== option)
                                  : [...selected, option];
                                field.onChange(next.join(","));
                              }}
                              onClear={() => field.onChange("")}
                              isDisabled={supportedEvents.length === 0}
                            >
                              {supportedEvents.map((event) => (
                                <SelectOption key={event} value={event}>
                                  {event}
                                </SelectOption>
                              ))}
                            </KeycloakSelect>
                          );
                        }}
                      />
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
