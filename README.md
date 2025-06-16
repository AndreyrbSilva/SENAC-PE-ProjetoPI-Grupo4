## 🏢 SENKAS - Projeto de Auditoria e Compliance Senac
Este projeto é um sistema Android desenvolvido para o gerenciamento de pilares e atividades no contexto do Compliance do Senac. Ele facilita a criação, acompanhamento e automação de processos com funcionalidades como geração de relatórios, controle de progresso e perfis com permissões distintas.

## 📝 Visão Geral
O sistema foi criado para organizar e automatizar tarefas relacionadas à conformidade (compliance), permitindo o gerenciamento de pilares, subpilares, ações e atividades por diferentes perfis de usuários. Também possibilita a análise de logs e geração de dashboards com base na produção de atividades realizadas.

## ✅ Funcionalidades Principais
Cadastro e gerenciamento de pilares, subpilares, ações e atividades

Atribuição de atividades a funcionários com progresso individual

Processamento de porcentagem de execução por atividade realizada

Geração de relatórios de produção e desempenho

Perfis de acesso diferenciados com funcionalidades específicas

Banco de dados inicial pré-carregado com usuários fictícios para testes

Interface responsiva e moderna com animações

## 🛠 Tecnologias Utilizadas
Mobile: Kotlin

Banco de Dados: SQLite (com Room – Android Jetpack)

Persistência: Room + TypeConverters personalizados

Concorrência: Kotlin Coroutines

API: Python (Hospedada no Python Anywhere)

## 🗃️ Banco de Dados
O banco de dados local (appsenkaspi.db) é criado automaticamente ao iniciar o app, com dados pré-populados por meio de um RoomDatabase.Callback.

Principais entidades:
PilarEntity, SubpilarEntity, AcaoEntity, AtividadeEntity

FuncionarioEntity (com perfis, login e permissões)

Entidades relacionais: AcaoFuncionarioEntity, AtividadeFuncionarioEntity

ChecklistItemEntity, RequisicaoEntity

O acesso aos dados é realizado por meio de DAOs (Data Access Objects) para garantir separação de responsabilidades e acesso seguro às operações de CRUD.

Claro! Abaixo está uma **documentação técnica** estruturada para você colar diretamente no seu `README.md`, descrevendo a API de geração de relatórios:


# 📊 API de Geração de Relatórios

Esta API fornece endpoints para gerar relatórios nos formatos **PDF**, **Word** e **Excel**, com base em dados de pilares, ações e atividades de um sistema de gestão. A API suporta relatórios gerais e por pilar.

## 🚀 Endpoints

### `GET /`

Retorna uma mensagem de status indicando que a API está online.

**Resposta:**

```json
{
  "mensagem": "API SENKAS rodando com sucesso!"
}
```

### `POST /relatorio/pdf`

Gera e retorna um relatório em **PDF** com base nos dados enviados.

**Corpo da requisição (JSON):**

```json
{
  "tipoRelatorio": "geral" | "pilar",
  "pilares": [...],
  "pilarId": "123" // apenas se tipoRelatorio for "pilar"
}
```

**Resposta:** Arquivo `.pdf` para download.

---

### `POST /relatorio/word`

Gera e retorna um relatório em **Word (.docx)** com os mesmos dados da rota anterior.

**Corpo da requisição (JSON):** igual ao endpoint `/relatorio/pdf`

**Resposta:** Arquivo `.docx` para download.


### `POST /relatorio/excel`

Gera e retorna um relatório em **Excel (.xlsx)**.

**Corpo da requisição (JSON):** igual ao endpoint `/relatorio/pdf`

**Resposta:** Arquivo `.xlsx` para download.


### `GET /relatorio/download/<nome_arquivo>`

Permite o download de arquivos já gerados anteriormente, localizados na pasta `relatorios`.

**Parâmetros de URL:**

* `nome_arquivo`: nome do arquivo a ser baixado.

**Resposta:** Arquivo solicitado como download.

## 📁 Estrutura Esperada dos Dados

Exemplo de entrada para `tipoRelatorio: "geral"`:

