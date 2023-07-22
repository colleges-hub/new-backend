package ru.ncti.backend.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * user: ichuvilin
 */
@Setter
@Getter
@Entity
@Table(name = "public_chats")
public class PublicChat {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "publicChat", cascade = {CascadeType.ALL})
    List<Message> messages;

    @ManyToMany
    Set<User> users = new HashSet<>();

    @Column(name = "photo")
    private String photo;
}
