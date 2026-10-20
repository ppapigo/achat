package com.achat.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;

@Component
public class SecurityOrigins {
    private final List<String> origins;
    public SecurityOrigins(@Value("${app.security.allowed-origins:https://achat.grooverapp.com}") String value,
                           Environment environment) {
        origins = Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (origins.isEmpty()) throw new IllegalStateException("Allowed origins are required");
        boolean dev = environment.acceptsProfiles(Profiles.of("dev", "test"))
                && !environment.acceptsProfiles(Profiles.of("prod"));
        for (String origin : origins) {
            URI uri = URI.create(origin);
            if (origin.contains("*") || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty())
                    || (!dev && (!"https".equals(uri.getScheme()) || !origin.equals("https://achat.grooverapp.com")))) {
                throw new IllegalStateException("Invalid allowed origin configuration");
            }
        }
    }
    public List<String> values() { return origins; }
}
