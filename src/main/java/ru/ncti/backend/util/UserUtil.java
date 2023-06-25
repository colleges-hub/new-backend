package ru.ncti.backend.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ncti.backend.api.response.ScheduleResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.Sample;
import ru.ncti.backend.repository.SampleRepository;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * user: ichuvilin
 */
@Component
@RequiredArgsConstructor
public class UserUtil {

    private final SampleRepository sampleRepository;

    public String getCurrentWeekType() {
        LocalDate currentDate = LocalDate.now();
        int currentWeekNumber = currentDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        return currentWeekNumber % 2 == 0 ? "2" : "1";
    }


    public Set<ScheduleResponse> getTypeSchedule(Group group) {
        List<Sample> sample = sampleRepository.findAllByGroup(group);
        String currentWeekType = getCurrentWeekType();
        Set<ScheduleResponse> set = new HashSet<>();

        Map<String, List<UserResponse>> mergedTeachersMap = new HashMap<>();

        sample.stream()
                .filter(s -> s.getParity().equals("0") || s.getParity().equals(currentWeekType))
                .forEach(s -> {
                    ScheduleResponse dto = convert(s);
                    String key = dto.getDay() + "-" + dto.getNumberPair() + "-" + dto.getClassroom() + "-" + dto.getSubject();
                    List<UserResponse> mergedTeachers = mergedTeachersMap.get(key);
                    if (mergedTeachers != null) {
                        mergedTeachers.addAll(dto.getTeachers());
                    } else {
                        mergedTeachers = new ArrayList<>(dto.getTeachers());
                        mergedTeachersMap.put(key, mergedTeachers);
                    }
                });

        mergedTeachersMap.forEach((key, mergedTeachers) -> {
            String[] parts = key.split("-");
            String day = parts[0];
            int numberPair = Integer.parseInt(parts[1]);
            String classroom = parts[2];
            String subject = parts[3];

            ScheduleResponse mergedDto = ScheduleResponse.builder()
                    .day(day)
                    .numberPair(numberPair)
                    .subject(subject)
                    .teachers(mergedTeachers)
                    .classroom(classroom)
                    .build();

            set.add(mergedDto);
        });

        return set;
    }

    private ScheduleResponse convert(Sample sample) {
        return ScheduleResponse.builder()
                .day(sample.getDay())
                .numberPair(sample.getNumberPair())
                .subject(sample.getSubject().getName())
                .teachers(List.of(UserResponse.builder()
                        .firstname(sample.getTeacher().getFirstname())
                        .lastname(sample.getTeacher().getLastname())
                        .surname(sample.getTeacher().getSurname())
                        .photo(sample.getTeacher().getPhoto())
                        .build()))
                .classroom(sample.getClassroom())
                .build();
    }
}
