package org.kalipo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Site representing an independent thread-space
 */

@Document(collection = "T_SITE")
public class Site implements Serializable {

    @Id
    private String id;

    /**
     *
     */
    @Size(min = 1, max = 50)
    private String name;

    /**
     * todo implement
     * Bans on IP range level
     * Similar to IRC Z-line see https://en.wikipedia.org/wiki/IRCd#Z-line
     */
//    private Set<String> ipBans = new HashSet<String>();

    private Set<String> moderatorIds = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getModeratorIds() {
        return moderatorIds;
    }

    public void setModeratorIds(Set<String> moderatorIds) {
        this.moderatorIds = moderatorIds;
    }

}
