package com.filemanager.resource;

import com.filemanager.entity.User;
import com.filemanager.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    private UserRepository userRepository;

    @GET
    @Path("/quota")
    public Response getMyQuota(@Context ContainerRequestContext requestContext) {
        Long userId = ((Number) requestContext.getProperty("userId")).longValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebApplicationException("Không tìm thấy user", 404));

        double percentage = (double) user.getUsedQuota() / user.getMaxQuota() * 100;

        return Response.ok(Map.of(
                "usedQuota", user.getUsedQuota(),
                "maxQuota", user.getMaxQuota(),
                "percentage", Math.round(percentage * 100.0) / 100.0 // Làm tròn 2 chữ số thập phân
        )).build();
    }

    // API dành cho Admin nâng cấp dung lượng (Ví dụ: truyền body {"newQuota": 5368709120} cho 5GB)
    @PUT
    @Path("/{id}/quota")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upgradeUserQuota(@PathParam("id") Long id, Map<String, Long> body) {
        // Trong thực tế, bạn sẽ cần thêm logic check Role Admin ở đây
        User user = userRepository.findById(id)
                .orElseThrow(() -> new WebApplicationException("Không tìm thấy user", 404));
        
        if(body.containsKey("newQuota")) {
            user.setMaxQuota(body.get("newQuota"));
            userRepository.save(user);
        }
        return Response.ok(Map.of("message", "Nâng cấp dung lượng thành công")).build();
    }
}