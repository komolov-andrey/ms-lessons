package com.example.order.async.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "async_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AsyncMessage extends PersistableEntity<AsyncMessageId> {

    @EmbeddedId
    private AsyncMessageId id;

    @Column(nullable = false)
    private String val;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        CREATED, SENT
    }

}
