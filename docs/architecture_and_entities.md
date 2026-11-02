# Padrões de Arquitetura e Modelagem de Entidades (K03)

## 1. Arquitetura em Camadas
O projeto seguirá o padrão arquitetural em camadas, refletindo o modelo MVC adaptado para APIs REST. A divisão de pacotes será:

*   `entities`: Classes de domínio mapeadas para o banco de dados (JPA/Hibernate).
*   `repositories`: Interfaces do Spring Data JPA para comunicação direta com o banco de dados.
*   `services`: Camada de regras de negócio (validações, lógicas, chamadas a repositórios).
*   `resources` (ou `controllers`): Controladores REST (endpoints) que recebem requisições HTTP e retornam respostas JSON.
*   `dto` (Data Transfer Objects): Classes auxiliares para transitar dados entre a API e o cliente, garantindo que as `entities` não sejam expostas diretamente.

## 2. Padrão Estrutural das Entidades JPA

Para manter a consistência com o banco de dados (H2 agora e PostgreSQL no futuro), todas as entidades devem seguir estritamente as regras abaixo:

### Passo 1: Anotações de Classe e Tabela
Todas as classes devem ser anotadas com `@Entity` e `@Table`. O nome da tabela deve usar o prefixo `tb_` e formato `snake_case`.
```java
@Entity
@Table(name = "tb_condominium")
public class Condominium { ... }
```

### Passo 2: Chave Primária (PK)
O ID deve ser do tipo `Long`, anotado com `@Id` e com geração automática de valor via `IDENTITY` (deixando o banco gerenciar a sequência).
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

### Passo 3: Mapeamento de Colunas (Fields)
Propriedades devem ser nomeadas em `camelCase` no Java. O Spring Boot já converte automaticamente para `snake_case` no banco (ex: `dateEvent` vira `date_event`).
Regras de validação do banco (NOT NULL, UNIQUE) devem ser refletidas na anotação `@Column`.
```java
@Column(nullable = false, length = 150)
private String name;

@Column(unique = true, length = 18)
private String cnpj;
```

### Passo 4: Construtores e Encapsulamento
*   Criar um construtor vazio (exigência do JPA).
*   Criar um construtor com todos os argumentos.
*   Gerar todos os `Getters` e `Setters` (pode ser feito via IDE ou utilizando a biblioteca Lombok `@Getter` e `@Setter`).
*   Implementar `hashCode` e `equals` comparando **apenas** pelo campo `id`.

### Passo 5: Relacionamentos e Herança
*   Para usuários (Administrador e Morador), utilizar a herança `@Inheritance(strategy = InheritanceType.JOINED)` na classe mãe `User`.
*   Para os Enums (como `Status`), utilizar sempre `@Enumerated(EnumType.STRING)` para salvar o texto legível no banco.
*   Arquivos binários (`bytea`) não devem usar a anotação `@Lob` no Hibernate 6, devendo utilizar `@JdbcTypeCode(SqlTypes.VARBINARY)`.