# Contributing to AiCMM

Thank you for your interest in contributing to the Agent Capability Maturity Model! This project aims to create a shared, evidence-based framework for evaluating AI agents, and we welcome contributions from the community.

## Ways to Contribute

### 1. Framework Development
- Refine dimension definitions and scoring rubrics
- Propose industry-specific adaptations (healthcare, manufacturing, defense, etc.)
- Add evidence guidelines and assessment templates
- Review and improve the formal specification

### 2. Tooling & Code
- Implement scoring engines, inspectors, and Agent Card generators
- Build integrations with A2A, MCP, and other protocols
- Create visualizations (radar charts, capability resumes)
- Improve CLI usability

### 3. Examples & Case Studies
- Score real-world agents and share Agent Cards
- Document capability profiles for well-known systems
- Create industry benchmark profiles

### 4. Documentation
- Improve clarity of framework descriptions
- Add diagrams and visual aids
- Translate documentation

## Getting Started

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create a branch** for your work: `git checkout -b feature/your-feature`
4. **Make changes** — follow the coding guidelines below
5. **Test** your changes: `mvn clean test`
6. **Commit** with a clear message
7. **Push** and open a Pull Request

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- Git

### Build
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

## Coding Guidelines

- **Java 17+** features encouraged (records, sealed classes, pattern matching)
- **Package**: `org.aicmm.*`
- **Testing**: JUnit 5 with meaningful test names
- **Documentation**: Javadoc for public APIs
- **Style**: Standard Java conventions, 4-space indentation

## Commit Messages

Use clear, descriptive commit messages:
```
Add scoring rubric for Autonomy dimension

- Define Level 0-5 observable behaviors
- Add evidence requirements for each level
- Include IT ops scenario examples
```

## Code of Conduct

We are committed to providing a welcoming and inclusive experience for everyone. Please be respectful, constructive, and collaborative in all interactions.

## Questions?

Open a [Discussion](../../discussions) for questions about the framework or implementation approach. Use [Issues](../../issues) for bugs and feature requests.

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
