package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
public final class Url extends Model {
    @Id
    private int id;
    @Column(unique = true)
    private String name;
    @WhenCreated
    private Instant createdAt;
    @OneToMany
    private List<UrlCheck> urlChecks;
    private Integer lastStatusCode = 1;
    private Instant lastCheckDate;


    public Url(String name) {
        this.name = name;
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    public Instant getLastCheckDate() {
        return lastCheckDate;
    }

    public void setLastCheckDate(Instant lastCheckDate) {
        this.lastCheckDate = lastCheckDate;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<UrlCheck> getUrlChecks() {
        return urlChecks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
