/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import {ShareResourceModal} from "./ShareResourceModal";

export interface DataTableState {
    toggleActions: boolean
}

export interface DataTableProps {
    data: any[]
    headers: any[]
}

export class DataTable extends React.Component<DataTableProps, DataTableState> {

    constructor(props: DataTableProps, shareModal: ShareResourceModal) {
        super(props);
        this.state = {toggleActions: false}
    }

    render(): React.ReactNode {
        return (
            <table className="pf-c-table pf-m-grid-xl" role="grid" id="simple-table-demo">
                <thead>
                {this.getHeaders()}
                </thead>
                <tbody>
                {this.getRows()}
                </tbody>
            </table>
        );
    }

    Actions = () => (
        <div className="pf-c-dropdown">
            <button className="pf-c-dropdown__toggle pf-m-plain"
                    id="simple-table-demo-dropdown-kebab-right-aligned-1-button"
                    aria-expanded="false" aria-label="Actions" onClick={this.toggleResourceActions}>
                <i className="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            {this.state.toggleActions &&
            <ul className="pf-c-dropdown__menu pf-m-align-right"
                aria-labelledby="simple-table-demo-dropdown-kebab-right-aligned-1-button">
                <li><a className="pf-c-dropdown__menu-item" href="#">Link</a></li>
                <li>
                    <button className="pf-c-dropdown__menu-item">Action</button>
                </li>
                <li><a className="pf-c-dropdown__menu-item pf-m-disabled" aria-disabled="true"
                       href="#">Disabled Link</a></li>
                <li>
                    <button className="pf-c-dropdown__menu-item" disabled>Disabled Action
                    </button>
                </li>
                <li className="pf-c-dropdown__separator" role="separator"></li>
                <li><a className="pf-c-dropdown__menu-item" href="#">Separated Link</a></li>
            </ul>
            }
        </div>
    )

    Share = (resource: any) => (
        <ShareResourceModal resource={resource}/>
    )

    private toggleResourceActions = (): void => {
        this.setState(({
            toggleActions: !this.state.toggleActions
        }));
    }

    private getHeaders() {
        var headers = [];
        let columns = [];

        for (let i = 0; i < this.props.headers.length; i++) {
            columns.push(<th scope="col">{this.props.headers[i]}</th>)
        }

        columns.push(<th scope="col"></th>)
        headers.push(<tr key={0}>{columns}</tr>)

        return headers;
    }

    private getRows() {
        var rows = [];

        for (let i = 0; i < this.props.data.length; i++) {
            let columns = [];

            for (let j = 0; j < this.props.data[i].length; j++) {
                columns.push(<td style={{fontSize: 14}}>{this.props.data[i][j]}</td>)
            }

            columns.push(<td className="pf-c-table__action">
                <div className="pf-c-toolbar__action-group">
                    {this.Share(this.props.data[i])}
                    <this.Actions/>
                </div>
            </td>)

            rows.push(<tr key={i}>{columns}</tr>)
        }

        return rows;
    }
}

const styles = {
    rowFontSize: 14
}
