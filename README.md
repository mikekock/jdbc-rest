## Usage

Start services with sbt:

```
$ sbt
> ~re-start
```

With the service up, you can start sending HTTP requests:

```


curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace1 -d '{"sql": "SELECT * FROM table"}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace2 -d '{"sql": ["INSERT INTO table (firstname) VALUES ('"'"'Mike'"'"')"]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace3 -d '{"sql": "SELECT * FROM test WHERE firstname = ?", "params":[{"columnType": "String", "index": 1, "value": "Mike"}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace4 -d '{"sql": "SELECT * FROM test WHERE id = ?", "params":[{"columnType": "Number", "index": 1, "value": "1"}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace5 -d '{"sql": "SELECT * FROM test WHERE d = ?", "params":[{"columnType": "Number", "index": 1, "value": "1.23456789"}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace6 -d '{"sql": "SELECT * FROM test WHERE id = ? AND d >= ?", "params":[{"columnType": "Number", "index": 2, "value": "1.23456789"}, {"columnType": "Number", "index": 1, "value": "2"}]}'


## Author & license


For licensing info see LICENSE file in project's root directory.

