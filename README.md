# GZAC Temporary Data Container

Temporary data container to exchange data between processes during a running case within the Generiek Zaakafhandelcomponent (GZAC) ecosystem.

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Data Types](#data-types)
- [Contributing](#contributing)

## Overview

The GZAC Temporary Data Container is a specialized component designed to handle temporary data exchange between processes during case execution. It serves as an intermediary storage solution for data that:

- Needs to be shared between multiple processes within a case
- Exists temporarily during case processing
- Doesn't belong to the permanent case record

Temporary data for a case is stored in a table in the GZAC Database.

### Key Features
- **Process-to-Process Communication**: Seamless data exchange between different process instances
- **Type Safety**: Support for various data types
- **Scoped Access**: Data isolation per case scope
- **Integration Ready**: Built for GZAC and Valtimo ecosystem compatibility


## Getting Started

### Prerequisites

- Java 11 or higher
- Spring Boot 2.7+
- GZAC backend 12.12 or higher

### Installation
Checkout the github repository.
Compile the library as JAR using the gradle script and include in your GZAC Implementation.

### Usage
Use temporary-data container value resolver prefix 'tzd' to store or retrieve values from the temporary-data container in GZAC forms. For example, tzd:someValue.

## Data Types

### Supported Data Types

The temporary data container supports various data types:

- **String**: Text data, JSON strings, XML
- **Number**: Integer, Long, Double, BigDecimal
- **Boolean**: True/false values
- **Date/Time**: LocalDate, LocalDateTime, ZonedDateTime
- **Complex Objects**: Custom DTOs, Maps, Lists


## Contributing
### Testing

```bash
# Run unit tests
./gradlew test

# Run integration tests
./gradlew test -Dtest.profile=integration
```

### Code Style

- Follow Kotlin coding conventions
- Use SonarQube for code quality
- Maintain test coverage above 80%
- Document public APIs

### Pull Request Process

1. Ensure tests pass
2. Update documentation
3. Add changelog entry
4. Submit PR with clear description

## License

This project is licensed under the EUPL 1.2 License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/generiekzaakafhandelcomponent/temporary-data/issues)
- **Documentation**: [GZAC Documentation](https://docs.valtimo.nl)
- **Community**: [GZAC Community](https://github.com/generiekzaakafhandelcomponent)

## Changelog

### Version 0.1.0
- Initial release
- Basic temporary data storage
- GZAC integration
### Version 0.2.0
- bugfix nested properties
- add support for JSONPath notation
- add license headers


---

*This project is part of the Generiek Zaakafhandelcomponent (GZAC) ecosystem, providing temporary data storage capabilities for case management and process automation.*
