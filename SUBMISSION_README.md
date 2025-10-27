# S-Emulator Assignment 3 - HTTP Client-Server Implementation

## Information
**Student ID:** [213533516]
**Email:** [hadeelmhameed00@gmail.com]
**GitHub Repository:
 (https://github.com/hadeelmhameed2/S-Emulator)


## Project Overview
This project implements Assignment 3 - a multi-user HTTP Client-Server version of the S-Emulator system using Apache Tomcat 9.0 and JavaFX for the client interface.

### Key Features
- ✅ Multi-user system with credit management
- ✅ 4 Architecture generations (I-IV) with different instruction support
- ✅ Program and function repository with validation
- ✅ Real-time dashboard updates (1 second refresh rate)
- ✅ Execution tracking and history per user
- ✅ Debug mode with step-by-step execution
- ✅ Credit-based execution model


## Architecture & Design Decisions

### 1. Module Organization
**Decision:** Three-module Gradle project
- **Engine Module:** Standalone JAR library containing execution logic
- **Server Module:** WAR file depending on engine
- **Client Module:** Fat JAR depending on engine for models only

### 2. Multi-User & Thread Safety
**Decision:** Used `ConcurrentHashMap` for user/program storage and `AtomicInteger` for counters

### 3. Architecture Credit System
**Decision:** Charge architecture cost upfront + 1 credit per cycle during execution

### 4. QUOTE Implementation
**Decision:** QUOTE instructions are NOT expanded during expansion phase - they execute at runtime

### 5. Real-Time Updates
**Decision:** Client polls server every 1 second using `ScheduledExecutorService`

### 6. No Data Persistence
**Decision:** All data stored in-memory only (as required by assignment)

### 7. JSON Communication
**Decision:** Used Google Gson library for JSON serialization

### 8. File Upload
**Decision:** Implemented multipart/form-data parsing without Apache Commons FileUpload

### 9. Program Validation
**Decision:** Three-tier validation during upload:
1. XML parsing validation
2. Function dependency validation (all called functions must exist)
3. No duplicate function names across the system

### 10. Debug Session Management
**Decision:** Server maintains debug sessions per user in a HashMap

## User Manual

### 1. Login Screen

**First Time Users:**
1. Launch the client application (`run.bat` or `java -jar SEmulatorClient.jar`)
2. Enter a unique username (e.g., "alice")
3. Click "Login"
4. You'll receive 1000 starting credits

**Error Cases:**
- Username already taken: Choose a different name
- Server not running: Start Tomcat first

### 2. Dashboard Screen
The dashboard shows 4 main sections:

#### A. Users Table (Top)
Shows all connected users with:
- Username
- Programs uploaded
- Functions contributed
- Current credits
- Credits used
- Total executions (run + debug)

**Actions:**
- Click a user row to see their execution history

#### B. Programs Table (Left Middle)
Shows all uploaded programs with:
- Program name
- Owner username
- Instruction count (degree 0)
- Maximum degree
- Execution count
- Average credit cost

**Actions:**
- Select a program and click "Run Program" to execute

#### C. Functions Table (Right Middle)
Shows all functions in the system:
- Function name
- Parent program (where it was defined)
- Owner username
- Instruction count
- Maximum degree

#### D. Execution History Table (Bottom)
Shows execution history for selected user (defaults to yourself):
- Run number
- Program/Function name
- Is it a function? (True/False)
- Architecture used (I, II, III, IV)
- Expansion degree
- Output value (y)
- Cycles consumed
- Credits used

**Auto-Refresh:** All tables refresh every 1 second automatically

### 3. Upload Program

1. Click "Upload Program" button
2. Select an XML file from your computer
3. Wait for validation:
   - ✅ Success: Program added to system
   - ❌ Error: Message shows what's wrong

**Validation Rules:**
- Program name must be unique
- All called functions must exist in the system (in this file or previously uploaded)
- Cannot redefine existing functions
- Must be valid XML format

### 4. Add Credits

1. Click "Add Credits" button
2. Enter amount (e.g., 1000)
3. Credits immediately added to your account