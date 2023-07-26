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

ENV DB_URL=jdbc:postgresql://mypostgres:5432/ncti
ENV DB_USER=postgres
ENV DB_PASSWORD=root
ENV SECRET=dSgVkYp3s6v9y$B&
ENV RABBITMQ_HOST=172.18.0.2
ENV RABBITMQ_USER=userok
ENV RABBITMQ_PASSWORD=p@ssw0rd
ENV MINIO_URL=minio1
ENV MINIO_ACCESS=ROOTUSER
ENV MINIO_SECRET=CHANGEME123
ENV MINIO_BUCKET=ncti

EXPOSE 8080

# Запуск приложения
CMD ["java", "-jar", "app.jar"]
