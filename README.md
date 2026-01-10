# 🛒 Loja SAS - IPCA

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white)
![Material Design 3](https://img.shields.io/badge/Material%20Design%203-757575?style=for-the-badge&logo=material-design&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)
![Jira](https://img.shields.io/badge/Jira-2596be?style=for-the-badge&logo=jira&logoColor=white)
![Milanote](https://img.shields.io/badge/Milanote-000000?style=for-the-badge&logo=milanote&logoColor=white)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow?style=for-the-badge)


Uma aplicação móvel Android desenvolvida para os **Serviços de Ação Social do IPCA (SAS-IPCA)**. O objetivo é facilitar a gestão, requisição e entrega de cabazes à comunidade académica, conectando beneficiários e colaboradores através de um sistema integrado de pedidos e gestão de stock.

---

## 📖 Sobre o Projeto

O **Loja Social SAS - IPCA** é uma aplicação móvel desenvolvida para modernizar e otimizar a gestão da Loja Social do Instituto Politécnico do Cávado e do Ave (IPCA).

**O Problema**

Atualmente, a gestão da loja é realizada de forma manual, recorrendo a cadernos para registar pedidos e ficheiros Excel para controlar o stock. Com o crescimento do número de beneficiários, este método tornou-se moroso, suscetível a erros e difícil de manter atualizado, limitando a capacidade de resposta dos serviços.

**A Solução**

Este projeto propõe uma plataforma digital centralizada que substitui os processos manuais, garantindo maior eficiência, transparência e fiabilidade. A aplicação conecta diretamente os dois principais intervenientes:

* **Serviços de Ação Social (SAS):** Permite gerir o inventário, validar candidaturas, organizar campanhas e calendarizar entregas de forma eficiente.

* **Estudantes (Beneficiários e Candidatos):** Facilita a submissão de candidaturas e permite efetuar pedidos de produtos (bens alimentares e higiene) diretamente pelo telemóvel.

**Contexto** 

Projeto desenvolvido no âmbito da Licenciatura em Engenharia de Sistemas Informáticos, com o objetivo de ser implementado como ferramenta oficial de apoio à comunidade escolar.

---

## ✨ Funcionalidades Principais

A aplicação foi desenhada para servir dois perfis distintos, cada um com ferramentas específicas para o seu papel no ecossistema da Loja Social:

### 🎓 Para o Beneficiário (Estudante)

O foco é a autonomia e a facilidade de acesso aos apoios.

* **🛒 Loja Digital & Pedidos:** Acesso a um catálogo de produtos (alimentares, higiene, etc.) onde pode adicionar itens ao carrinho e efetuar a requisição diretamente pelo telemóvel.
* **📄 Candidatura Integrada:** Submissão do processo de candidatura a beneficiário (ou renovação), incluindo o envio de comprovativos e dados pessoais, sem necessidade de deslocações.
* **📅 Gestão de Entregas:** Receção de notificações sobre o estado do pedido e a possibilidade de negociar/reagendar a data e hora de levantamento do cabaz em caso de impedimento.
* **🔔 Alertas Inteligentes:** Notificações automáticas para lembrar o dia da recolha ou avisar sobre o estado de aprovação da candidatura.

### 🏢 Para o Colaborador (SAS)

O foco é a eficiência operacional e o controlo rigoroso de recursos.

* **📦 Gestão de Stock e Validades:** Controlo total do inventário com alertas de validade e stock baixo. O sistema ajuda a priorizar a saída de produtos com prazo mais curto para evitar desperdício.
* **✅ Validação de Processos:** Dashboard para aprovar ou rejeitar candidaturas e pedidos de apoio, com acesso rápido ao histórico e perfil de cada beneficiário.
* **🚚 Logística de Distribuição:** Ferramentas para agendar entregas, registar levantamentos efetuados e gerir faltas.
* **📢 Campanhas e Doações:** Criação e gestão de campanhas de recolha de alimentos, incluindo o registo de doadores e contabilização de produtos angariados.
* **📊 Relatórios:** Consulta de relatórios de stock, pedidos e entregas, facilitando o acompanhamento e controlo das operações.

---

## 📱 Screenshots

| Ecrã de Login | Catálogo de Produtos | Novo Pedido | Histórico |  Perfil |
|:---:|:---:|:---:|:---:|:---:|
| <img src="https://i.postimg.cc/FHGfmHFR/login.jpg" width="220"/> | <img src="https://i.postimg.cc/dVn7vVQ0/home.jpg" width="220"/> | <img src="https://i.postimg.cc/RZG39ZC3/order.jpg" width="220"/> | <img src="https://i.postimg.cc/Hk3JHkWY/history.jpg" width="220"/> | <img src="https://i.postimg.cc/FHGfmHFz/profile.jpg" width="220"/> |

---

## 🛠️ Tecnologias e Ferramentas

Este projeto utiliza uma stack moderna, focada na eficiência e escalabilidade:

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **Interface (UI):** [Jetpack Compose](https://developer.android.com/jetpack/compose)
    * **Material Design 3:** Sistema de design de última geração.
    * **Icons Extended:** Biblioteca completa de ícones.
* **Arquitetura:** MVVM (Model-View-ViewModel).
* **Navegação:** Jetpack Navigation Compose (Single Activity Architecture).
* **Backend & Cloud (Firebase):**
    * **Firestore:** Base de dados NoSQL para sincronização em tempo real (Snapshots).
    * **Authentication:** Gestão segura de identidades.
    * **Analytics:** Monitorização de uso da app.
    * **Cloud Messaging:** Suporte para notificações push.
* **Concorrência:** Kotlin Coroutines & Flows.
* **Gestão e Design:**
    * **Planeamento:** Jira (Metodologia Scrum/Sprints).
    * **Prototipagem:** Figma (Mockups da interface).
    * **Organização:** Milanote (Brainstorming e documentação).
---

## 📐 Arquitetura do Sistema

O projeto segue as melhores práticas de desenvolvimento Android moderno, utilizando a arquitetura **MVVM (Model-View-ViewModel)** para garantir a separação de responsabilidades e testabilidade.

Segue também uma **Arquitetura Cliente-Servidor**, onde a aplicação móvel (Cliente) processa a interface e comunica com o Firebase (Servidor/BaaS) para gerir a lógica de negócio e persistência de dados.

O desenvolvimento foi suportado por uma análise técnica rigorosa, incluindo:

* **Diagramas de Casos de Uso:** Para mapear as interações de cada ator (Colaborador, Beneficiário, Interessado).
* **BPMN (Processos de Negócio):** Modelação dos fluxos de aprovação de candidaturas e gestão de entregas.
* **Diagramas de Estado:** Para garantir a consistência no ciclo de vida das encomendas (ex: *Criado* -> *Pendente* -> *Entregue*).

> 📂 **Nota:** A documentação técnica completa (incluindo diagramas de classes e sequência) pode ser consultada no [Relatório Técnico do Projeto](https://alunosipca-my.sharepoint.com/:b:/g/personal/a27959_alunos_ipca_pt/IQDKvXW7zvhKSZfeokhADn6MAdsBSH8pjXdeSEeSxhOaIxs?e=jDub18).

---

## 🎨 Design e Prototipagem

Antes da implementação, toda a interface foi desenhada e validada focando na experiência do utilizador (UX).
O protótipo de alta fidelidade pode ser consultado aqui:

[![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)](https://www.figma.com/design/jfj0dO8HPIz8bu9YZgXEsM/Loja-Social-IPCA?node-id=0-1&t=1ZBrMYnPpT8N5RFY-1)

> **Nota:** O design sistema segue as diretrizes do Material 3 para garantir consistência e acessibilidade.

---

## 📂 Estrutura de Pastas

```text
ipca.project.lojasas
├── models                      # Data Classes
│   ├── Authentication          # Dados temporários de Login/Registo
│   ├── Campaign                # Gestão de campanhas de recolha
│   ├── Candidature             # Processo de candidatura socioeconómica
│   ├── CartManager             # Singleton para gestão do Carrinho Local
│   ├── Category                # Categorias de produtos da Loja
│   ├── Delivery                # Logística e confirmação de entregas
│   ├── Donation                # Registo de doações recebidas
│   ├── Notification            # Estrutura de alertas Push
│   ├── Order                   # Pedidos e itens requisitados
│   ├── Product                 # Produtos e gestão de Lotes (FIFO)
│   ├── ProposalDelivery        # Negociação de datas (Chat)
│   └── User                    # Perfil de Utilizador (Staff/Estudante)
│
├── ui                          # Interface (Compose) e ViewModels
│   ├── authentication          # Ecrãs de Login
│   ├── beneficiary             # Área do Beneficiário (Loja, Pedidos)
│   ├── candidature             # Fluxo de Candidatura
│   ├── collaborator            # Área do Colaborador (Stock, Validação)
│   ├── components              # Widgets reutilizáveis (Cards, Inputs)
│   └── theme                   # Tema Material 3 (Cores, Tipografia)
│
├── MyFirebaseMessagingService  # Gestão de Notificações Push (FCM)
└── MainActivity                # Ponto de entrada da aplicação
```

---

## 🚀 Como Executar

Instruções passo-a-passo para configurar o ambiente de desenvolvimento e testar a aplicação:

### Pré-requisitos
* **IDE:** [Android Studio](https://developer.android.com/studio).
* **JDK:** Versão 17 (compatível com a stack atual).
* **Dispositivo:** Emulador ou telemóvel físico com Android 7.0 (API 24) ou superior.

### Instalação

1.  **Clonar o Repositório:**
    ```bash
    git clone https://github.com/HugoSpecial/Loja-SAS-IPCA.git
    ```

2.  **Configurar o Firebase (Crucial):**
    * O projeto utiliza serviços Google (Auth, Firestore, Messaging) que requerem autenticação.
    * É necessário obter o ficheiro `google-services.json`
    * **Onde colocar:** Colar o ficheiro na diretoria `app/` do projeto:
      `ipca.project.lojasas/app/google-services.json`

3.  **Compilar e Correr:**
    * Abrir o projeto no Android Studio.
    * Aguardar a sincronização do Gradle (pode demorar alguns minutos).
    * Selecionar o dispositivo alvo e clicar em **Run** (▶️).

> **Nota:** Se houver erros relacionados com o `google-services`, verificar se o `package_name` no ficheiro JSON corresponde a `ipca.project.lojasas`.

---

## ⚖️ Autoria e Contexto Académico

Este projeto foi desenhado e implementado no âmbito das Unidades Curriculares de **Projeto Aplicado** e **Programação de Dispositivos Móveis** da Licenciatura em **Engenharia de Sistemas Informáticos**, no **Instituto Politécnico do Cávado e do Ave (IPCA)**, ano letivo **2025/2026**.

O software foi desenvolvido como **proposta de solução oficial** para a modernização dos serviços do SAS-IPCA.

### 👨‍💻 Equipa de Desenvolvimento

| Nome | Número |
| :--- | :---: |
| **Duarte Pereira** | 27959 |
| **Hugo Especial** | 27963 |
| **Paulo Gonçalves** | 27966 |
| **José Pires** | 27968 |
| **Marco Cardoso** | 27969 |
| **Hugo Pereira** | 27970 |

---

### 📄 Declaração de Direitos de Autor (Copyright)

**Copyright © 2025 Grupo 5. Todos os direitos reservados.**

O código fonte e os recursos gráficos deste projeto são propriedade intelectual dos autores acima listados.
A utilização, cópia, modificação ou distribuição deste software para fins comerciais é estritamente proibida sem autorização prévia por escrito dos autores.


É concedida permissão ao **IPCA** para utilização deste projeto para fins de avaliação, demonstração académica e arquivo institucional.

Autoriza-se ainda o uso interno e sem restrições por parte dos **Serviços de Ação Social (SAS-IPCA)** para a eventual implementação e gestão operacional da Loja Social.

---
