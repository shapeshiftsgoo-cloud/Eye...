# Eye...

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

> <img width="1366" height="614" alt="POST" src="https://github.com/user-attachments/assets/61c0d6ee-073f-4598-9ffa-af26c868feb0" />

#### 2. Listando Todas as Mensagens (GET)

> <img width="1366" height="615" alt="GET ALL" src="https://github.com/user-attachments/assets/a7644911-2b67-4f83-9e1e-e90e94d024ab" />
> <img width="1366" height="606" alt="GET ID" src="https://github.com/user-attachments/assets/a7c62d24-89df-4989-bdcd-c3b969f3e747" />


#### 3. Buscando ID Inexistente (GET / 404)

> <img width="1366" height="611" alt="GET NONE" src="https://github.com/user-attachments/assets/f1de6af5-f820-4b31-82b8-20fb851f9d55" />

#### 4. Apagando uma Mensagem (DELETE)

> <img width="1366" height="613" alt="Delete" src="https://github.com/user-attachments/assets/f915c6a5-002b-47af-8b5c-02b8f3660dbf" />
> <img width="1365" height="611" alt="DELETE Nonw" src="https://github.com/user-attachments/assets/68954353-60ae-4df7-b0e7-7aba6a4b3bdf" /
