---
name: kc-who-are-you
description: A skill that creates a summary report with the information about the core session context
---

# Purpose

The core session context are all the instructions you initially read when you start a new conversation. They are the 
instructions that define your identity, your goals, your constraints, the skills, rules, project-defined agents, and 
your performance evaluation criteria. They are also the instructions that define how you should respond to user queries 
and how you should format your answers.

This skill is designed to show a summary report of the core session context so that the user can check if you are
considering all the instructions and capabilities expected from you when executing tasks.

## Instructions

1. Whenever the user asks "who are you?" or "what is your identity?" or similar questions about your identity, you should execute this skill to generate a summary report of the core session context.

## Report

```
## Context Summary 

### Environment: 

<environment>

### Project:

<project> 

#### Modules:

<as bullets, all the name of the modules you know about in the project>

<your_role>

### Your team:

<your_team>

### Your role:

<your_role>

### Key Constraints

<key_constraints>

### Available Skills:

<list only the project-level skills you have, as bullets>

### Available agents:

<list only the project-level agents you have, as bullets>
```

## **Critical** Rules
- Use the format from the `Report` section strictly as defined, do not change the format or add any additional information that is not explicitly requested in the report format.