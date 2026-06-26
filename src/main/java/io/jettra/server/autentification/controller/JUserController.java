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
import io.jettra.server.autentification.entity.JUser;
import io.jettra.server.autentification.repository.JUserRepository;
import java.util.List;
import java.util.UUID;

@Secured
@Path("/autentification/jusers")
@DeclareRoles({"ADMIN", "MANAGER"})
@RolesAllowed({"ADMIN"})
public class JUserController {

    @Inject
    JUserRepository jUserRepository = new io.jettra.server.autentification.repository.JUserRepositoryImpl();
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
