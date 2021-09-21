### Run stress tests

```bash
mvn clean install
java -jar ./stress-tests/target/stress-tests.jar
```
Run some specific test case

```bash
mvn clean install
java -jar ./stress-tests/target/stress-tests.jar -t <test_name>
```
