# Загрузка образа для сборки приложения
FROM gradle:7.3.3-jdk17 AS build

# Копирование всех файлов из текущего каталога на хост-системе в Docker-образ
COPY . /home/app

# Установка рабочей директории
WORKDIR /home/app

# Запуск сборки приложения
RUN gradle clean build -x test --no-daemon

# Загрузка образа для запуска приложения
FROM openjdk:17-slim AS run

# Копирование собранного jar-файла из предыдущего образа в текущий
ARG JAR_FILE=/home/app/build/libs/*.jar
COPY --from=build ${JAR_FILE} /home/app/app.jar

# Установка рабочей директории
WORKDIR /home/app

ENV DB_URL=jdbc:postgresql://localhost:5432/db
ENV DB_USER=q
ENV DB_PASSWORD=q
ENV EMAIL_USER=mail
ENV EMAIL_PASSWORD=mail
ENV SECRET=dSgVkYp3s6v9y$B&
ENV RABBITMQ_HOST=localhost
ENV RABBITMQ_USER=u
ENV RABBITMQ_PASSWORD=p

EXPOSE 8080

# Запуск приложения
CMD ["java", "-jar", "app.jar"]
