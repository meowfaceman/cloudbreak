package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/recipe", description = ControllerDescription.RECIPE_DESCRIPTION, position = 5)
public interface RecipeEndpoint {

    @POST
    @Path("user/recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    IdJson postPrivate(RecipeRequest recipeRequest);

    @POST
    @Path("account/recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    IdJson postPublic(RecipeRequest recipeRequest);

    @GET
    @Path("user/recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    Set<RecipeResponse> getPrivates();

    @GET
    @Path("account/recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    Set<RecipeResponse> getPublics();

    @GET
    @Path("user/recipes/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    RecipeResponse getPrivate(@PathParam(value = "name") String name);

    @GET
    @Path("account/recipes/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    RecipeResponse getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("recipes/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    RecipeResponse get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("recipes/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @DELETE
    @Path("account/recipes/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/recipes/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);
}
