/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { NgModule }             from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { MyResourcesPageComponent } from './my-resources-page.component';
import { MyResourcesDetailPageComponent } from './my-resources-detail-page.component';
import { SharedWithMePageComponent } from './shared-with-me-page.component';
import { SharedWithMeDetailPageComponent } from './shared-with-me-detail-page.component';

const routes: Routes = [
    { path: 'my-resources', component: MyResourcesPageComponent },
    { path: 'my-resources-detail', component: MyResourcesDetailPageComponent },
    { path: 'shared-with-me', component: SharedWithMePageComponent },
    { path: 'shared-with-me-detail', component: SharedWithMeDetailPageComponent },
    { path: '**', component: MyResourcesPageComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MyResourcesRoutingModule {}

