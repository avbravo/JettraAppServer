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
import io.jettra.server.autentification.entity.JAccreditation;
import io.jettra.server.autentification.repository.JAccreditationRepository;
import io.jettra.server.discoverer.Discovered;
import java.util.List;
import java.util.UUID;

@Secured
@Path("/autentification/jaccreditations")
@DeclareRoles({"ADMIN", "MANAGER"})
@RolesAllowed({"ADMIN"})
@Discovered
public class JAccreditationController {

    @Inject
    JAccreditationRepository jAccreditationRepository = new io.jettra.server.autentification.repository.JAccreditationRepositoryImpl();

    @GET
    @Produces("application/json")
    public List<JAccreditation> findAll() {
        return jAccreditationRepository.findAll();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response save(JAccreditation jAccreditation) {
        jAccreditationRepository.save(jAccreditation);
        return Response.ok("Saved successfully").build();
    }

    @DELETE
    @Path("/{id}")
    @Produces("application/json")
    public Response delete(@PathParam("id") String id) {
        UUID uuid = UUID.fromString(id);
        jAccreditationRepository.delete(uuid);
        return Response.ok("Deleted successfully").build();
    }
}
