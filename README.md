# Eye...

Parte 1: Implementação do Código (Quarkus)
1. Criação do Projeto

Para criar o projeto com as dependências corretas (suporte a REST e serialização JSON), você pode usar o Maven no seu terminal:
Bash

mvn io.quarkus.platform:quarkus-maven-plugin:create \
    -DprojectGroupId=br.com.faculdade \
    -DprojectArtifactId=sistema-mensagens \
    -Dextensions="resteasy-reactive,resteasy-reactive-jackson"
cd sistema-mensagens

2. Modelo de Dados (Mensagem.java)

Crie esta classe no pacote principal. Mantive os atributos públicos conforme solicitado na sua especificação, garantindo que o Jackson (biblioteca de JSON do Quarkus) consiga serializar e desserializar corretamente a data (LocalDateTime).
Java

package br.com.faculdade;

import java.time.LocalDateTime;

public class Mensagem {
    public Long id;
    public String remetente;
    public String conteudo;
    public LocalDateTime timestamp;

    // Construtor vazio exigido por frameworks de serialização JSON
    public Mensagem() {}

    public Mensagem(Long id, String remetente, String conteudo) {
        this.id = id;
        this.remetente = remetente;
        this.conteudo = conteudo;
        this.timestamp = LocalDateTime.now();
    }
}

3. Resource / Controller REST (MensagemResource.java)

Esta classe será responsável por receber as requisições HTTP e manipulá-las usando a lista em memória. Note o uso de AtomicLong para simular a geração de IDs de forma segura (thread-safe), e os códigos de status HTTP explícitos.
Java

package br.com.faculdade;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Path("/mensagens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MensagemResource {

    // Armazenamento em memória conforme exigido
    private final List<Mensagem> repositorio = new ArrayList<>();
    private final AtomicLong geradorId = new AtomicLong(1);

    @GET
    public Response listarTodas() {
        return Response.ok(repositorio).build(); // Retorna 200 OK
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Optional<Mensagem> mensagem = repositorio.stream()
                .filter(m -> m.id.equals(id))
                .findFirst();

        if (mensagem.isPresent()) {
            return Response.ok(mensagem.get()).build(); // Retorna 200 OK
        }
        return Response.status(Response.Status.NOT_FOUND).build(); // Retorna 404 Not Found
    }

    @POST
    public Response enviarMensagem(Mensagem novaMensagem) {
        novaMensagem.id = geradorId.getAndIncrement();
        novaMensagem.timestamp = LocalDateTime.now();
        repositorio.add(novaMensagem);
        
        return Response.status(Response.Status.CREATED) // Retorna 201 Created
                       .entity(novaMensagem)
                       .build();
    }

    @DELETE
    @Path("/{id}")
    public Response removerMensagem(@PathParam("id") Long id) {
        boolean removido = repositorio.removeIf(m -> m.id.equals(id));
        
        if (removido) {
            return Response.noContent().build(); // Retorna 204 No Content (sucesso sem corpo)
        }
        return Response.status(Response.Status.NOT_FOUND).build(); // Retorna 404 se não achar
    }
}

Para rodar o projeto:
Execute mvn compile quarkus:dev no terminal. A aplicação estará rodando em http://localhost:8080.
Parte 2: Modelo do Relatório (README.md)

Copie o conteúdo abaixo e cole no seu README.md no GitHub. Atenção: Você precisará tirar os prints do Postman e adicionar as imagens onde indicado [INSERIR IMAGEM AQUI].
Markdown

# Sistema de Troca de Mensagens Distribuído com Quarkus

Este projeto implementa uma aplicação RESTful utilizando o framework **Quarkus** para simular a comunicação direta entre processos em um ambiente de sistemas distribuídos. O objetivo principal é demonstrar na prática o modelo de passagem de mensagens (*send/receive*) encapsulado no protocolo HTTP.

## 4.1 Arquitetura da Solução

Em um sistema distribuído, os nós (processos) não compartilham memória e dependem estritamente da troca de mensagens pela rede para coordenar suas ações. Nossa arquitetura estabelece uma relação Cliente-Servidor clássica baseada em comunicação síncrona.

### Fluxo de uma Requisição `POST /mensagens`

1. **Sender (Cliente/Postman):** Atua como o emissor da mensagem. O usuário compõe a carga útil (o JSON contendo `remetente` e `conteudo`) e inicia a operação.
2. **Encapsulamento HTTP:** A aplicação cliente encapsula esses dados no "Corpo" (Body) de uma requisição HTTP. O cabeçalho (Header) `Content-Type: application/json` é anexado para informar ao receptor a estrutura do dado. Essa requisição trafega sobre uma conexão TCP/IP, garantindo a entrega dos pacotes.
3. **Receiver (Servidor Quarkus):** A aplicação Quarkus, através do servidor embutido e da especificação JAX-RS (RESTEasy), intercepta a requisição na porta 8080. Ela desencapsula o HTTP, faz o *parse* (desserialização) do JSON para o objeto Java `Mensagem`, atribui o ID e o Timestamp, e armazena o estado em memória.
4. **Resposta (Acknowledge):** O Quarkus monta uma resposta HTTP com o código de status apropriado, confirmando ao *Sender* que a mensagem foi recebida e processada com sucesso.

### Mapeamento Teórico: Operações Send e Receive

