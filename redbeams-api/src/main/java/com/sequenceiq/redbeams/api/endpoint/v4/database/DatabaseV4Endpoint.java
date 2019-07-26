package com.sequenceiq.redbeams.api.endpoint.v4.database;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.redbeams.api.RedbeamsApi;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.doc.Notes.DatabaseNotes;
import com.sequenceiq.redbeams.doc.OperationDescriptions.DatabaseOpDescription;
import com.sequenceiq.redbeams.doc.ParamDescriptions.DatabaseParamDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

@Path("/v4/databases")
@Consumes(MediaType.APPLICATION_JSON)
@Api(tags = { "databases" },
    protocols = "http,https",
    produces = MediaType.APPLICATION_JSON,
    authorizations = { @Authorization(value = RedbeamsApi.CRN_HEADER_API_KEY) })
public interface DatabaseV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.LIST, notes = DatabaseNotes.LIST,
            nickname = "listDatabases")
    DatabaseV4Responses list(
        @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true) @QueryParam("environmentId") String environmentId
    );

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.REGISTER, notes = DatabaseNotes.REGISTER,
            nickname = "registerDatabase")
    DatabaseV4Response register(
        @Valid @ApiParam(DatabaseParamDescriptions.DATABASE_REQUEST) DatabaseV4Request request
    );

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.GET_BY_NAME, notes = DatabaseNotes.GET_BY_NAME,
            nickname = "getDatabase")
    DatabaseV4Response get(
        @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true) @QueryParam("environmentId") String environmentId,
        @ApiParam(DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.DELETE_BY_NAME, notes = DatabaseNotes.DELETE_BY_NAME,
            nickname = "deleteDatabase")
    DatabaseV4Response delete(
        @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true) @QueryParam("environmentId") String environmentId,
        @ApiParam(DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.DELETE_MULTIPLE_BY_NAME, notes = DatabaseNotes.DELETE_MULTIPLE_BY_NAME,
            nickname = "deleteMultipleDatabases")
    DatabaseV4Responses deleteMultiple(
        @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true) @QueryParam("environmentId") String environmentId,
        @ApiParam(DatabaseParamDescriptions.NAMES) Set<String> names
    );

    @POST
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.TEST_CONNECTION, notes = DatabaseNotes.TEST_CONNECTION,
            nickname = "testDatabaseConnection")
    DatabaseTestV4Response test(
        @Valid @ApiParam(DatabaseParamDescriptions.DATABASE_TEST_REQUEST) DatabaseTestV4Request databaseTestV4Request
    );
}