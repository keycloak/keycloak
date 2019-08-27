/* 
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
import { 
  Modal, 
  Button, 
  ButtonProps, 
  Dropdown,
  DropdownPosition,
  DropdownToggle,
  DropdownItem
} from '@patternfly/react-core';
import {Msg} from './Msg';

interface DropdownWrapperProps {
    dropdownWrapperItems: any[];
    dropdownWrapperTitle?: string;
    onDropdownSelect: () => void;
}

interface DropdownWrapperState {
    isDropdownOpen: boolean;
}

/**
 * @author Bruno Oliveira bruno@abstractj.org (C) 2019 Red Hat Inc.
 */
export class DropdownWrapper extends React.Component<DropdownWrapperProps, DropdownWrapperState> {
    protected static defaultProps = {
        dropdownWrapperTitle: 'Name'
    };
    
    public constructor(props: DropdownWrapperProps) {
        super(props);
        this.state = {
            isDropdownOpen: false
        };
    }

    private handleDropdownToggle = () => {
        this.setState(({ isDropdownOpen }) => ({
            isDropdownOpen: !isDropdownOpen
        }));
    };

    private handleSelect = () => {
        this.handleDropdownToggle();
        this.props.onDropdownSelect();
    }

    public render(): React.ReactNode {
      const { isDropdownOpen } = this.state;
        return (
            <React.Fragment>
              <Dropdown
                onSelect={this.handleSelect}
                position={DropdownPosition.right}
                toggle={<DropdownToggle onToggle={this.handleDropdownToggle}>{this.props.dropdownWrapperTitle}</DropdownToggle>}
                isOpen={isDropdownOpen}
                dropdownItems={this.props.dropdownWrapperItems}
              />
            </React.Fragment>
        );
    }
};
