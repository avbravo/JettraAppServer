package com.jettra.server.autentification.controller;

import com.jettra.core.inject.annotation.Inject;
import com.jettra.rest.annotations.accreditation.DeclareRoles;
import com.jettra.rest.annotations.accreditation.RolesAllowed;
import com.jettra.rest.annotations.*;
import com.jettra.rest.core.Response;
import com.jettra.server.autentification.entity.JRole;
import com.jettra.server.autentification.repository.JRoleRepository;
import java.util.List;
import java.util.UUID;

@Secured
@Path("/autentification/jroles")
@DeclareRoles({"ADMIN", "MANAGER"})
@RolesAllowed({"ADMIN"})
public class JRoleController {
    @Inject
    JRoleRepository jRoleRepository = new com.jettra.server.autentification.repository.JRoleRepositoryImpl();
    @GET
    @Produces("application/json")
    public List<JRole> findAll() {
        return jRoleRepository.findAll();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response save(JRole jRole) {
        jRoleRepository.save(jRole);
        return Response.ok("Saved successfully").build();
    }

    @DELETE
    @Path("/{id}")
    @Produces("application/json")
    public Response delete(@PathParam("id") String id) {
        UUID uuid = UUID.fromString(id);
        jRoleRepository.delete(uuid);
        return Response.ok("Deleted successfully").build();
    }
}
