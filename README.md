# vat-registered-companies

This is the vat-registered-companies repo.

It has been upgraded to Java 11, sbt 1.9.9, Scala 2.13.12, Play 3.0.

Both unit tests and Integration tests exist for this repo.
To run Integration tests, run
sbt it:test
or
within SBT Shell: IntegrationTest / test

# Run Services

You can run services locally through Service Manager:
```
sm2 --start VAT_REG_CO_ALL
```

To run repo on port 8731:
```
sbt run
```

## Testing and coverage

To run tests use:
```
sbt test
```

To run integration tests use:
```
sbt it:test
```
or
```
sbt IntegrationTest / test
```

To run tests and coverage, use the following commands in order:
```
sbt clean coverage test it:test coverageReport
```
or
```
sbt clean coverage test IntegrationTest / test coverageReport
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
