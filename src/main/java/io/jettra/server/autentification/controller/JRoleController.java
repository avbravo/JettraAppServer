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
import io.jettra.rest.core.Response;
import io.jettra.server.autentification.entity.JRole;
import io.jettra.server.autentification.repository.JRoleRepository;
import java.util.List;
import java.util.UUID;

@Secured
@Path("/autentification/jroles")
@DeclareRoles({"ADMIN", "MANAGER"})
@RolesAllowed({"ADMIN"})
public class JRoleController {
    @Inject
    JRoleRepository jRoleRepository = new io.jettra.server.autentification.repository.JRoleRepositoryImpl();
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
