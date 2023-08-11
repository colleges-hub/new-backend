package ru.ncti.backend.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Entity
@Table(name = "fcm_tokents")
public class FCM {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token")
    private String token;

    @Column(name = "device")
    private String device;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
