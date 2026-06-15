package com.example.order.async.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@MappedSuperclass
public abstract class PersistableEntity<ID> implements Persistable<ID> {

    @Column(nullable = false, insertable = false, updatable = false)
    @ColumnDefault("now()")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
