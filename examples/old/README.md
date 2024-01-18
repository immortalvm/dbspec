# Old example

NB. The example in this folder has not been maintained.

## Setup

### Docker

Docker is required for running the DBMS used for testing.

### PostgreSQL Client

The PostgreSQL client is is required for setting up a test database
On Debian and Debian-based distros the package ```postgresql-client-14``` can be installed.

## Setting up PostgreSQL for testing

To run the test script, we need to set up PostgreSQL in Docker container.
```shell
docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=geheim -v postgres:/var/lib/postgresql/data postgres:14
```
