package ru.collegehub.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"template", "schedules"})
@EqualsAndHashCode(exclude = {"template", "schedules"})
@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "speciality")
    private Speciality speciality;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "course")
    private Integer course;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private Set<Template> template;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private Set<Schedule> schedules;
}
