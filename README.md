## Usage

Start services with sbt:

```
$ sbt
> ~re-start
```

With the service up, you can start sending HTTP requests:

```


curl -X POST -H 'Content-Type: application/json' http://localhost:9000/query/trace1 -d '{"sql": "SELECT * FROM table"}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/execute/trace2 -d '{"sql": ["SELECT * Fexecute able","other"]}'
curl -X POST -H 'Content-Type: application/json' http://localhost:9000/execute/trace3 -d '{"sql": ["INSERT INTO tet (firstname) VALUES ('"'"'Mike'"'"')"]}'



## Author & license


For licensing info see LICENSE file in project's root directory.

