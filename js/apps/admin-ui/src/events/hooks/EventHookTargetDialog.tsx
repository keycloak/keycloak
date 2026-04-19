import type EventHookProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/EventHookProviderRepresentation";
import type EventHookTargetRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTargetRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  TextAreaControl,
  TextControl,
  useAlerts,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  ClipboardCopy,
  Divider,
  Form,
  FormGroup,
  Grid,
  GridItem,
  Modal,
  ModalVariant,
  SelectOption,
  Switch,
} from "@patternfly/react-core";
import { useEffect, useMemo, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import {
  addTrailingSlash,
  convertFormValuesToObject,
  convertToFormValues,
} from "../../util";
import useLocaleSort from "../../utils/useLocaleSort";
import { EventHookTargetTestDialog } from "./EventHookTargetTestDialog";
import { EventHookTargetTypeIcon } from "./EventHookTargetTypeIcon";

const REDACTED_SECRET_VALUE = "********";
const DEFAULT_MAX_EVENTS_PER_BATCH = 1000;
const DEFAULT_AGGREGATION_TIMEOUT_MS = 5000;
const DEFAULT_MAX_ATTEMPTS = 5;
const DEFAULT_RETRY_DELAY_MS = 5000;
const DEFAULT_EMAIL_RECIPIENT_TEMPLATE = '${(user.email)!""}';
const DEFAULT_EMAIL_LOCALE_TEMPLATE_PLACEHOLDER =
  '${(event.details.locale)!"en"}';
const DEFAULT_EMAIL_SUBJECT_TEMPLATE = [
  "<#if event??>",
  '${event.eventType!event.operationType!"UNKNOWN"} for ${(user.username)!(event.userId!"unknown user")}',
  "<#else>",
  "${events?size} grouped events",
  "</#if>",
].join("\n");
const DEFAULT_EMAIL_TEXT_TEMPLATE = [
  "<#if event??>",
  'Event ${event.eventType!event.operationType!"UNKNOWN"} for ${(user.username)!(event.userId!"unknown user")}',
  "",
  "<#if event.details?? && event.details?size gt 0>",
  "Details:",
  "<#list event.details?keys as key>",
  "- ${key}: ${event.details[key]}",
  "</#list>",
  "</#if>",
  "<#else>",
  "${events?size} events were grouped for delivery.",
  "",
  "<#list events as currentEvent>",
  '- ${currentEvent.eventType!currentEvent.operationType!"UNKNOWN"} for ${currentEvent.userId!"unknown user"}',
  "</#list>",
  "</#if>",
].join("\n");
const DEFAULT_EMAIL_HTML_TEMPLATE = [
  "<#if event??>",
  '<p><strong>${event.eventType!event.operationType!"UNKNOWN"}</strong> for ${(user.username)!(event.userId!"unknown user")}</p>',
  "<#if event.details?? && event.details?size gt 0>",
  "<ul>",
  "  <#list event.details?keys as key>",
  "    <li><strong>${key}:</strong> ${event.details[key]}</li>",
  "  </#list>",
  "</ul>",
  "</#if>",
  "<#else>",
  "<p>${events?size} events were grouped for delivery.</p>",
  "<ul>",
  "  <#list events as currentEvent>",
  '    <li>${currentEvent.eventType!currentEvent.operationType!"UNKNOWN"} for ${currentEvent.userId!"unknown user"}</li>',
  "  </#list>",
  "</ul>",
  "</#if>",
].join("\n");
const baseEventFilterProperty = {
  name: "events",
  label: "eventHookTargetEvents",
  helpText: "eventHookTargetEventsHelp",
  type: "MultivaluedString",
  defaultValue: "*",
};

const isTruthyValue = (value: unknown) => value === true || value === "true";
const hasConfiguredSecret = (value: unknown) =>
  typeof value === "string" ? value.trim().length > 0 : Boolean(value);
const isEmptyValue = (value: unknown) =>
  value === undefined || value === null || value === "";

type EventHookTargetForm = {
  name?: string;
  type?: string;
  enabled?: boolean;
  testExampleId?: string;
  config?: Record<string, unknown>;
};

type DeliveryMode = "SINGLE" | "BULK";

type EventHookTargetDialogProps = {
  target?: EventHookTargetRepresentation;
  providers: EventHookProviderRepresentation[];
  onClose: () => void;
  onSaved: () => void;
};

export const EventHookTargetDialog = ({
  target,
  providers,
  onClose,
  onSaved,
}: EventHookTargetDialogProps) => {
  const { adminClient } = useAdminClient();
  const { environment } = useEnvironment();
  const { realm, realmRepresentation } = useRealm();
  const serverInfo = useServerInfo();
  const localeSort = useLocaleSort();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const [providerSelectOpen, setProviderSelectOpen] = useState(false);
  const [deliveryModeOpen, setDeliveryModeOpen] = useState(false);
  const [pendingSubmitValues, setPendingSubmitValues] =
    useState<EventHookTargetForm>();
  const [testDialogTarget, setTestDialogTarget] =
    useState<EventHookTargetRepresentation>();
  const form = useForm<EventHookTargetForm>({
    mode: "onChange",
    defaultValues: {
      enabled: true,
    },
  });
  const {
    control,
    handleSubmit,
    reset,
    trigger,
    watch,
    setValue,
    formState: { isValid },
  } = form;

  const targetName = watch("name");
  const enabled = watch("enabled");
  const selectedType = watch("type");
  const selectedProvider = useMemo(
    () => providers.find(({ id }) => id === selectedType),
    [providers, selectedType],
  );
  const configMetadata = useMemo(
    () => selectedProvider?.configMetadata ?? [],
    [selectedProvider],
  );
  const eventFilterOptions = useMemo(
    () => [
      "*",
      ...localeSort(
        serverInfo.enums?.["eventType"] || [],
        (eventType) => eventType,
      ).filter((eventType) => eventType !== "*"),
    ],
    [localeSort, serverInfo.enums],
  );
  const eventFilterProperty = useMemo(
    () => ({
      ...baseEventFilterProperty,
      options: eventFilterOptions,
    }),
    [eventFilterOptions],
  );
  const metadataByName = useMemo(
    () =>
      Object.fromEntries(
        configMetadata
          .filter((property) => property.name)
          .map((property) => [property.name!, property]),
      ),
    [configMetadata],
  );

  const deliveryMode = watch("config.deliveryMode");
  const retryEnabled = watch("config.retryEnabled");
  const hmacEnabled = watch("config.hmacEnabled");
  const pullSecret = watch("config.pullSecret");
  const supportsBatch = Boolean(selectedProvider?.supportsBatch);
  const supportsRetry = selectedProvider?.supportsRetry !== false;
  const supportsAggregation = Boolean(selectedProvider?.supportsAggregation);
  const isHttpTarget = selectedProvider?.id === "http";
  const isEmailTarget = selectedProvider?.id === "email";
  const isPullTarget = selectedProvider?.id === "pull";
  const isBulkDelivery = deliveryMode === "BULK";
  const isRetryEnabled =
    supportsRetry &&
    (retryEnabled === undefined || isTruthyValue(retryEnabled));
  const isHmacConfigured = isTruthyValue(hmacEnabled);
  const customBodyMappingTemplate = watch("config.customBodyMappingTemplate");
  const [customBodyMappingExpanded, setCustomBodyMappingExpanded] =
    useState(false);
  const isCustomBodyMappingEnabled =
    customBodyMappingExpanded ||
    (typeof customBodyMappingTemplate === "string" &&
      customBodyMappingTemplate.trim().length > 0);
  const isPullSecretConfigured = hasConfiguredSecret(pullSecret);
  const canTestTarget =
    Boolean(selectedProvider) && (!isPullTarget || Boolean(target?.id));
  const isRealmEmailConfigured = useMemo(() => {
    const smtpServer = realmRepresentation?.smtpServer;
    if (!smtpServer) {
      return false;
    }

    const host = smtpServer.host;
    const from = smtpServer.from;
    return (
      typeof host === "string" &&
      host.trim().length > 0 &&
      typeof from === "string" &&
      from.trim().length > 0
    );
  }, [realmRepresentation?.smtpServer]);
  const pullPreviewUrl = useMemo(() => {
    if (!isPullTarget) {
      return undefined;
    }

    return `${addTrailingSlash(environment.serverBaseUrl)}realms/${realm}/event-hooks/${target?.id || "{targetId}"}/consume`;
  }, [environment.serverBaseUrl, isPullTarget, realm, target?.id]);
  const pullTestPreviewUrl = useMemo(() => {
    if (!isPullTarget) {
      return undefined;
    }

    return `${addTrailingSlash(environment.serverBaseUrl)}realms/${realm}/event-hooks/${target?.id || "{targetId}"}/test`;
  }, [environment.serverBaseUrl, isPullTarget, realm, target?.id]);

  const requiresPullSecretConfirmation = (values: EventHookTargetForm) =>
    values.type === "pull" && !hasConfiguredSecret(values.config?.pullSecret);

  const submittedSettings = (config?: Record<string, unknown>) => {
    const settings = { ...(config || {}) };
    const secretFields = (selectedProvider?.configMetadata || [])
      .filter((property) => property.secret || property.type === "Password")
      .map((property) => property.name)
      .filter((name): name is string => Boolean(name));

    secretFields.forEach((field) => {
      if (settings[field] === REDACTED_SECRET_VALUE) {
        delete settings[field];
      }
    });

    if (!supportsBatch) {
      delete settings.deliveryMode;
      delete settings.maxEventsPerBatch;
      delete settings.aggregationTimeoutMs;
    } else if (config?.deliveryMode !== "BULK") {
      delete settings.maxEventsPerBatch;
      delete settings.aggregationTimeoutMs;
    } else {
      if (isEmptyValue(settings.maxEventsPerBatch)) {
        settings.maxEventsPerBatch = DEFAULT_MAX_EVENTS_PER_BATCH;
      }

      if (!supportsAggregation) {
        delete settings.aggregationTimeoutMs;
      } else if (isEmptyValue(settings.aggregationTimeoutMs)) {
        settings.aggregationTimeoutMs = DEFAULT_AGGREGATION_TIMEOUT_MS;
      }
    }

    if (!supportsRetry) {
      delete settings.retryEnabled;
      delete settings.maxAttempts;
      delete settings.retryDelayMs;
    } else if (!isTruthyValue(config?.retryEnabled ?? true)) {
      delete settings.maxAttempts;
      delete settings.retryDelayMs;
    } else {
      if (isEmptyValue(settings.maxAttempts)) {
        settings.maxAttempts = DEFAULT_MAX_ATTEMPTS;
      }

      if (isEmptyValue(settings.retryDelayMs)) {
        settings.retryDelayMs = DEFAULT_RETRY_DELAY_MS;
      }
    }

    if (selectedProvider?.id === "http") {
      if (!isTruthyValue(config?.hmacEnabled)) {
        delete settings.hmacAlgorithm;
        delete settings.hmacSecret;
      }
      delete settings["hmacEnabled"];
    }

    if (isEmptyValue(settings.customBodyMappingTemplate)) {
      delete settings.customBodyMappingTemplate;
    }

    delete settings.customBodyMappingEnabled;

    return settings;
  };

  const toPayload = (
    values: EventHookTargetForm,
  ): EventHookTargetRepresentation => {
    const converted = convertFormValuesToObject<
      EventHookTargetForm & { config?: Record<string, unknown> },
      EventHookTargetRepresentation & { config?: Record<string, unknown> }
    >(values);

    return {
      id: target?.id,
      name: converted.name,
      type: converted.type,
      enabled: converted.enabled,
      settings: submittedSettings(converted.config),
    };
  };

  useEffect(() => {
    setCustomBodyMappingExpanded(
      typeof customBodyMappingTemplate === "string" &&
        customBodyMappingTemplate.trim().length > 0,
    );
  }, [customBodyMappingTemplate]);

  useEffect(() => {
    if (target) {
      const provider = providers.find(({ id }) => id === target.type);
      const settings = target.settings || {};

      reset({ enabled: target.enabled ?? true });
      convertToFormValues(
        {
          ...target,
          config: settings,
        },
        setValue,
      );

      if (provider?.supportsBatch) {
        setValue(
          "config.deliveryMode",
          (settings.deliveryMode as DeliveryMode | undefined) || "SINGLE",
        );
        setValue(
          "config.maxEventsPerBatch",
          settings.maxEventsPerBatch ?? DEFAULT_MAX_EVENTS_PER_BATCH,
        );

        if (provider.supportsAggregation) {
          setValue(
            "config.aggregationTimeoutMs",
            settings.aggregationTimeoutMs ?? DEFAULT_AGGREGATION_TIMEOUT_MS,
          );
        }
      }

      if (provider?.supportsRetry !== false) {
        setValue("config.retryEnabled", settings.retryEnabled ?? true);
        setValue(
          "config.maxAttempts",
          settings.maxAttempts ?? DEFAULT_MAX_ATTEMPTS,
        );
        setValue(
          "config.retryDelayMs",
          settings.retryDelayMs ?? DEFAULT_RETRY_DELAY_MS,
        );
      }

      void trigger();
    } else {
      reset({ enabled: true });
    }
  }, [providers, reset, setValue, target, trigger]);

  const persistTarget = async (values: EventHookTargetForm) => {
    const payload = toPayload(values);

    try {
      if (target?.id) {
        await adminClient.eventHooks.updateTarget(
          {
            realm,
            targetId: target.id,
          },
          payload,
        );
        addAlert(t("eventHookTargetUpdated"), AlertVariant.success);
      } else {
        await adminClient.eventHooks.createTarget({
          realm,
          ...payload,
        });
        addAlert(t("eventHookTargetCreated"), AlertVariant.success);
      }
      onSaved();
    } catch (error) {
      addError(
        target?.id
          ? "eventHookTargetUpdateError"
          : "eventHookTargetCreateError",
        error,
      );
    }
  };

  const [togglePullSecretConfirm, PullSecretConfirm] = useConfirmDialog({
    titleKey: "eventHookTargetPullSecretConfirmTitle",
    messageKey: "eventHookTargetPullSecretConfirm",
    onConfirm: () => {
      if (pendingSubmitValues) {
        void persistTarget(pendingSubmitValues);
      }
    },
  });

  const onSubmit = async (values: EventHookTargetForm) => {
    if (requiresPullSecretConfirmation(values)) {
      setPendingSubmitValues(values);
      togglePullSecretConfirm();
      return;
    }

    await persistTarget(values);
  };

  const openTestDialog = (values: EventHookTargetForm) => {
    setTestDialogTarget(toPayload(values));
  };

  const renderConfigField = (name: string) => {
    const property = metadataByName[name];

    return property ? (
      <DynamicComponents stringify properties={[property]} />
    ) : null;
  };

  const renderEmailTextField = (
    name: string,
    placeholder?: string,
    defaultValue?: string,
  ) => {
    const property = metadataByName[name];

    if (!property?.name || !property?.label) {
      return null;
    }

    return (
      <TextControl
        name={`config.${property.name}`}
        label={t(property.label)}
        helperText={property.helpText ? t(property.helpText) : undefined}
        placeholder={placeholder}
        defaultValue={defaultValue}
        rules={property.required ? { required: t("required") } : undefined}
      />
    );
  };

  const renderEmailTextAreaField = (
    name: string,
    rows: number,
    placeholder?: string,
    defaultValue?: string,
  ) => {
    const property = metadataByName[name];

    if (!property?.name || !property?.label) {
      return null;
    }

    return (
      <>
        <TextAreaControl
          name={`config.${property.name}`}
          label={t(property.label)}
          placeholder={placeholder}
          defaultValue={defaultValue}
          rules={property.required ? { required: t("required") } : undefined}
          resizeOrientation="vertical"
          rows={rows}
        />
        {property.helpText && (
          <div className="pf-v5-u-font-size-sm pf-v5-u-color-200 pf-v5-u-mt-xs pf-v5-u-mb-md">
            {t(property.helpText)}
          </div>
        )}
      </>
    );
  };

  // Shared settings blocks used across multiple target types.
  const renderBatchConfig = () => {
    if (!supportsBatch) {
      return null;
    }

    return (
      <>
        <FormGroup
          label={t("eventHookTargetDeliveryMode")}
          fieldId="event-hook-target-delivery-mode"
          labelIcon={
            <HelpItem
              helpText={t("eventHookTargetDeliveryModeHelp")}
              fieldLabelId="eventHookTargetDeliveryMode"
            />
          }
        >
          <Controller
            name="config.deliveryMode"
            control={control}
            render={({ field }) => (
              <KeycloakSelect
                toggleId="event-hook-target-delivery-mode"
                aria-label={t("eventHookTargetDeliveryMode")}
                isOpen={deliveryModeOpen}
                onToggle={() => setDeliveryModeOpen(!deliveryModeOpen)}
                onSelect={(value) => {
                  field.onChange(value as DeliveryMode);
                  setDeliveryModeOpen(false);
                }}
                selections={
                  ((deliveryMode || field.value) as DeliveryMode | undefined) ||
                  "SINGLE"
                }
                variant="single"
              >
                <SelectOption value="SINGLE">{t("SINGLE")}</SelectOption>
                <SelectOption value="BULK">{t("BULK")}</SelectOption>
              </KeycloakSelect>
            )}
          />
        </FormGroup>
        {isBulkDelivery &&
          (supportsAggregation ? (
            <Grid hasGutter>
              <GridItem lg={6} sm={12}>
                <TextControl
                  name="config.maxEventsPerBatch"
                  label={t("eventHookTargetMaxEventsPerBatch")}
                  helperText={t("eventHookTargetMaxEventsPerBatchHelp")}
                  type="number"
                  rules={{ required: t("required") }}
                />
              </GridItem>
              <GridItem lg={6} sm={12}>
                <TextControl
                  name="config.aggregationTimeoutMs"
                  label={t("eventHookTargetAggregationTimeoutMs")}
                  helperText={t("eventHookTargetAggregationTimeoutMsHelp")}
                  type="number"
                  rules={{ required: t("required") }}
                />
              </GridItem>
            </Grid>
          ) : (
            <TextControl
              name="config.maxEventsPerBatch"
              label={t("eventHookTargetMaxEventsPerBatch")}
              helperText={t("eventHookTargetMaxEventsPerBatchHelp")}
              type="number"
              rules={{ required: t("required") }}
            />
          ))}
      </>
    );
  };

  const renderRetryConfig = () => {
    if (!supportsRetry) {
      return null;
    }

    return (
      <>
        <FormGroup
          label={t("eventHookTargetRetry")}
          fieldId="event-hook-target-retry-enabled"
          labelIcon={
            <HelpItem
              helpText={t("eventHookTargetRetryHelp")}
              fieldLabelId="eventHookTargetRetry"
            />
          }
        >
          <Controller
            name="config.retryEnabled"
            control={control}
            render={({ field }) => (
              <Switch
                id="event-hook-target-retry-enabled"
                label={t("enabled")}
                labelOff={t("disabled")}
                isChecked={
                  field.value === undefined ? true : isTruthyValue(field.value)
                }
                onChange={(_, checked) => field.onChange(checked)}
              />
            )}
          />
        </FormGroup>
        {isRetryEnabled && (
          <>
            <TextControl
              name="config.maxAttempts"
              label={t("eventHookTargetMaxAttempts")}
              helperText={t("eventHookTargetMaxAttemptsHelp")}
              type="number"
              rules={{ required: t("required") }}
            />
            <TextControl
              name="config.retryDelayMs"
              label={t("eventHookTargetInitialDelayMs")}
              helperText={t("eventHookTargetInitialDelayMsHelp")}
              type="number"
              rules={{ required: t("required") }}
            />
          </>
        )}
      </>
    );
  };

  const renderBodyMappingConfig = () => {
    if (!metadataByName.customBodyMappingTemplate) {
      return null;
    }

    return (
      <>
        <FormGroup
          label={t("eventHookTargetCustomBodyMapping")}
          fieldId="event-hook-target-custom-body-mapping-enabled"
          labelIcon={
            <HelpItem
              helpText={t("eventHookTargetCustomBodyMappingHelp")}
              fieldLabelId="eventHookTargetCustomBodyMapping"
            />
          }
        >
          <Switch
            id="event-hook-target-custom-body-mapping-enabled"
            label={t("eventHookTargetOptional")}
            labelOff={t("disabled")}
            isChecked={isCustomBodyMappingEnabled}
            onChange={(_, checked) => {
              setCustomBodyMappingExpanded(checked);
              if (!checked) {
                setValue("config.customBodyMappingTemplate", "", {
                  shouldDirty: true,
                  shouldValidate: true,
                });
              }
            }}
          />
        </FormGroup>
        {isCustomBodyMappingEnabled &&
          renderConfigField("customBodyMappingTemplate")}
      </>
    );
  };

  // HTTP-specific settings.
  const renderHttpConfig = () => (
    <>
      <Grid hasGutter>
        <GridItem lg={4} sm={12}>
          {renderConfigField("method")}
        </GridItem>
        <GridItem lg={8} sm={12}>
          {renderConfigField("url")}
        </GridItem>
      </Grid>
      {renderConfigField("headers")}
      <FormGroup
        label={t("eventHookTargetHmac")}
        fieldId="event-hook-target-hmac-enabled"
        labelIcon={
          <HelpItem
            helpText={t("eventHookTargetHmacHelp")}
            fieldLabelId="eventHookTargetHmac"
          />
        }
      >
        <Controller
          name="config.hmacEnabled"
          control={control}
          render={({ field }) => (
            <Switch
              id="event-hook-target-hmac-enabled"
              label={t("eventHookTargetOptional")}
              labelOff={t("disabled")}
              isChecked={isTruthyValue(field.value)}
              onChange={(_, checked) => field.onChange(checked)}
            />
          )}
        />
      </FormGroup>
      {isHmacConfigured && (
        <Grid hasGutter>
          <GridItem lg={4} sm={12}>
            {renderConfigField("hmacAlgorithm")}
          </GridItem>
          <GridItem lg={8} sm={12}>
            {renderConfigField("hmacSecret")}
          </GridItem>
        </Grid>
      )}
      {renderBodyMappingConfig()}
      <Grid hasGutter>
        <GridItem lg={6} sm={12}>
          {renderConfigField("connectTimeoutMs")}
        </GridItem>
        <GridItem lg={6} sm={12}>
          {renderConfigField("readTimeoutMs")}
        </GridItem>
      </Grid>
    </>
  );

  // Email-specific settings and inline guidance.
  const renderEmailConfig = () => (
    <>
      {!isRealmEmailConfigured && (
        <Alert
          isInline
          variant="warning"
          title={t("eventHookTargetEmailSmtpMissingTitle")}
          className="pf-v5-u-mb-md"
        >
          {t("eventHookTargetEmailSmtpMissing")}
        </Alert>
      )}
      <Alert
        isInline
        variant="info"
        title={t("eventHookTargetEmailUsesRealmConfigTitle")}
        className="pf-v5-u-mb-md"
      >
        {t("eventHookTargetEmailUsesRealmConfig")}
      </Alert>
      <div className="pf-v5-u-mb-md">
        <div className="pf-v5-u-font-size-lg pf-v5-u-font-weight-bold pf-v5-u-mb-sm">
          {t("eventHookTargetEmailRoutingSection")}
        </div>
        <div className="pf-v5-u-font-size-sm pf-v5-u-color-200 pf-v5-u-mb-md">
          {t("eventHookTargetEmailRoutingSectionHelp")}
        </div>
      </div>
      <Grid hasGutter>
        <GridItem lg={8} sm={12}>
          {renderEmailTextField(
            "recipientTemplate",
            DEFAULT_EMAIL_RECIPIENT_TEMPLATE,
            DEFAULT_EMAIL_RECIPIENT_TEMPLATE,
          )}
        </GridItem>
        <GridItem lg={4} sm={12}>
          {renderEmailTextField(
            "localeTemplate",
            DEFAULT_EMAIL_LOCALE_TEMPLATE_PLACEHOLDER,
          )}
        </GridItem>
      </Grid>
      <Alert
        isInline
        variant="info"
        title={t("eventHookTargetEmailRecipientHintTitle")}
        className="pf-v5-u-mb-md"
      >
        {t("eventHookTargetEmailRecipientHint")}
      </Alert>
      <Alert
        isInline
        variant="info"
        title={t("eventHookTargetEmailLocalizationTitle")}
        className="pf-v5-u-mb-md"
      >
        {t("eventHookTargetEmailLocalization")}
      </Alert>
      <div className="pf-v5-u-mt-lg pf-v5-u-mb-md">
        <div className="pf-v5-u-font-size-lg pf-v5-u-font-weight-bold pf-v5-u-mb-sm">
          {t("eventHookTargetEmailSubjectSection")}
        </div>
        <div className="pf-v5-u-font-size-sm pf-v5-u-color-200 pf-v5-u-mb-md">
          {t("eventHookTargetEmailSubjectSectionHelp")}
        </div>
      </div>
      {renderEmailTextAreaField(
        "subjectTemplate",
        3,
        DEFAULT_EMAIL_SUBJECT_TEMPLATE,
        DEFAULT_EMAIL_SUBJECT_TEMPLATE,
      )}
      <Alert
        isInline
        variant="info"
        title={t("eventHookTargetEmailTemplateHintTitle")}
        className="pf-v5-u-mb-md"
      >
        {t("eventHookTargetEmailTemplateHint")}
      </Alert>
      <div className="pf-v5-u-mt-lg pf-v5-u-mb-md">
        <div className="pf-v5-u-font-size-lg pf-v5-u-font-weight-bold pf-v5-u-mb-sm">
          {t("eventHookTargetEmailBodiesSection")}
        </div>
        <div className="pf-v5-u-font-size-sm pf-v5-u-color-200 pf-v5-u-mb-md">
          {t("eventHookTargetEmailBodiesSectionHelp")}
        </div>
      </div>
      <div className="pf-v5-u-mb-lg">
        <div className="pf-v5-u-font-size-md pf-v5-u-font-weight-bold pf-v5-u-mb-sm">
          {t("eventHookTargetEmailTextEditorTitle")}
        </div>
        {renderEmailTextAreaField(
          "textBodyTemplate",
          14,
          DEFAULT_EMAIL_TEXT_TEMPLATE,
          DEFAULT_EMAIL_TEXT_TEMPLATE,
        )}
      </div>
      <div>
        <div className="pf-v5-u-font-size-md pf-v5-u-font-weight-bold pf-v5-u-mb-sm">
          {t("eventHookTargetEmailHtmlEditorTitle")}
        </div>
        {renderEmailTextAreaField(
          "htmlBodyTemplate",
          16,
          DEFAULT_EMAIL_HTML_TEMPLATE,
          DEFAULT_EMAIL_HTML_TEMPLATE,
        )}
      </div>
    </>
  );

  // Pull-specific hints and generated endpoint previews.
  const renderPullConfig = () => (
    <>
      {!isPullSecretConfigured && (
        <Alert
          isInline
          variant="warning"
          title={t("eventHookTargetPullSecretWarningTitle")}
        >
          {t("eventHookTargetPullSecretWarning")}
        </Alert>
      )}
      {pullPreviewUrl && (
        <FormGroup
          label={t("eventHookTargetPullUrlPreview")}
          fieldId="event-hook-target-pull-url-preview"
          labelIcon={
            <HelpItem
              helpText={t("eventHookTargetPullUrlPreviewHelp")}
              fieldLabelId="eventHookTargetPullUrlPreview"
            />
          }
        >
          <ClipboardCopy id="event-hook-target-pull-url-preview" isReadOnly>
            {pullPreviewUrl}
          </ClipboardCopy>
        </FormGroup>
      )}
      {pullTestPreviewUrl && (
        <FormGroup
          label={t("eventHookTargetPullTestUrlPreview")}
          fieldId="event-hook-target-pull-test-url-preview"
          labelIcon={
            <HelpItem
              helpText={t("eventHookTargetPullTestUrlPreviewHelp")}
              fieldLabelId="eventHookTargetPullTestUrlPreview"
            />
          }
        >
          <ClipboardCopy
            id="event-hook-target-pull-test-url-preview"
            isReadOnly
          >
            {pullTestPreviewUrl}
          </ClipboardCopy>
        </FormGroup>
      )}
    </>
  );

  return (
    <>
      <Modal
        variant={ModalVariant.medium}
        title={t(target ? "editEventHookTarget" : "createEventHookTarget")}
        onClose={onClose}
        isOpen
        actions={[
          canTestTarget ? (
            <Button
              key="test"
              variant={ButtonVariant.secondary}
              onClick={handleSubmit(openTestDialog)}
              isDisabled={!isValid}
            >
              {t("testEventHookTarget")}
            </Button>
          ) : null,
          <Button
            key="save"
            type="submit"
            form="event-hook-target-form"
            isDisabled={!isValid}
          >
            {t("save")}
          </Button>,
          <Button key="cancel" variant={ButtonVariant.link} onClick={onClose}>
            {t("cancel")}
          </Button>,
        ].filter(Boolean)}
      >
        <Form id="event-hook-target-form" onSubmit={handleSubmit(onSubmit)}>
          <FormProvider {...form}>
            <TextControl
              name="name"
              label={t("name")}
              autoFocus
              rules={{ required: t("required") }}
            />
            <DynamicComponents stringify properties={[eventFilterProperty]} />
            <Grid hasGutter>
              <GridItem lg={8} sm={12}>
                <FormGroup
                  label={t("provider")}
                  fieldId="event-hook-target-type"
                >
                  <Controller
                    name="type"
                    control={control}
                    rules={{ required: t("required") }}
                    render={({ field }) => (
                      <KeycloakSelect
                        toggleId="event-hook-target-type"
                        onToggle={() =>
                          setProviderSelectOpen(!providerSelectOpen)
                        }
                        onSelect={(value) => {
                          const provider = providers.find(
                            ({ id }) => id === value,
                          );
                          if (!target?.id) {
                            reset({
                              enabled: enabled ?? true,
                              name: targetName,
                              type: value as string,
                              config: {
                                ...(provider?.id === "email"
                                  ? {
                                      subjectTemplate:
                                        DEFAULT_EMAIL_SUBJECT_TEMPLATE,
                                      textBodyTemplate:
                                        DEFAULT_EMAIL_TEXT_TEMPLATE,
                                      htmlBodyTemplate:
                                        DEFAULT_EMAIL_HTML_TEMPLATE,
                                    }
                                  : {}),
                                ...(provider?.supportsBatch
                                  ? {
                                      deliveryMode: "SINGLE",
                                      maxEventsPerBatch:
                                        DEFAULT_MAX_EVENTS_PER_BATCH,
                                    }
                                  : {}),
                                ...(provider?.supportsAggregation
                                  ? {
                                      aggregationTimeoutMs:
                                        DEFAULT_AGGREGATION_TIMEOUT_MS,
                                    }
                                  : {}),
                                ...(provider?.supportsRetry === false
                                  ? {}
                                  : {
                                      retryEnabled: true,
                                      maxAttempts: DEFAULT_MAX_ATTEMPTS,
                                      retryDelayMs: DEFAULT_RETRY_DELAY_MS,
                                    }),
                              },
                            });
                          } else {
                            field.onChange(value as string);
                          }
                          setProviderSelectOpen(false);
                        }}
                        selections={field.value}
                        variant="single"
                        aria-label={t("provider")}
                        placeholderText={t("selectEventHookTargetType")}
                        isOpen={providerSelectOpen}
                        isDisabled={Boolean(target?.id)}
                      >
                        {providers.map((provider) => (
                          <SelectOption key={provider.id} value={provider.id}>
                            <span className="pf-v5-u-display-flex pf-v5-u-align-items-center pf-v5-u-gap-sm">
                              <EventHookTargetTypeIcon type={provider.id} />
                              <span>{provider.id}</span>
                            </span>
                          </SelectOption>
                        ))}
                      </KeycloakSelect>
                    )}
                  />
                </FormGroup>
              </GridItem>
              <GridItem lg={4} sm={12}>
                <FormGroup
                  label={t("status")}
                  fieldId="event-hook-target-enabled"
                >
                  <Controller
                    name="enabled"
                    control={control}
                    render={({ field }) => (
                      <Switch
                        id="event-hook-target-enabled"
                        label={t("enabled")}
                        labelOff={t("disabled")}
                        isChecked={field.value ?? false}
                        onChange={(_, checked) => field.onChange(checked)}
                      />
                    )}
                  />
                </FormGroup>
              </GridItem>
            </Grid>
            {selectedProvider && (
              <>
                {renderBatchConfig()}
                {renderRetryConfig()}
                <div className="pf-v5-u-mt-md pf-v5-u-mb-md">
                  <div className="pf-v5-u-font-size-sm pf-v5-u-color-200 pf-v5-u-mb-sm">
                    {t("eventHookTargetTypeOptions")}
                  </div>
                  <Divider />
                </div>
                {isHttpTarget ? (
                  renderHttpConfig()
                ) : isEmailTarget ? (
                  renderEmailConfig()
                ) : (
                  <>
                    <DynamicComponents
                      stringify
                      properties={configMetadata.filter(
                        ({ name }) => name !== "customBodyMappingTemplate",
                      )}
                    />
                    {isPullTarget && renderBodyMappingConfig()}
                  </>
                )}
                {isPullTarget && renderPullConfig()}
              </>
            )}
          </FormProvider>
        </Form>
      </Modal>
      {testDialogTarget && (
        <EventHookTargetTestDialog
          target={testDialogTarget}
          onClose={() => setTestDialogTarget(undefined)}
        />
      )}
      <PullSecretConfirm />
    </>
  );
};
