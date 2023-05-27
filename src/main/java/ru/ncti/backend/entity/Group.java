package ru.ncti.backend.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
 */
@Getter
@Setter
@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gr")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_spec")
    private Speciality speciality;

    @Column(name = "name_gr", nullable = false)
    private String name;

    @Column(name = "course")
    private Integer course;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private Set<Sample> sample;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private Set<Schedule> schedules;
}
