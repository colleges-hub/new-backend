package ru.ncti.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Entity
@Builder
@Table(name = "messages")
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @ManyToOne(targetEntity = User.class, cascade = {CascadeType.ALL})
    private User sender;

    @Column(name = "type")
    private String type;

    @ManyToOne(targetEntity = PublicChat.class, cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    private PublicChat publicChat;

    @ManyToOne(targetEntity = PrivateChat.class, cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    private PrivateChat privateChat;

    @Column(name = "createdAt")
    private Instant createdAt;
}
