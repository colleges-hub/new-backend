package ru.ncti.backend.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Entity
@Table(name = "speciality")
public class Speciality {
    @Id
    @Column(name = "id_spec")
    private String id;

    @Column(name = "name_spec", nullable = false)
    private String name;
}
