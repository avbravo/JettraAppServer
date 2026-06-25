package com.jettra.server.autentification.controller;

import com.jettra.core.inject.annotation.Inject;
import com.jettra.rest.annotations.accreditation.DeclareRoles;
import com.jettra.rest.annotations.accreditation.RolesAllowed;
import com.jettra.server.autentification.entity.JCredential;
import com.jettra.server.autentification.repository.JCredentialRepository;
import com.jettra.rest.annotations.*;
import com.jettra.rest.core.Response;
import java.util.List;
import java.util.UUID;

@Secured
@Path("/autentification/jcredentials")
@DeclareRoles({"ADMIN", "MANAGER"})
@RolesAllowed({"ADMIN"})
public class JCredentialController {
    @Inject
    JCredentialRepository jCredentialRepository = new com.jettra.server.autentification.repository.JCredentialRepositoryImpl();

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
