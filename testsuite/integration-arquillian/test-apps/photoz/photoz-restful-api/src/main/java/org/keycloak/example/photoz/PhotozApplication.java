package org.keycloak.example.photoz;

import org.keycloak.example.photoz.admin.AdminAlbumService;
import org.keycloak.example.photoz.album.AlbumService;
import org.keycloak.example.photoz.album.ProfileService;
import org.keycloak.example.photoz.unsecured.UnsecuredService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Basic auth app.
 */
@ApplicationPath("/")
public class PhotozApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new LinkedHashSet<Class<?>>();
        resources.add(AlbumService.class);
        resources.add(AdminAlbumService.class);
        resources.add(ProfileService.class);
        resources.add(UnsecuredService.class);
        return resources;
    }
}
