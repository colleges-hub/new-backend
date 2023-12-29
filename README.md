# Backend

## Startup docker container:

- go to directory kafka
- start docker-compose

```bash
docker-compose up -d
```

- start docker build

```bash
docker build .
```

- run docker images. Use next env variable
    - DB_HOST* - db host
    - DB_PORT* - db port
    - DB_BASE* - db base
    - DB_USER* - user for connect to DB
    - DB_PASSWORD* - user password for connect to DB
    - SECRET* - secret key for jwt token
    - KAFKA_BOOTSTRAP* - url for connect to kafka

P.S: * - required variable

```bash
docker run --name backend -p 8080:8080 -d backend
```