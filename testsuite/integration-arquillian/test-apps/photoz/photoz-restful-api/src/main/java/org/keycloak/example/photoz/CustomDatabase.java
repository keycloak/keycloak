package org.keycloak.example.photoz;

import org.keycloak.example.photoz.entity.Album;
import org.keycloak.example.photoz.entity.Photo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author mhajas
 */
public class CustomDatabase {

    private static final CustomDatabase INSTANCE = new CustomDatabase();
    private List<Album> albums;
    private List<Photo> photos;
    private Long lastIndex = 0L;


    public static final CustomDatabase create() {
        return INSTANCE;
    }

    private CustomDatabase() {
        albums = new ArrayList<>();
    }

    public List<Album> getAll() {
        return albums;
    }

    public void addAlbum(Album a) {
        a.setId(lastIndex++);
        albums.add(a);
    }

    public void remove(Album albumToRemove) {
        Iterator<Album> iter = albums.iterator();

        while (iter.hasNext()) {
            Album a = iter.next();
            if (a.getId().equals(albumToRemove.getId())) {
                iter.remove();
            }
        }
    }

    public Album findById(Long id) {
        for (Album a : albums) {
            if(a.getId().equals(id)) {
                return a;
            }
        }

        return null;
    }

    public Album findByName(String name) {
        for (Album a : albums) {
            if(a.getName().equals(name)) {
                return a;
            }
        }

        return null;
    }

    public List<Album> findByUserId(String userId) {
        List<Album> result = new ArrayList<>();

        for (Album a : albums) {
            if (a.getUserId().equals(userId)) {
                result.add(a);
            }
        }

        return result;
    }

    public int cleanAll() {
        int result = albums.size() + photos.size();
        albums.clear();
        photos.clear();

        return result;
    }
}
