📖 Project Overview
The S-Emulator is a multi-stage Java project that simulates execution of the S-Language, a theoretical minimal programming model supporting only four primitive instructions.


Throughout the course, the emulator evolves through three main versions:

🖥️ Console Application – foundational engine, XML-based program loading, and command-line interaction.

🎨 JavaFX GUI – interactive desktop environment with debugging, visualization, and expansion.

🌐 HTTP Client-Server – multi-user distributed system deployed on Apache Tomcat 9.



Each stage extends the same modular architecture — maintaining a clean separation between UI, engine, and data-transfer layers.



⚙️ Architecture & Design Decisions

🧠 Core Principles

Engine-Driven Design – all logic encapsulated in a reusable engine module.

DTO Pattern – ProgramDto, InstructionDto, ExecutionDto, etc., bridge between UI / server and engine.

XML Validation – programs validated against schemas (v1–v3) using JAXB for safe mapping.

Thread Isolation – long tasks (load, execute) run in background JavaFX Tasks or async HTTP threads.

MVC / Observer Patterns – ensure clear flow between model updates and UI state.



📦 Installation & Deployment

🔧 Requirements :

Java 21 (JDK 21)

Apache Tomcat 9.0 (for Assignment 3)

Maven / Gradle build support

Windows 10 environment (tested and graded)



S-Emulator/
│

├── engine/

│   ├── model/

│   ├── validation/

│   ├── execution/

│   └── expansion/

│

├── ui-consolel

├── ui-fx/

├── server/

├── client/

└── common/


⚡ Execution Modes:

▶️ Run – Execute the entire program and display output, variable state, and total cycles.

🐞 Debug – Step forward / backward, pause, resume, and observe variable changes in real-time.

💾 History – Browse previous executions with their inputs, outputs, and cycle counts.


