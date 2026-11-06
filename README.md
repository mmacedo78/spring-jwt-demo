# Spring Boot + JWT + Swagger (H2 local / PostgreSQL em produção)

Requisitos: Java 25+ e Maven 4.1.2+ (ou use o Docker, abaixo)

## Perfis

| Perfil  | Banco             | Como ativar                                   |
|---------|-------------------|-----------------------------------------------|
| `local` | H2 em memória     | padrão (nenhuma configuração necessária)      |
| `prod`  | PostgreSQL        | `SPRING_PROFILES_ACTIVE=prod`                 |

Arquivos: `application.properties` (comum), `application-local.properties`, `application-prod.properties`.

### Local (H2)
    mvn spring-boot:run

- Swagger UI:  http://localhost:8080/swagger-ui.html
- H2 Console:  http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:demodb | user: sa | senha: vazia)
- Usuários de teste (criados só no perfil `local`):
  - admin / admin123  (ROLE_ADMIN – pode deletar produtos)
  - user  / user123   (ROLE_USER)

### Produção (PostgreSQL)
Variáveis de ambiente:

| Variável          | Obrigatória | Descrição                                                    |
|--------------------|-------------|--------------------------------------------------------------|
| `DB_URL`          | não         | padrão `jdbc:postgresql://localhost:5432/demodb`             |
| `DB_USERNAME`     | sim         | usuário do banco                                             |
| `DB_PASSWORD`     | sim         | senha do banco                                               |
| `JWT_SECRET`      | sim         | Base64, mín. 256 bits (`openssl rand -base64 48`)            |
| `JWT_EXPIRATION_MS` | não       | padrão 3600000 (1 h)                                         |
| `JPA_DDL_AUTO`    | não         | padrão `update`                                              |
| `SWAGGER_ENABLED` | não         | padrão `false`                                               |

    SPRING_PROFILES_ACTIVE=prod DB_USERNAME=demo DB_PASSWORD=... JWT_SECRET=... java -jar target/*.jar

No perfil `prod` o H2 console fica desligado e os usuários de demonstração **não** são criados;
cadastre-os via `POST /auth/register`.

### Docker Compose (app + PostgreSQL)
    cp .env.example .env     # preencha DB_PASSWORD e JWT_SECRET
    docker compose up --build

## Testes
    mvn test

Os testes usam o perfil `test` (H2 isolado, ver `src/test/resources/application-test.properties`):

- `JwtServiceTest` – geração, validação, expiração, assinatura adulterada, chave fraca
- `AuthControllerTest` – registro e login (sucesso, conflito, credenciais inválidas, validação)
- `ProductControllerTest` – CRUD, 401 sem/ com token inválido ou expirado, 403 no delete para USER
- `UserRepositoryTest`, `ProductRepositoryTest` – persistência e unicidade de username
- `LocalProfileTest`, `ProdProfileConfigTest` – garantem H2 no `local` e PostgreSQL/segredos por ambiente no `prod`

## Fluxo de teste da API
1. POST /auth/login com {"username":"admin","password":"admin123"}
2. Copie o token retornado
3. No Swagger, clique em "Authorize" e cole o token (sem o prefixo "Bearer")
4. Teste os endpoints de /products

## Via curl
    TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
      -d '{"username":"admin","password":"admin123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
    curl -X POST localhost:8080/products -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' -d '{"name":"Teclado","price":199.90}'
    curl localhost:8080/products -H "Authorization: Bearer $TOKEN"
