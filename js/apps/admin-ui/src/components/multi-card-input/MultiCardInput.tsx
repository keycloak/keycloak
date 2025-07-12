import {
    Button,
    Card,
    CardBody,
    Stack,
    StackItem,
    TextInput,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import React, { useEffect, useMemo } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type FieldKey = {
    name: string;
    label: string;
};

export type MultiCardInputProps = {
    name: string;
    fieldKeys: FieldKey[];
    requiredFields?: string[];
    isDisabled?: boolean;
    defaultValue?: Record<string, string>[] | string;
    isRequired?: boolean;
    stringify?: boolean;
};

export const MultiCardInput = ({
                                   name,
                                   fieldKeys,
                                   requiredFields = [],
                                   isDisabled = false,
                                   defaultValue,
                                   isRequired = false,
                                   stringify = false,
                               }: MultiCardInputProps) => {
    const { t } = useTranslation();
    const { register, setValue, control } = useFormContext();

    const rawValue = useWatch({
        name,
        control,
        defaultValue,
    });

    const items: Record<string, string>[] = useMemo(() => {
        if (stringify && typeof rawValue === "string") {
            return stringToCardArray(rawValue, fieldKeys);
        }
        if (Array.isArray(rawValue)) {
            return rawValue;
        }
        if (stringify && typeof defaultValue === "string") {
            return stringToCardArray(defaultValue, fieldKeys);
        }
        if (Array.isArray(defaultValue)) {
            return defaultValue;
        }
        return [];
    }, [rawValue, defaultValue, stringify, fieldKeys]);

    const update = (cards: Record<string, string>[]) => {
        const finalValue = stringify ? toStringValue(cards) : cards;
        setValue(name, finalValue, {
            shouldDirty: true,
            shouldValidate: true,
        });
    };

    const updateField = (
        rowIndex: number,
        key: string,
        fieldValue: string
    ) => {
        const updated = [...items];
        updated[rowIndex] = {
            ...updated[rowIndex],
            [key]: fieldValue,
        };
        update(updated);
    };

    const removeItem = (rowIndex: number) => {
        const updated = [...items.slice(0, rowIndex), ...items.slice(rowIndex + 1)];
        update(updated);
    };

    const addItem = () => {
        const emptyItem = Object.fromEntries(fieldKeys.map(({ name }) => [name, ""]));
        update([...items, emptyItem]);
    };

    const canAdd =
        items.length === 0 ||
        requiredFields.every(
            (key) => items[items.length - 1]?.[key]?.trim?.() !== ""
        );

    useEffect(() => {
        register(name, {
            validate: (value) =>
                isRequired &&
                (Array.isArray(value)
                    ? value.length === 0
                    : typeof value === "string" && value.trim() === "")
                    ? t("required")
                    : undefined,
        });
    }, [register, name, isRequired, t]);

    return (
        <div>
            {items.map((item, rowIndex) => (
                <Card key={rowIndex} style={{ marginBottom: "1rem" }}>
                    <CardBody>
                        <Stack hasGutter>
                            {fieldKeys.map(({ name: key, label }) => (
                                <StackItem key={key}>
                                    <label
                                        htmlFor={`${key}-${rowIndex}`}
                                        style={{
                                            display: "block",
                                            fontWeight: 600,
                                            marginBottom: 4,
                                        }}
                                    >
                                        {label}{" "}
                                        {requiredFields.includes(key) && (
                                            <span style={{ color: "red" }}>*</span>
                                        )}
                                    </label>
                                    <TextInput
                                        id={`${key}-${rowIndex}`}
                                        value={item[key] || ""}
                                        onChange={(e) => updateField(rowIndex, key, e.currentTarget.value)}
                                        isDisabled={isDisabled}
                                        aria-label={`${label} - row ${rowIndex + 1}`}
                                    />
                                </StackItem>
                            ))}
                            <StackItem>
                                <Button
                                    variant="plain"
                                    onClick={() => removeItem(rowIndex)}
                                    aria-label={t("delete")}
                                    isDisabled={isDisabled}
                                >
                                    <MinusCircleIcon style={{ marginRight: 4 }} />
                                    {t("delete")}
                                </Button>
                            </StackItem>
                        </Stack>
                    </CardBody>
                </Card>
            ))}

            <Button
                variant="link"
                onClick={addItem}
                isDisabled={!canAdd || isDisabled}
                data-testid={`${name}-add`}
            >
                <PlusCircleIcon style={{ marginRight: 4 }} />
                {t("add")}
            </Button>
        </div>
    );
};

// 🔁 Parse JSON string into card array
function stringToCardArray(
    value: string,
    fieldKeys: FieldKey[]
): Record<string, string>[] {
    try {
        const parsed = JSON.parse(value);
        if (Array.isArray(parsed)) {
            return parsed.map((card) =>
                Object.fromEntries(
                    fieldKeys.map(({ name }) => [name, card[name] || ""])
                )
            );
        }
        return [];
    } catch (e) {
        console.warn("[MultiCardInput] Failed to parse JSON:", e);
        return [];
    }
}

// 🔁 Convert array of card objects to JSON string
function toStringValue(cards: Record<string, string>[]): string {
    return JSON.stringify(cards);
}
