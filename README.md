## Usage

Start services with sbt:

```
$ sbt
> ~re-start
```

With the service up, you can start sending HTTP requests:

```
mysql --user=root --password=123456 scala

curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace1 -d '[{"sql": "CREATE TABLE IF NOT EXISTS jdbc_rest_test (id INT NOT NULL, s VARCHAR(255), dttm DATETIME, ts TIMESTAMP, b TINYINT, d DOUBLE, dc DECIMAL(40,20), bi BIGINT, PRIMARY KEY (id))"}]'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace1 -d '[{"sql": "INSERT INTO jdbc_rest_test (id, s, dttm, ts, b, d, dc, bi) VALUES (1, '"'One', '2016-01-02 01:23:45', '2016-01-03 1:23:45.123456', 1, 1.2345, 1.23456789, 123456789012345)"'"}, {"sql": "INSERT INTO jdbc_rest_test (id, s, dttm, ts, b, d, dc, bi) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", "params": [{"columnType": "Number", "index" : 1, "value": 2},{"columnType": "String", "index": 2, "value": "Two"},{"columnType": "Timestamp", "index": 3, "value": "2016-01-03T01:23:45.0"},{"columnType": "Timestamp", "index": 4, "value": "2016-01-03T01:23:45.123456"},{"columnType": "Number", "index": 5, "value": 0},{"columnType": "Number", "index": 6, "value": 222.12345},{"columnType": "Number", "index": 7, "value": 222.123456789},{"columnType": "Number", "index": 8, "value": 222222222222}]}]'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace1 -d '[{"sql": "INSERT INTO jdbc_rest_test (id) VALUES (3)"}]'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace1 -d '{"sql": "SELECT * FROM jdbc_rest_test"}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace3 -d '{"sql": "SELECT * FROM jdbc_rest_test WHERE id = ?", "params":[{"columnType": "Number", "index": 1, "value": 1}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/select/trace3 -d '{"sql": "SELECT * FROM jdbc_rest_test WHERE s = ?", "params":[{"columnType": "String", "index": 1, "value": "One"}]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace1 -d '[{"sql": "DELETE FROM jdbc_rest_test"}]'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/base/execute/trace1 -d '[{"sql": "DROP TABLE jdbc_rest_test"}]'


## Author & license


For licensing info see LICENSE file in project's root directory.
