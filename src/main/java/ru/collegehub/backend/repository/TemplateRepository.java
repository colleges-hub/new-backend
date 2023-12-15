package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Group;
import ru.collegehub.backend.model.Template;
import ru.collegehub.backend.model.User;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findAllByTeacher(User user);

    List<Template> findAllByGroup(Group group);
}
