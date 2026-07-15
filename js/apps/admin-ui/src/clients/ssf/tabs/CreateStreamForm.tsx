import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { SsfStreamConfigInputRepresentation } from "@keycloak/keycloak-admin-client";
import {
  HelpItem,
  KeycloakSelect,
  PasswordInput,
  SelectControl,
  SelectVariant,
  TextControl,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  InputGroup,
  InputGroupItem,
  Label,
  SelectOption,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import {
  Controller,
  FormProvider,
  useForm,
  useFormContext,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../../admin-client";
import { CopyToClipboardButton } from "../../../components/copy-to-clipboard-button/CopyToClipboardButton";
import { FormAccess } from "../../../components/form/FormAccess";
import {
  DELIVERY_METHOD_POLL_URI,
  DELIVERY_METHOD_PUSH_URI,
  isValidPushEndpointUrl,
} from "../utils";

export type CreateStreamFormFields = {
  profile: "SSF_1_0" | "SSE_CAEP";
  deliveryMethod: "PUSH" | "POLL";
  endpointUrl: string;
  authHeader: string;
  eventsRequested: string[];
  description: string;
};

export const getDefaultCreateStreamValues = (
  profile?: string,
): CreateStreamFormFields => ({
  profile: profile === "SSE_CAEP" ? "SSE_CAEP" : "SSF_1_0",
  deliveryMethod: "PUSH",
  endpointUrl: "",
  authHeader: "",
  eventsRequested: [],
  description: "",
});

export type CreateStreamFormProps = {
  client: ClientRepresentation;
  receiverSupportedEvents: string[];
  nativelyEmittedEvents: string[];
  onCancel: () => void;
  onSuccess: () => void;
};

const CreateStreamFormBody = ({
  client,
  receiverSupportedEvents,
  nativelyEmittedEvents,
  onCancel,
  onSuccess,
}: CreateStreamFormProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const {
    control,
    handleSubmit,
    reset,
    watch,
    trigger,
    clearErrors,
    getValues,
    formState,
  } = useFormContext<CreateStreamFormFields>();

  const [eventsOpen, setEventsOpen] = useState(false);
  const [eventsFilter, setEventsFilter] = useState("");

  const deliveryMethod = watch("deliveryMethod");

  useEffect(() => {
    void trigger();
  }, [trigger]);

  useEffect(() => {
    if (deliveryMethod === "POLL") {
      clearErrors("endpointUrl");
    } else {
      void trigger("endpointUrl");
    }
  }, [clearErrors, deliveryMethod, trigger]);

  const submitCreateStream = async ({
    profile,
    deliveryMethod: method,
    endpointUrl,
    authHeader,
    eventsRequested,
    description,
  }: CreateStreamFormFields) => {
    if (!client.id) {
      return;
    }

    try {
      const savedProfile = client.attributes?.["ssf.profile"] || "SSF_1_0";
      if (profile !== savedProfile) {
        await adminClient.clients.update(
          { id: client.id },
          {
            ...client,
            attributes: {
              ...(client.attributes ?? {}),
              "ssf.profile": profile,
            },
          },
        );
      }

      const delivery: NonNullable<
        SsfStreamConfigInputRepresentation["delivery"]
      > = {
        method:
          method === "POLL"
            ? DELIVERY_METHOD_POLL_URI
            : DELIVERY_METHOD_PUSH_URI,
      };
      if (method === "PUSH") {
        delivery.endpoint_url = endpointUrl.trim();
        if (authHeader.trim()) {
          delivery.authorization_header = authHeader.trim();
        }
      }
      const body: SsfStreamConfigInputRepresentation = { delivery };
      if (eventsRequested.length > 0) {
        body.events_requested = eventsRequested;
      }
      if (description.trim()) {
        body.description = description.trim();
      }

      await adminClient.ssf.createClientStream(
        { clientId: client.clientId! },
        body,
      );
      addAlert(t("ssfCreateStreamSuccess"), AlertVariant.success);
      onSuccess();
    } catch (error) {
      addError("ssfCreateStreamError", error);
    }
  };

  const handleCancel = () => {
    reset(getDefaultCreateStreamValues(client.attributes?.["ssf.profile"]));
    onCancel();
  };

  return (
    <FormAccess
      role="manage-clients"
      fineGrainedAccess={client.access?.configure}
      isHorizontal
    >
      <SelectControl
        id="ssfCreateStreamProfile"
        name="profile"
        label={t("ssfProfile")}
        labelIcon={t("ssfCreateStreamProfileHelp")}
        controller={{
          defaultValue: getDefaultCreateStreamValues(
            client.attributes?.["ssf.profile"],
          ).profile,
        }}
        options={[
          { key: "SSF_1_0", value: t("ssfProfile.SSF_1_0") },
          { key: "SSE_CAEP", value: t("ssfProfile.SSE_CAEP") },
        ]}
      />
      <SelectControl
        id="ssfCreateStreamDeliveryMethod"
        name="deliveryMethod"
        label={t("ssfCreateStreamDeliveryMethod")}
        labelIcon={t("ssfCreateStreamDeliveryMethodHelp")}
        controller={{ defaultValue: "PUSH" }}
        options={[
          { key: "PUSH", value: t("ssfDelivery.PUSH") },
          { key: "POLL", value: t("ssfDelivery.POLL") },
        ]}
      />
      {deliveryMethod === "PUSH" && (
        <>
          <TextControl<CreateStreamFormFields>
            name="endpointUrl"
            label={t("ssfCreateStreamEndpointUrl")}
            labelIcon={t("ssfCreateStreamEndpointUrlHelp")}
            defaultValue=""
            rules={{
              validate: (value) => {
                const endpointUrl = typeof value === "string" ? value : "";
                if (getValues("deliveryMethod") !== "PUSH") {
                  return true;
                }
                if (!endpointUrl.trim()) {
                  return t("ssfCreateStreamEndpointUrlRequired");
                }
                if (!isValidPushEndpointUrl(endpointUrl)) {
                  return t("ssfCreateStreamEndpointUrlInvalid");
                }
                return true;
              },
            }}
            data-testid="ssfCreateStreamEndpointUrl"
          />
          <FormGroup
            label={t("ssfStreamPushAuthHeader")}
            fieldId="ssfCreateStreamAuthHeader"
            labelIcon={
              <HelpItem
                helpText={t("ssfStreamPushAuthHeaderHelp")}
                fieldLabelId="ssfStreamPushAuthHeader"
              />
            }
          >
            <Controller
              name="authHeader"
              defaultValue=""
              control={control}
              render={({ field }) => (
                <InputGroup>
                  <InputGroupItem isFill>
                    <PasswordInput
                      id="ssfCreateStreamAuthHeader"
                      data-testid="ssfCreateStreamAuthHeader"
                      value={field.value}
                      onChange={(event) =>
                        field.onChange((event.target as HTMLInputElement).value)
                      }
                    />
                  </InputGroupItem>
                  <InputGroupItem>
                    <CopyToClipboardButton
                      id="ssfCreateStreamAuthHeader"
                      text={field.value}
                      label="ssfStreamPushAuthHeader"
                      variant="control"
                    />
                  </InputGroupItem>
                </InputGroup>
              )}
            />
          </FormGroup>
        </>
      )}
      <FormGroup
        label={t("ssfCreateStreamEventsRequested")}
        fieldId="ssfCreateStreamEventsRequested"
        labelIcon={
          <HelpItem
            helpText={t("ssfCreateStreamEventsRequestedHelp")}
            fieldLabelId="ssfCreateStreamEventsRequested"
          />
        }
      >
        <Controller
          name="eventsRequested"
          defaultValue={[]}
          control={control}
          render={({ field }) => (
            <KeycloakSelect
              toggleId="ssfCreateStreamEventsRequested"
              data-testid="ssfCreateStreamEventsRequested"
              variant={SelectVariant.typeaheadMulti}
              chipGroupProps={{
                numChips: 5,
                expandedText: t("hide"),
                collapsedText: t("showRemaining"),
              }}
              typeAheadAriaLabel={t("ssfCreateStreamEventsRequested")}
              onToggle={setEventsOpen}
              isOpen={eventsOpen}
              selections={field.value}
              onSelect={(value) => {
                const option = value.toString();
                if (!option) return;
                const next = field.value.includes(option)
                  ? field.value.filter((event) => event !== option)
                  : [...field.value, option];
                field.onChange(next);
                setEventsFilter("");
              }}
              onClear={() => {
                field.onChange([]);
                setEventsFilter("");
              }}
              onFilter={setEventsFilter}
            >
              {receiverSupportedEvents
                .filter((event) =>
                  event.toLowerCase().includes(eventsFilter.toLowerCase()),
                )
                .map((event) => (
                  <SelectOption key={event} value={event}>
                    {event}
                    {nativelyEmittedEvents.includes(event) && (
                      <Label color="blue" isCompact className="pf-v5-u-ml-sm">
                        {t("ssfNativelyEmittedBadge")}
                      </Label>
                    )}
                  </SelectOption>
                ))}
            </KeycloakSelect>
          )}
        />
      </FormGroup>
      <TextControl<CreateStreamFormFields>
        name="description"
        label={t("ssfCreateStreamDescription")}
        labelIcon={t("ssfCreateStreamDescriptionHelp")}
        defaultValue=""
        data-testid="ssfCreateStreamDescription"
      />
      <ActionGroup>
        <Button
          variant="primary"
          isDisabled={!formState.isValid || formState.isSubmitting}
          onClick={() => void handleSubmit(submitCreateStream)()}
          data-testid="ssfCreateStreamSubmit"
        >
          {t("ssfCreateStream")}
        </Button>
        <Button
          variant="link"
          onClick={handleCancel}
          data-testid="ssfCreateStreamCancel"
        >
          {t("cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};

export const CreateStreamForm = (props: CreateStreamFormProps) => {
  const form = useForm<CreateStreamFormFields>({
    mode: "onChange",
    defaultValues: {
      ...getDefaultCreateStreamValues(props.client.attributes?.["ssf.profile"]),
      eventsRequested: [...props.receiverSupportedEvents],
    },
  });

  return (
    <FormProvider {...form}>
      <CreateStreamFormBody {...props} />
    </FormProvider>
  );
};
