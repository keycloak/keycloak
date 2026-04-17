import type EventHookTestExampleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTestExampleRepresentation";
import type EventHookTargetRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTargetRepresentation";
import type EventHookTestResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTestResultRepresentation";
import { HelpItem, KeycloakSelect, KeycloakSpinner, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
    Alert,
    AlertVariant,
    Button,
    ButtonVariant,
    FormGroup,
    Modal,
    ModalVariant,
    SelectOption,
} from "@patternfly/react-core";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";

type EventHookTargetTestDialogProps = {
    target: EventHookTargetRepresentation;
    onClose: () => void;
};

type TestResultState = {
    message: string;
    variant: AlertVariant;
};

export const EventHookTargetTestDialog = ({
    target,
    onClose,
}: EventHookTargetTestDialogProps) => {
    const { adminClient } = useAdminClient();
    const { realm } = useRealm();
    const { t } = useTranslation();
    const { addError } = useAlerts();
    const [testExampleOpen, setTestExampleOpen] = useState(false);
    const [isLoadingExamples, setIsLoadingExamples] = useState(true);
    const [isTesting, setIsTesting] = useState(false);
    const [testExamples, setTestExamples] = useState<EventHookTestExampleRepresentation[]>([]);
    const [selectedTestExampleId, setSelectedTestExampleId] = useState<string>();
    const [result, setResult] = useState<TestResultState>();

    const selectedTestExample = useMemo(
        () => testExamples.find((example) => example.id === selectedTestExampleId),
        [selectedTestExampleId, testExamples],
    );

    const formatTestExampleLabel = (example: EventHookTestExampleRepresentation) =>
        `${example.sourceType}: ${example.eventName}`;

    const testFailureMessage = (testResult: EventHookTestResultRepresentation) => {
        const parts = [testResult.statusCode, testResult.details].filter(
            (value): value is string => Boolean(value && value.trim()),
        );

        return parts.join(" - ");
    };

    useEffect(() => {
        let active = true;

        setIsLoadingExamples(true);
        void adminClient.eventHooks.findTestExamples({ realm })
            .then((examples) => {
                if (!active) {
                    return;
                }

                setTestExamples(examples);
                setSelectedTestExampleId((current) =>
                    current && examples.some((example) => example.id === current)
                        ? current
                        : examples[0]?.id,
                );
            })
            .catch((error) => {
                if (active) {
                    addError("eventHookTargetLoadTestExamplesError", error);
                }
            })
            .finally(() => {
                if (active) {
                    setIsLoadingExamples(false);
                }
            });

        return () => {
            active = false;
        };
    }, [addError, adminClient.eventHooks, realm]);

    const runTest = async () => {
        setIsTesting(true);
        setResult(undefined);

        try {
            const testResult = await adminClient.eventHooks.testTarget({
                realm,
                exampleId: selectedTestExampleId,
                ...target,
            });
            const details = testFailureMessage(testResult);

            setResult({
                message: testResult.success
                    ? t("eventHookTargetTestSucceeded")
                    : details
                        ? t("eventHookTargetTestFailed", { details })
                        : t("eventHookTargetTestFailedWithoutDetails"),
                variant: testResult.success ? AlertVariant.success : AlertVariant.danger,
            });
        } catch (error) {
            addError("eventHookTargetTestError", error);
        } finally {
            setIsTesting(false);
        }
    };

    return (
        <Modal
            variant={ModalVariant.medium}
            title={t("testEventHookTarget")}
            isOpen
            onClose={onClose}
            actions={[
                <Button
                    key="run-test"
                    onClick={runTest}
                    isDisabled={isLoadingExamples || isTesting}
                    isLoading={isTesting}
                    spinnerAriaValueText={t("testingEventHookTarget")}
                >
                    {t("testEventHookTarget")}
                </Button>,
                <Button
                    key="close"
                    variant={ButtonVariant.link}
                    onClick={onClose}
                    isDisabled={isTesting}
                >
                    {t("close")}
                </Button>,
            ]}
        >
            <div className="pf-v5-u-mb-md">
                <div className="pf-v5-u-font-weight-bold">{target.name || t("eventHookTargets")}</div>
                {target.type && <div className="pf-v5-u-color-200">{target.type}</div>}
            </div>
            {isLoadingExamples ? (
                <KeycloakSpinner />
            ) : (
                <>
                    {testExamples.length > 0 && (
                        <FormGroup
                            label={t("eventHookTargetTestEventType")}
                            fieldId="event-hook-target-test-example"
                            labelIcon={
                                <HelpItem
                                    helpText={t("eventHookTargetTestEventTypeHelp")}
                                    fieldLabelId="eventHookTargetTestEventType"
                                />
                            }
                        >
                            <KeycloakSelect
                                toggleId="event-hook-target-test-example"
                                aria-label={t("eventHookTargetTestEventType")}
                                isOpen={testExampleOpen}
                                onToggle={() => setTestExampleOpen(!testExampleOpen)}
                                onSelect={(value) => {
                                    setSelectedTestExampleId(value as string);
                                    setTestExampleOpen(false);
                                }}
                                selections={selectedTestExampleId}
                                variant="single"
                            >
                                {testExamples.map((example) => (
                                    <SelectOption key={example.id} value={example.id}>
                                        {formatTestExampleLabel(example)}
                                    </SelectOption>
                                ))}
                            </KeycloakSelect>
                            {selectedTestExample && (
                                <div className="pf-v5-u-color-200 pf-v5-u-font-size-sm pf-v5-u-mt-sm">
                                    {t("eventHookTargetTestEventTypeSelected", {
                                        type: formatTestExampleLabel(selectedTestExample),
                                    })}
                                </div>
                            )}
                        </FormGroup>
                    )}
                    {result && (
                        <Alert
                            isInline
                            className="pf-v5-u-mt-md"
                            title={result.message}
                            variant={result.variant}
                        />
                    )}
                </>
            )}
        </Modal>
    );
};
