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

import {BackgroundImageSrc, BackgroundImage} from '@patternfly/react-core';
 
declare const resourceUrl: string;

const pFlyImages = resourceUrl + '/node_modules/@patternfly/patternfly/assets/images/';

const bgImages = {
    [BackgroundImageSrc.xs]: pFlyImages + 'pfbg_576.jpg',
    [BackgroundImageSrc.xs2x]: pFlyImages + 'pfbg_576@2x.jpg',
    [BackgroundImageSrc.sm]: pFlyImages + 'pfbg_768.jpg',
    [BackgroundImageSrc.sm2x]: pFlyImages + 'pfbg_768@2x.jpg',
    [BackgroundImageSrc.lg]: pFlyImages + 'pfbg_1200.jpg',
    [BackgroundImageSrc.filter]: pFlyImages + 'background-filter.svg#image_overlay'
};

interface BackgroundProps {}
export class Background extends React.Component<BackgroundProps> {
    
    public constructor(props: BackgroundProps) {
        super(props);
    }

    public render(): React.ReactNode {
        return (
            <BackgroundImage src={bgImages} />
        );
    }
};