```json
{
  "tipoRelatorio": "geral",
  "pilares": [
    {
      "id": "1",
      "nome": "Governança",
      "descricao": "Descrição do pilar",
      "dataInicio": "2024-01-01",
      "dataPrazo": "2024-12-31",
      "status": "Em andamento",
      "criadoPor": "Administrador",
      "acoes": [
        {
          "nome": "Planejamento Estratégico",
          "descricao": "Descrição da ação",
          "status": "Ativa",
          "atividades": [
            {
              "nome": "Análise de Riscos",
              "status": "Concluida",
              "responsavel": "João"
            }
          ]
        }
      ]
    }
  ]
}
```

Para `tipoRelatorio: "pilar"`, envie apenas um pilar no array `pilares` e informe também `pilarId`.

## 📄 Formatos de Relatório

* **PDF**: Contém informações dos pilares, ações e atividades, com gráficos de status (pizza e barras).
* **Word**: Documento estruturado com tabelas, seções e gráficos embutidos.
* **Excel**:
  
  * `geral`: lista os pilares com dados resumidos.
  * `pilar`: lista as ações e atividades detalhadamente.

## 🛠️ Bibliotecas Utilizadas

![image](https://github.com/user-attachments/assets/66e204b3-c490-4565-8ada-4bf28aeadb4c)

* **Flask**: Framework web.
* **FPDF**: Geração de PDFs.
* **python-docx**: Geração de documentos Word.
* **pandas + openpyxl**: Manipulação de planilhas Excel.
* **matplotlib**: Geração de gráficos.

## 📂 Diretórios Importantes

* `relatorios/`: Arquivos de saída (.pdf, .docx, .xlsx)
* `graficos/`: Imagens dos gráficos gerados (usadas nos relatórios)

## ✅ Validações e Regras

* `"tipoRelatorio"` deve ser `"geral"` ou `"pilar"`.
* Se `"pilar"`, é obrigatório fornecer `pilarId` e um único item no array `pilares`.
* Geração de gráficos ocorre apenas quando há dados válidos para isso.

## 🚀 Como Executar o Projeto
Clone o repositório:

bash
Copiar
Editar
git clone https://github.com/seu-usuario/appsenkaspi.git
cd appsenkaspi
Abra o projeto no Android Studio.

Sincronize o Gradle e construa o projeto.

Execute em um emulador Android ou dispositivo físico.

## 📦 Requisitos do Sistema

Dependência 1: API externa em Python

Dependência 2: Banco de dados SQLite (Room)

Dependência 3: Android Studio + ambiente Kotlin configurado

## 👥 Contribuições dos Membros do Grupo
Saulo – Responsável pela implementação de funcionalidades e atualização do banco de dados.

Andrey – Responsável pelo design da interface, animações visuais e aprimoramento da experiência do usuário.

Matheus – Responsável pela integração e implementações de API.

João – Responsável pela gestão de erros e correção de bugs.

Lucas – Responsável pela criação das telas iniciais do escopo do projeto.

Vitor – Responsável pela criação das telas iniciais do escopo do projeto.

Carlos - Responsável por revisar e atualizar a documentação do projeto.

## Links
[Notion](https://www.notion.so/Sistema-de-Ouvidoria-do-SENAC-1a6cf81c640d8080b6d3f4cd051740fa?pvs=4) - Documentação de Requisitos

[Trello](https://trello.com/invite/b/67ec3fa72b0388fbbbc61382/ATTI65e83e7e71fbc4b6e05d9965b82e2f0fDD78737D/projeto-integrador) - Ferramenta de Gerenciamento

[Figma](https://www.figma.com/design/hzO79KFKydvrj3Y4NApAbE/Projeto-do-Grupo-4?node-id=2-6) - Prototipação de Alto Nivel

[Miro](https://miro.com/app/board/uXjVIFXhKPU=/?inviteKey=VzVFWVVWTWl6dU13cFJuN2wvVjE4UytHVXlhak5KNWtXblhKY2x5ZkRkd0pXdlYvVDlqZTZHdjlJeWRBTzB0S1kzT1E2RE84bEt2VDkzTzNXVXBHaURrZlk4NlVtVmc4SzJVQllVM0hPZHlYQ0hNcjQ0a0JJR2Mra0tnVFNwTFJyVmtkMG5hNDA3dVlncnBvRVB2ZXBnPT0hdjE=) - Personas e Historia
