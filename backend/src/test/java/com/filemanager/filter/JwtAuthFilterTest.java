package com.filemanager.filter;

import com.filemanager.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

class JwtAuthFilterTest {

    private JwtAuthFilter filter;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "9a4f2c8d3b7a1e6f45c8a0b3f267d8b1d4e6f3c8a9d2b5f8e3a9c8b5f6v8a3d9");
        ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 604800000L);
        jwtUtil.init();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
    }

    @Test
    void shouldRejectProtectedRequestWhenBearerTokenTypeIsRefresh() {
        String refreshToken = jwtUtil.generateRefreshToken("alice", 1L);

        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> queryParams = mock(MultivaluedMap.class);

        when(ctx.getMethod()).thenReturn("GET");
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("files/list");
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer " + refreshToken);
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);
        when(queryParams.getFirst("token")).thenReturn(null);

        filter.filter(ctx);

        verify(ctx).abortWith(any(Response.class));
        verify(ctx, never()).setProperty(eq("userId"), any());
        verify(ctx, never()).setProperty(eq("username"), any());
    }
}
