package org.kalipo.domain;

import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * A Notification.
 */
@Document(collection = "T_NOTIFICATION")
public class Notification implements Serializable {

    @Id
    private String id;

    @CreatedDate
    private DateTime createdDate;

    private boolean seen;

    @NotNull(message = "{constraint.notnull.noticeType}")
    private Type type;

    @NotNull(message = "{constraint.notnull.resourceId}")
    private String resourceId;

    @NotNull(message = "{constraint.notnull.recipientId}")
    private String recipientId;

    @NotNull(message = "{constraint.notnull.initiatorId}")
    private String initiatorId;

    private String message;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum Type {
        LIKE, MENTION, REPLY, DELETION, REJECTED, APPROVAL
    }
}
