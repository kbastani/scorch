package demo.scorch.audit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * An entity base class that is used for auditing purposes.
 *
 * @author Kenny Bastani
 */
@JsonAutoDetect
public class AbstractEntity implements Serializable {

    private String id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date lastModified;

    public AbstractEntity() {
        this.createdAt = new Date();
        this.lastModified = new Date(createdAt.getTime());
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "AbstractEntity{" +
                "id='" + id + '\'' +
                ", createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }
}

