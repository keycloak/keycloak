import React from 'react';
import { Table, Thead, Tbody, Tr, Th, Td, TableText } from '@patternfly/react-table';
import { ClipboardCopy, ClipboardCopyVariant, Tooltip } from '@patternfly/react-core';

// TIDECLOAK IMPLEMENTATION
export interface License {
    licenseData: string;
    status: string;
    date: string;
}

type TideLicenseHistoryProps = {
    licenseList: License[];
};



export const TideLicenseHistory: React.FC<TideLicenseHistoryProps> = ({ licenseList }) => {
    const formatDates = (expiryDate: number) => {
        const date = new Date(expiryDate * 1000);
        const localTime = date.toLocaleString();
        const utcTime = date.toUTCString();
        const estTime = date.toLocaleString("en-US", { timeZone: "America/New_York" });

        return { localTime, utcTime, estTime };
    };

    return (
        <div style={{ maxHeight: '400px', overflowY: 'auto', border: '1px solid #ccc', borderRadius: '4px' }}>
            <Table variant="compact" borders={true}>
                <Thead>
                    <Tr>
                        <Th>License</Th>
                        <Th>Status</Th>
                        <Th>Date</Th>
                    </Tr>
                </Thead>
                <Tbody>
                    {licenseList.length === 0 ? (
                        <Tr>
                            <Td colSpan={3} style={{ textAlign: 'center' }}>
                                No license history available.
                            </Td>
                        </Tr>
                    ) : (
                        licenseList.map((license, index) => {
                            const { localTime, utcTime, estTime } = formatDates(
                                parseInt(license.date),
                            );
                            return (
                            <Tr key={index}>
                                <Td width={50}>
                                    <TableText wrapModifier="truncate">
                                        <ClipboardCopy
                                            isCode
                                            isReadOnly
                                            hoverTip="Copy to clipboard"
                                            clickTip="Copied!"
                                            variant={ClipboardCopyVariant.inline} // Keeps it inline and compact
                                        >
                                            {JSON.stringify(license.licenseData, null, 2)}
                                        </ClipboardCopy>
                                    </TableText>
                                </Td>
                                <Td>{license.status}</Td>
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
                            </Tr>
                        )
                    })
                    )}
                </Tbody>
            </Table>
        </div>
    );
};
