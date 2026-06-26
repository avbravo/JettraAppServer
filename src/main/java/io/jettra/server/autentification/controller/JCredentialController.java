package io.jettra.server.autentification.controller;

import io.jettra.rest.annotations.POST;
import io.jettra.rest.annotations.DELETE;
import io.jettra.rest.annotations.Consumes;
import io.jettra.rest.annotations.GET;
import io.jettra.rest.annotations.Secured;
import io.jettra.rest.annotations.Produces;
import io.jettra.rest.annotations.PathParam;
import io.jettra.rest.annotations.Path;
import io.jettra.core.inject.annotation.Inject;
import io.jettra.rest.annotations.accreditation.DeclareRoles;
import io.jettra.rest.annotations.accreditation.RolesAllowed;
import io.jettra.server.autentification.entity.JCredential;
import io.jettra.server.autentification.repository.JCredentialRepository;
import io.jettra.rest.core.Response;
import java.util.List;
import java.util.UUID;

@Secured
@Path("/autentification/jcredentials")
@DeclareRoles({"ADMIN", "MANAGER"})
@RolesAllowed({"ADMIN"})
public class JCredentialController {
    @Inject
    JCredentialRepository jCredentialRepository = new io.jettra.server.autentification.repository.JCredentialRepositoryImpl();

    @GET
    @Produces("application/json")
    public List<JCredential> findAll() {
        return jCredentialRepository.findAll();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response save(JCredential credential) {
       jCredentialRepository.save(credential);
        return Response.ok("Saved successfully").build();
    }

    @DELETE
    @Path("/{id}")
    @Produces("application/json")
    public Response delete(@PathParam("id") String id) {
        UUID uuid = UUID.fromString(id);
        jCredentialRepository.delete(uuid);
        return Response.ok("Deleted successfully").build();
    }
}
