import type EventHookProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/EventHookProviderRepresentation";
import type EventHookTargetRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTargetRepresentation";
import type EventHookTestResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTestResultRepresentation";
import { HelpItem, KeycloakSelect, TextControl, useAlerts, useEnvironment } from "@keycloak/keycloak-ui-shared";
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
import { addTrailingSlash, convertFormValuesToObject, convertToFormValues } from "../../util";

const REDACTED_SECRET_VALUE = "********";
const eventFilterProperty = {
    name: "events",
    label: "eventHookTargetEvents",
    helpText: "eventHookTargetEventsHelp",
    type: "MultivaluedString",
    defaultValue: "*",
};

const isTruthyValue = (value: unknown) => value === true || value === "true";
const hasConfiguredSecret = (value: unknown) =>
    typeof value === "string" ? value.trim().length > 0 : Boolean(value);

type EventHookTargetForm = {
    name?: string;
    type?: string;
    enabled?: boolean;
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
    const { realm } = useRealm();
    const { t } = useTranslation();
    const { addAlert, addError } = useAlerts();
    const [providerSelectOpen, setProviderSelectOpen] = useState(false);
    const [deliveryModeOpen, setDeliveryModeOpen] = useState(false);
    const [isTesting, setIsTesting] = useState(false);
    const [pendingSubmitValues, setPendingSubmitValues] = useState<EventHookTargetForm>();
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
    const configMetadata = selectedProvider?.configMetadata || [];
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
    const isPullTarget = selectedProvider?.id === "pull";
    const isBulkDelivery = deliveryMode === "BULK";
    const isRetryEnabled = supportsRetry && (retryEnabled === undefined || isTruthyValue(retryEnabled));
    const isHmacConfigured = isTruthyValue(hmacEnabled);
    const isPullSecretConfigured = hasConfiguredSecret(pullSecret);
    const pullPreviewUrl = useMemo(() => {
        if (!isPullTarget) {
            return undefined;
        }

        return `${addTrailingSlash(environment.serverBaseUrl)}realms/${realm}/event-hooks/${target?.id || "{targetId}"}/consume`;
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
        } else if (!supportsAggregation) {
            delete settings.aggregationTimeoutMs;
        }

        if (!supportsRetry) {
            delete settings.retryEnabled;
            delete settings.maxAttempts;
            delete settings.retryDelayMs;
        } else if (!isTruthyValue(config?.retryEnabled ?? true)) {
            delete settings.maxAttempts;
            delete settings.retryDelayMs;
        }

        if (selectedProvider?.id === "http") {
            if (!isTruthyValue(config?.hmacEnabled)) {
                delete settings.hmacAlgorithm;
                delete settings.hmacSecret;
            }
            delete settings["hmacEnabled"];
        }

        return settings;
    };

    const toPayload = (values: EventHookTargetForm): EventHookTargetRepresentation => {
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

    const testFailureMessage = (result: EventHookTestResultRepresentation) => {
        const parts = [result.statusCode, result.details].filter(
            (value): value is string => Boolean(value && value.trim()),
        );

        return parts.join(" - ");
    };

    useEffect(() => {
        if (target) {
            convertToFormValues(
                {
                    ...target,
                    config: target.settings || {},
                },
                setValue,
            );
        } else {
            reset({ enabled: true });
        }
    }, [target, providers, reset, setValue]);

    const persistTarget = async (values: EventHookTargetForm) => {
        const payload = toPayload(values);

        try {
            if (target?.id) {
                await adminClient.eventHooks.updateTarget({
                    realm,
                    targetId: target.id,
                }, payload);
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

    const onTest = async (values: EventHookTargetForm) => {
        setIsTesting(true);

        try {
            const result = await adminClient.eventHooks.testTarget({
                realm,
                ...toPayload(values),
            });
            const details = testFailureMessage(result);

            addAlert(
                result.success
                    ? t("eventHookTargetTestSucceeded")
                    : details
                        ? t("eventHookTargetTestFailed", { details })
                        : t("eventHookTargetTestFailedWithoutDetails"),
                result.success ? AlertVariant.success : AlertVariant.danger,
            );
        } catch (error) {
            addError("eventHookTargetTestError", error);
        } finally {
            setIsTesting(false);
        }
    };

    const renderHttpField = (name: string) => {
        const property = metadataByName[name];

        return property ? <DynamicComponents stringify properties={[property]} /> : null;
    };

    const renderHttpConfig = () => (
        <>
            <Grid hasGutter>
                <GridItem lg={4} sm={12}>
                    {renderHttpField("method")}
                </GridItem>
                <GridItem lg={8} sm={12}>
                    {renderHttpField("url")}
                </GridItem>
            </Grid>
            {renderHttpField("headers")}
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
                        {renderHttpField("hmacAlgorithm")}
                    </GridItem>
                    <GridItem lg={8} sm={12}>
                        {renderHttpField("hmacSecret")}
                    </GridItem>
                </Grid>
            )}
            <Grid hasGutter>
                <GridItem lg={6} sm={12}>
                    {renderHttpField("connectTimeoutMs")}
                </GridItem>
                <GridItem lg={6} sm={12}>
                    {renderHttpField("readTimeoutMs")}
                </GridItem>
            </Grid>
        </>
    );

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
                                isChecked={field.value === undefined ? true : isTruthyValue(field.value)}
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
                                selections={(field.value as DeliveryMode | undefined) || "SINGLE"}
                                variant="single"
                            >
                                <SelectOption value="SINGLE">{t("SINGLE")}</SelectOption>
                                <SelectOption value="BULK">{t("BULK")}</SelectOption>
                            </KeycloakSelect>
                        )}
                    />
                </FormGroup>
                {isBulkDelivery && (
                    <>
                        {supportsAggregation ? (
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
                        )}
                    </>
                )}
            </>
        );
    };

    return (
        <>
            <Modal
                variant={ModalVariant.medium}
                title={t(target ? "editEventHookTarget" : "createEventHookTarget")}
                onClose={onClose}
                isOpen
                actions={[
                    <Button
                        key="test"
                        variant={ButtonVariant.secondary}
                        onClick={handleSubmit(onTest)}
                        isDisabled={!isValid}
                        isLoading={isTesting}
                        spinnerAriaValueText={t("testingEventHookTarget")}
                    >
                        {t("testEventHookTarget")}
                    </Button>,
                    <Button
                        key="save"
                        type="submit"
                        form="event-hook-target-form"
                        isDisabled={!isValid || isTesting}
                    >
                        {t("save")}
                    </Button>,
                    <Button
                        key="cancel"
                        variant={ButtonVariant.link}
                        onClick={onClose}
                        isDisabled={isTesting}
                    >
                        {t("cancel")}
                    </Button>,
                ]}
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
                                <FormGroup label={t("provider")} fieldId="event-hook-target-type">
                                    <Controller
                                        name="type"
                                        control={control}
                                        rules={{ required: t("required") }}
                                        render={({ field }) => (
                                            <KeycloakSelect
                                                toggleId="event-hook-target-type"
                                                onToggle={() => setProviderSelectOpen(!providerSelectOpen)}
                                                onSelect={(value) => {
                                                    const provider = providers.find(({ id }) => id === value);
                                                    if (!target?.id) {
                                                        reset({
                                                            enabled: enabled ?? true,
                                                            name: targetName,
                                                            type: value as string,
                                                            config: {
                                                                ...(provider?.supportsBatch ? { deliveryMode: "SINGLE" } : {}),
                                                                ...(provider?.supportsRetry === false ? {} : { retryEnabled: true }),
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
                                                        {provider.id}
                                                    </SelectOption>
                                                ))}
                                            </KeycloakSelect>
                                        )}
                                    />
                                </FormGroup>
                            </GridItem>
                            <GridItem lg={4} sm={12}>
                                <FormGroup label={t("status")} fieldId="event-hook-target-enabled">
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
                                {isHttpTarget
                                    ? renderHttpConfig()
                                    : (
                                        <DynamicComponents
                                            stringify
                                            properties={configMetadata}
                                        />
                                    )}
                                {isPullTarget && !isPullSecretConfigured && (
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
                            </>
                        )}
                    </FormProvider>
                </Form>
            </Modal>
            <PullSecretConfirm />
        </>
    );
};
