/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.workflow.admin.resource;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import org.keycloak.models.ModelException;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.services.ErrorResponse;

/**
 * Resource for managing steps within a workflow.
 *
 */
@Tag(name = "Workflow Steps", description = "Manage steps within workflows")
public class WorkflowStepsResource {

    private final WorkflowsManager workflowsManager;
    private final Workflow workflow;

    public WorkflowStepsResource(WorkflowsManager workflowsManager, Workflow workflow) {
        this.workflowsManager = workflowsManager;
        this.workflow = workflow;
    }

    /**
     * Get all steps for this workflow.
     *
     * @return list of steps
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all steps for this workflow")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Success", 
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, 
                                     schema = @Schema(type = SchemaType.ARRAY, 
                                                    implementation = WorkflowStepRepresentation.class)))
    })
    public List<WorkflowStepRepresentation> getSteps() {
        return workflowsManager.getSteps(workflow.getId()).stream()
                .map(workflowsManager::toRepresentation)
                .toList();
    }

    /**
     * Add a new step to this workflow.
     *
     * @param stepRep step representation
     * @param position optional position to insert the step at (0-based index)
     * @return the created step
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add a new step to this workflow")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Step created successfully", 
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, 
                                     schema = @Schema(implementation = WorkflowStepRepresentation.class))),
        @APIResponse(responseCode = "400", description = "Invalid step representation or position")
    })
    public Response addStep(
            @RequestBody(description = "Step to add", required = true,
                        content = @Content(schema = @Schema(implementation = WorkflowStepRepresentation.class)))
            WorkflowStepRepresentation stepRep, 
            @Parameter(description = "Position to insert the step at (0-based index). If not specified, step is added at the end.")
            @QueryParam("position") Integer position) {
        if (stepRep == null) {
            throw ErrorResponse.error("Step representation cannot be null", Response.Status.BAD_REQUEST);
        }
        try {
            WorkflowStep step = workflowsManager.toModel(stepRep);
            WorkflowStep addedStep = workflowsManager.addStepToWorkflow(workflow, step, position);

            return Response.ok(workflowsManager.toRepresentation(addedStep)).build();
        } catch (ModelException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Remove a step from this workflow.
     *
     * @param stepId ID of the step to remove
     * @return no content response on success
     */
    @Path("{stepId}")
    @DELETE
    @Operation(summary = "Remove a step from this workflow")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Step removed successfully"),
        @APIResponse(responseCode = "400", description = "Invalid step ID"),
        @APIResponse(responseCode = "404", description = "Step not found")
    })
    public Response removeStep(
            @Parameter(description = "ID of the step to remove", required = true)
            @PathParam("stepId") String stepId) {
        if (stepId == null || stepId.trim().isEmpty()) {
            throw new BadRequestException("Step ID cannot be null or empty");
        }

        workflowsManager.removeStepFromWorkflow(workflow, stepId);
        return Response.noContent().build();
    }

    /**
     * Get a specific step by its ID.
     *
     * @param stepId ID of the step to retrieve
     * @return the step representation
     */
    @Path("{stepId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a specific step by its ID")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Step found", 
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, 
                                     schema = @Schema(implementation = WorkflowStepRepresentation.class))),
        @APIResponse(responseCode = "400", description = "Invalid step ID"),
        @APIResponse(responseCode = "404", description = "Step not found")
    })
    public WorkflowStepRepresentation getStep(
            @Parameter(description = "ID of the step to retrieve", required = true)
            @PathParam("stepId") String stepId) {
        if (stepId == null || stepId.trim().isEmpty()) {
            throw new BadRequestException("Step ID cannot be null or empty");
        }

        WorkflowStep step = workflowsManager.getStepById(stepId);

        if (step == null) {
            throw new BadRequestException("Step not found: " + stepId);
        }

        return workflowsManager.toRepresentation(step);
    }
}
