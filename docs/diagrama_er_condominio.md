# Sistema de Condomínio: diagrama relacional (PostgreSQL)

Banco `db_condominio`, tabelas com prefixo `tb_`, colunas em `snake_case`. Scripts em `db/01_create_database.sql` e `db/02_create_tables.sql`.

```mermaid
erDiagram
    tb_condominium {
        bigint id PK
        varchar(150) name
        varchar(18) cnpj UK
        varchar(20) phone
        varchar(255) address
    }
    tb_user {
        bigint id PK
        varchar(150) name
        varchar(14) cpf UK
        varchar(20) phone
        varchar(255) password
    }
    tb_administrator {
        bigint id PK, FK
        bigint condominium_id FK, UK
    }
    tb_unit {
        bigint id PK
        bigint condominium_id FK
        varchar(20) block
        varchar(20) number
    }
    tb_resident {
        bigint id PK, FK
        bigint unit_id FK
        varchar(20) status
    }
    tb_complaint {
        bigint id PK
        uuid protocol UK
        bigint condominium_id FK
        text description
        varchar(30) category
        varchar(30) location
        varchar(20) status
        timestamp created_at
        date date_event
    }
    tb_action {
        bigint id PK
        bigint complaint_id FK
        text description
        timestamp created_at
    }
    tb_attachment {
        bigint id PK
        bigint complaint_id FK
        varchar(255) file_name
        varchar(100) mime_type
        bigint size
        bytea content
    }
    tb_complaint_status_history {
        bigint id PK
        bigint complaint_id FK
        varchar(20) previous_status
        varchar(20) new_status
        timestamp created_at
        text justification
    }

    tb_user ||--o| tb_administrator : "é um (JOINED)"
    tb_user ||--o| tb_resident : "é um (JOINED)"
    tb_condominium ||--o| tb_administrator : "administra"
    tb_condominium ||--o{ tb_unit : "possui"
    tb_condominium ||--o{ tb_complaint : "recebe"
    tb_unit ||--o{ tb_resident : "mora em"
    tb_complaint ||--o{ tb_action : "ações"
    tb_complaint ||--o{ tb_attachment : "anexos"
    tb_complaint ||--o{ tb_complaint_status_history : "histórico de status"
```

## Decisões do modelo

- **Herança de `User`:** `tb_administrator` e `tb_resident` usam como PK o próprio `id` de `tb_user` (FK com `ON DELETE CASCADE`), que é o mapeamento `@Inheritance(strategy = JOINED)`. Só `tb_user` gera o ID.
- **Anonimato:** `tb_complaint` não tem coluna ligada a usuário.
- **Condomínio único (MVP):** o índice `uq_tb_condominium_single` impede uma segunda linha em `tb_condominium`. Para vários condomínios, remova esse índice.
- **Enums:** colunas `VARCHAR` com `CHECK`, usadas com `@Enumerated(EnumType.STRING)`.
- **`protocol`:** `UUID` único; o `ComplaintTrackingDTO` do morador é buscado por ele.
- **Histórico:** `previous_status` é `NULL` no registro inicial; o `CHECK` exige `justification` não vazia quando `new_status = 'REJECTED'` (a mesma regra do service, reforçada no banco).
- **Exclusão em cascata:** ações, anexos e histórico são apagados junto com a denúncia (`orphanRemoval` no JPA).

## Ajustes no JPA

- `@Table(name = "tb_complaint")` etc. em cada entidade, ou `spring.jpa.hibernate.naming.physical-strategy` padrão do Spring Boot, que já converte `dateEvent` em `date_event`.
- `Attachment.content`: no PostgreSQL, `@Lob byte[]` vira `oid` no Hibernate 6. Para usar `BYTEA`, tire o `@Lob` e anote o campo com `@JdbcTypeCode(SqlTypes.VARBINARY)`.
- Com o schema criado pelos scripts, use `spring.jpa.hibernate.ddl-auto=validate`.
