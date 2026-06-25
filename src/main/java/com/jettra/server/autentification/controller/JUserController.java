package com.jettra.server.autentification.controller;

import com.jettra.core.inject.annotation.Inject;
import com.jettra.rest.annotations.accreditation.DeclareRoles;
import com.jettra.rest.annotations.accreditation.RolesAllowed;
import com.jettra.rest.annotations.*;
import com.jettra.rest.core.Response;
import com.jettra.server.autentification.entity.JUser;
import com.jettra.server.autentification.repository.JUserRepository;
import java.util.List;
import java.util.UUID;

@Secured
@Path("/autentification/jusers")
@DeclareRoles({"ADMIN", "MANAGER"})
@RolesAllowed({"ADMIN"})
public class JUserController {

    @Inject
    JUserRepository jUserRepository = new com.jettra.server.autentification.repository.JUserRepositoryImpl();
    @GET
    @Produces("application/json")
    public List<JUser> findAll() {
        System.out.println("\npaso 1");
        return jUserRepository.findAll();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response save(JUser jUser) {
        jUserRepository.save(jUser);
        return Response.ok("Saved successfully").build();
    }

    @DELETE
    @Path("/{id}")
    @Produces("application/json")
    public Response delete(@PathParam("id") String id) {
          UUID uuid = UUID.fromString(id);
        jUserRepository.delete(uuid);
        return Response.ok("Deleted successfully").build();
    }
}
