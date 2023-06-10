package ru.ncti.backend.service;

import java.util.Map;
import java.util.Set;

public interface UserInterface<T> {

    T getProfile();

    Map<String, Set<T>> schedule();
}