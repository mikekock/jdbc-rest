## Usage

Start services with sbt:

```
$ sbt
> ~re-start
```

With the service up, you can start sending HTTP requests:

```
mysql --user=root --password=123456 scala

curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace1 -d '[{"sql": "CREATE TABLE IF NOT EXISTS jdbc_rest_test (id INT NOT NULL, s VARCHAR(255), dttm DATETIME, ts TIMESTAMP, b TINYINT, d DOUBLE, dc DECIMAL, bi BIGINT, PRIMARY KEY (id))"}]'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace1 -d '[{"sql": "INSERT INTO jdbc_rest_test (id, s, dttm, ts, b, d, dc, bi) VALUES (1, '"'One', '2016-01-02 01:23:45', '2016-01-03 1:23:45.123456789', 1, 1.2345, 1.23456789, 123456789012345)"'"}, {"sql": "INSERT INTO jdbc_rest_test (id, s, dttm, ts, b, d, dc, bi) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", params: [{"columenType": "Number", "index" : 1, "value": 2}]}]'




curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace1 -d '{"sql": "SELECT * FROM jdbc_rest_test"}'
#curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace2 -d '{"sql": ["INSERT INTO table (firstname) VALUES ('"'"'Mike'"'"')"]}'

curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace3 -d '{"sql": "SELECT * FROM jdbc_rest_test WHERE id = ?", "params":[{"columnType": "Number", "index": 1, "value": 1}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace3 -d '{"sql": "SELECT * FROM jdbc_rest_test WHERE s = ?", "params":[{"columnType": "String", "index": 1, "value": "One"}]}'

curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace4 -d '{"sql": "SELECT * FROM test WHERE id = ?", "params":[{"columnType": "Number", "index": 1, "value": "1"}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace5 -d '{"sql": "SELECT * FROM test WHERE d = ?", "params":[{"columnType": "Number", "index": 1, "value": "1.23456789"}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace6 -d '{"sql": "SELECT * FROM test WHERE id = ? AND d >= ?", "params":[{"columnType": "Number", "index": 2, "value": "1.23456789"}, {"columnType": "Number", "index": 1, "value": "2"}]}'


## Author & license


For licensing info see LICENSE file in project's root directory.

