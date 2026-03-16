import React from 'react';
import { Table, Thead, Tbody, Tr, Th, Td } from '@patternfly/react-table';
import { Tooltip, Button, AlertVariant } from '@patternfly/react-core';
import { useAdminClient } from '../../admin-client';
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";



// TIDECLOAK IMPLEMENTATION
export interface ScheduledTaskInfo {
    taskName: string;
    startDateMillis: number;
    delayMillis: number;
}

enum TideTasks {
    genVRK = "Scheduled task to generate a new VRK before the current license expiration.",
    switchVRK = "Scheduled task to switch the signed pending VRK to active upon current license expiration.",
    rotateVRK = "Scheduled task to sign a new VRK.",
}

type TideScheduledTasksProps = {
    scheduledTasks: ScheduledTaskInfo[];
    refresh: () => void;
};

export const TideScheduledTasks: React.FC<TideScheduledTasksProps> = ({ scheduledTasks, refresh }) => {
    const { adminClient } = useAdminClient();
    const { addAlert } = useAlerts();
    const { t } = useTranslation();



    const formatDates = (startDateMillis: number, delayMillis: number) => {
        const date = new Date(startDateMillis + delayMillis);
        const localTime = date.toLocaleString();
        const utcTime = date.toUTCString();
        const estTime = date.toLocaleString("en-US", { timeZone: "America/New_York" });

        return { localTime, utcTime, estTime };
    };

    const extractTaskDisplayName = (taskName: string): string => {
        const parts = taskName.split(":");
        return parts.length > 1 ? parts[1] : taskName;
    };

    const genVRKTask = scheduledTasks.find(task =>
        extractTaskDisplayName(task.taskName) === "genVRK"
    );

    const handleRunNow = async (taskName: string) => {
        try {
            await adminClient.tideAdmin.triggerScheduledTask({taskName});
            refresh();
        } catch(e){
            if(e instanceof Error){
                const responseData = (e as any)?.responseData;

                addAlert(responseData, AlertVariant.danger);

            } else{
                addAlert(`Error running ${taskName} task.`, AlertVariant.danger);
            }
        }
    };

    const handleScheduleGenVRK = async () => {
        try {
            await adminClient.tideAdmin.scheduleGenVRKTask();
            refresh();

        } catch (e){
            if(e instanceof Error){
                const responseData = (e as any)?.responseData;

                addAlert(responseData, AlertVariant.danger);

            } else{
                addAlert(`Error scheduling genVRK task. See logs.`, AlertVariant.danger);

            }
        }
    }

    return (
        <div style={{ maxHeight: '400px', overflowY: 'auto', border: '1px solid #ccc', borderRadius: '4px' }}>
            <Table variant="compact" borders={true}>
                <Thead>
                    <Tr>
                        <Th>Task Name</Th>
                        <Th>Description</Th>
                        <Th>Scheduled Date</Th>
                        <Th>Action</Th>
                    </Tr>
                </Thead>
                <Tbody>
                    {scheduledTasks.map((scheduledTask, index) => {
                        const { localTime, utcTime, estTime } = formatDates(
                            scheduledTask.startDateMillis,
                            scheduledTask.delayMillis
                        );

                        return (
                            <Tr key={index}>
                                <Td>{scheduledTask.taskName}</Td>
                                <Td>{TideTasks[extractTaskDisplayName(scheduledTask.taskName) as keyof typeof TideTasks]}</Td>
                                <Td>
                                    <Tooltip
                                        content={
                                            <>
                                                <div><strong>UTC:</strong> {utcTime}</div>
                                                <div><strong>EST:</strong> {estTime}</div>
                                            </>
                                        }
                                    >
                                        <span>{localTime}</span>
                                    </Tooltip>
                                </Td>
                                <Td>
                                    {extractTaskDisplayName(scheduledTask.taskName) === "genVRK" && (
                                        <Button onClick={async () => await handleRunNow(scheduledTask.taskName)} variant="primary">
                                            Run Now
                                        </Button>
                                    )}
                                    {extractTaskDisplayName(scheduledTask.taskName) === "rotateVRK" && (
                                        <Button onClick={async () => await handleRunNow(scheduledTask.taskName)} variant="primary">
                                            Run Now
                                        </Button>
                                    )}
                                    {extractTaskDisplayName(scheduledTask.taskName) === "switchVRK" && (
                                        <Button onClick={async () => await handleRunNow(scheduledTask.taskName)} variant="primary">
                                            Run Now
                                        </Button>
                                    )}
                                </Td>
                            </Tr>
                        );
                    })}
                    {!genVRKTask && (
                        <Tr>
                            <Td>genVRK</Td>
                            <Td>{TideTasks.genVRK}</Td>
                            <Td>
                                <Tooltip
                                    content={
                                        <>
                                            <div><strong>UTC:</strong> N/A</div>
                                            <div><strong>EST:</strong> N/A</div>
                                        </>
                                    }
                                >
                                    <span>No task scheduled</span>
                                </Tooltip>
                            </Td>
                            <Td>
                                <Button onClick={async () => await handleScheduleGenVRK()} variant="primary">
                                    Schedule
                                </Button>
                            </Td>
                        </Tr>
                    )}
                </Tbody>
            </Table>
        </div>
    );
};
