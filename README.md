## 📱 SENKAS - Projeto de Auditoria e Compliance Senac

Este é um aplicativo Android nativo desenvolvido no Android Studio, utilizando Kotlin e Room, voltado para o gerenciamento de pilares e atividades no contexto de auditoria e compliance do Senac.


---

## 📝 Visão Geral

O sistema SENKAS foi criado para organizar e automatizar tarefas relacionadas à conformidade (compliance), permitindo que gestores e colaboradores acompanhem o progresso de ações e atividades associadas a diferentes pilares da instituição. Também inclui geração de relatórios, controle de produção e visualização analítica via dashboards.


---

## ✅ Funcionalidades Principais

📌 Cadastro e gerenciamento de pilares, subpilares, ações e atividades

👤 Atribuição de atividades a funcionários com controle de progresso

📈 Cálculo automático da porcentagem de execução por atividade

🧾 Geração de relatórios (PDF, Word e Excel) via API externa

🔐 Perfis de acesso com permissões distintas

📂 Banco de dados inicial com dados fictícios para testes

🖥️ Interface moderna, responsiva e com animações nativas



---

## 🛠 Tecnologias Utilizadas

Mobile

Kotlin

Android Studio (versão mínima: 2022.1.1)

Room (Jetpack)

Coroutines


Backend (Relatórios)

Python (API hospedada no PythonAnywhere)

Flask, FPDF, python-docx, pandas, openpyxl, matplotlib



---

## 🗃️ Banco de Dados

Utilizamos SQLite com Room, criando a base local automaticamente ao iniciar o app. O banco já vem pré-populado para testes e conta com diversas entidades e relacionamentos:

Entidades Principais

PilarEntity, SubpilarEntity, AcaoEntity, AtividadeEntity

FuncionarioEntity (login, perfil e permissões)

AcaoFuncionarioEntity, AtividadeFuncionarioEntity

ChecklistItemEntity, RequisicaoEntity



---

## 📊 API de Relatórios

A API externa gera relatórios dinâmicos com base nos dados locais do app. É possível gerar arquivos nos formatos:

PDF: com gráficos de status (pizza e barras)

Word: estruturado em seções e tabelas

Excel: com listas e detalhes de pilares, ações e atividades


Principais Endpoints

GET /
POST /relatorio/pdf
POST /relatorio/word
POST /relatorio/excel
GET /relatorio/download/<nome_arquivo>

> A estrutura de envio inclui tipo do relatório, lista de pilares e, opcionalmente, o ID do pilar.




---

## 📂 Estrutura Esperada dos Dados

{
  "tipoRelatorio": "geral",
  "pilares": [
    {
      "id": "1",
      "nome": "Governança",
      "acoes": [
        {
          "nome": "Planejamento",
          "atividades": [
            {
              "nome": "Análise de Riscos",
              "responsavel": "João"
            }
          ]
        }
      ]
    }
  ]
}


---

## 🚀 Como Configurar o Ambiente de Desenvolvimento

Pré-requisitos

Android Studio 2022.1.1 ou superior

Kotlin SDK

Gradle 7.4+

SDK Android API 30 ou superior

Acesso à internet (para integração com a API externa)


Passos para execução:

1. Clone o repositório:



git clone https://github.com/seu-usuario/appsenkaspi.git
cd appsenkaspi/Mobile

2. Abra o projeto no Android Studio


3. Aguarde o carregamento e sincronize o Gradle


4. Conecte um dispositivo físico ou inicie um emulador


5. Rode o app pelo botão ▶️ “Run”



> A base de dados será criada automaticamente com dados simulados.




---

## 📦 Dependências Importantes

// Room
implementation "androidx.room:room-runtime:2.6.1"
kapt "androidx.room:room-compiler:2.6.1"

// Coroutines
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

// ViewModel & LiveData
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.6.2"

// Networking (se houver)
implementation "com.squareup.retrofit2:retrofit:2.9.0"


---

## 👥 Equipe e Responsabilidades

Saulo – Banco de dados e funcionalidades

Andrey – UI/UX e animações

Matheus – Integrações com a API

João – Tratamento de erros e debug

Lucas – Telas e funcionalidades iniciais

Vitor – Telas e funcionalidades iniciais

Carlos – Documentação técnica
## Links
[Notion](https://www.notion.so/Sistema-de-Ouvidoria-do-SENAC-1a6cf81c640d8080b6d3f4cd051740fa?pvs=4) - Documentação de Requisitos

[Trello](https://trello.com/invite/b/67ec3fa72b0388fbbbc61382/ATTI65e83e7e71fbc4b6e05d9965b82e2f0fDD78737D/projeto-integrador) - Ferramenta de Gerenciamento

[Figma](https://www.figma.com/design/hzO79KFKydvrj3Y4NApAbE/Projeto-do-Grupo-4?node-id=2-6) - Prototipação de Alto Nivel

[Miro](https://miro.com/app/board/uXjVIFXhKPU=/?inviteKey=VzVFWVVWTWl6dU13cFJuN2wvVjE4UytHVXlhak5KNWtXblhKY2x5ZkRkd0pXdlYvVDlqZTZHdjlJeWRBTzB0S1kzT1E2RE84bEt2VDkzTzNXVXBHaURrZlk4NlVtVmc4SzJVQllVM0hPZHlYQ0hNcjQ0a0JJR2Mra0tnVFNwTFJyVmtkMG5hNDA3dVlncnBvRVB2ZXBnPT0hdjE=) - Personas e Historia