A especificação JAX-RS mapeia perfeitamente as primitivas abstratas de comunicação (`Send` e `Receive`) para os métodos padrão do HTTP:

* **`POST` (Ação de Send / Enviar):** Representa a injeção de uma nova mensagem no sistema. O processo cliente "envia" (Send) os dados para o processo servidor.
* **`GET` (Ação de Receive / Consultar):** O processo cliente solicita ativamente a leitura do estado atual ou a recuperação de mensagens previamente enviadas. Funciona como uma operação de "receber/puxar" dados (Pull).
* **`DELETE` (Ação de Compensação / Remoção):** Mapeado para um comando de exclusão remota, instruindo o processo receptor a apagar um registro específico em sua memória.

## 4.2 Evidências de Funcionamento

Abaixo estão as evidências dos testes realizados via Postman simulando um cliente distribuído.

### Tabela de Testes e Endpoints

| Método | Endpoint | Descrição da Operação | Status Retornado |
| :--- | :--- | :--- | :--- |
| **POST** | `/mensagens` | Envio de uma nova mensagem ao servidor. | `201 Created` |
| **GET** | `/mensagens` | Recuperação de todas as mensagens em memória. | `200 OK` |
| **GET** | `/mensagens/{id}` | Busca de uma mensagem específica por um ID válido. | `200 OK` |
| **GET** | `/mensagens/{id}` | Busca de uma mensagem com ID não existente. | `404 Not Found` |
| **DELETE**| `/mensagens/{id}` | Deleção de uma mensagem por ID. | `204 No Content` |
| **DELETE**| `/mensagens/{id}` | Tentativa de deletar um ID não existente. | `404 Not Found` |

### Justificativa dos Status Codes HTTP

O uso correto de Status Codes é fundamental para garantir que o cliente (Sender) entenda o que aconteceu do lado do servidor (Receiver) sem precisar inspecionar a fundo a resposta de texto:

* **`200 OK`:** Usado nas consultas (`GET`). Indica que a requisição do cliente foi processada e as informações solicitadas foram localizadas e devolvidas com sucesso no corpo da resposta.
* **`201 Created`:** Retornado exclusivamente no `POST`. A semântica do protocolo HTTP determina que este código deve ser usado quando uma requisição resulta na criação bem-sucedida de um novo recurso (neste caso, uma nova mensagem no servidor).
* **`204 No Content`:** Retornado no `DELETE` bem-sucedido. Ele informa ao cliente: *"Sua solicitação de deleção foi um sucesso, mas não há nenhum arquivo ou conteúdo adicional que eu precise te devolver além desta confirmação"*.
* **`404 Not Found`:** Utilizado no `GET /{id}` e `DELETE /{id}`. Este código sinaliza um erro de estado: o servidor entendeu a requisição do cliente perfeitamente, mas o ID fornecido não existe na memória do processo receptor.

### Prints do Postman

#### 1. Criando uma Mensagem (POST)
> *[INSERIR IMAGEM AQUI - Print do Postman mostrando o Body da requisição e o Status 201 Created no canto superior direito]*

#### 2. Listando Todas as Mensagens (GET)
> *[INSERIR IMAGEM AQUI - Print do Postman mostrando o array de JSON retornado e o Status 200 OK]*

#### 3. Buscando ID Inexistente (GET / 404)
> *[INSERIR IMAGEM AQUI - Print do Postman tentando acessar /mensagens/999 e recebendo o Status 404 Not Found]*

#### 4. Apagando uma Mensagem (DELETE)
> *[INSERIR IMAGEM AQUI - Print do Postman na rota DELETE recebendo o Status 204 No Content]*

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>br.com.faculdade</groupId>
    <artifactId>sistema-mensagens</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>quarkus</packaging>

    <properties>
        <compiler-plugin.version>3.15.0</compiler-plugin.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
        <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
        <quarkus.platform.version>3.34.1</quarkus.platform.version>
        <skipITs>true</skipITs>
        <surefire-plugin.version>3.5.4</surefire-plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>${quarkus.platform.group-id}</groupId>
                <artifactId>${quarkus.platform.artifact-id}</artifactId>
                <version>${quarkus.platform.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-jsonb</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest-client-jsonb</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-jaxb</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-jsonb</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-client-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-links</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-jsonp</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-client-jsonb</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-client-jaxb</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-client-mutiny</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest-client-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-mutiny</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-arc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>${quarkus.platform.group-id}</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.platform.version}</version>
                <extensions>true</extensions>
            </plugin>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${compiler-plugin.version}</version>
                <configuration>
                    <parameters>true</parameters>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${surefire-plugin.version}</version>
                <configuration>
                    <argLine>@{argLine}</argLine>
                    <systemPropertyVariables>
                        <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                        <maven.home>${maven.home}</maven.home>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>${surefire-plugin.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <argLine>@{argLine}</argLine>
                    <systemPropertyVariables>
                        <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                        <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                        <maven.home>${maven.home}</maven.home>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>native</id>
            <activation>
                <property>
                    <name>native</name>
                </property>
            </activation>
            <properties>
                <quarkus.package.jar.enabled>false</quarkus.package.jar.enabled>
                <skipITs>false</skipITs>
                <quarkus.native.enabled>true</quarkus.native.enabled>
            </properties>
        </profile>
    </profiles>
</project>
