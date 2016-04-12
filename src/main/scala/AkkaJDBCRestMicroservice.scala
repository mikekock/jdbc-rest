import java.net.URLDecoder
import java.sql._
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import spray.json._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContextExecutor

case class SQLPreparedStatementTypeValue(columnType: String, index: Int, value: Any)

case class QuerySQLRequest(sql: String, params: Option[Seq[SQLPreparedStatementTypeValue]])

case class QuerySQLResult(result: Option[Seq[Map[String, Any]]], error: Option[String], message: Option[String], status: Option[String])

case class ExecuteSQLRequest(sql: String, params: Option[Seq[SQLPreparedStatementTypeValue]])

case class ExecuteSQLResult(result: Long, error: Option[String], message: Option[String], status: Option[String])

trait Protocols extends DefaultJsonProtocol {

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case null => JsNull
      case n: Int => JsNumber(n)
      case l: Long => JsNumber(l)
      case s: Short => JsNumber(s)
      case s: String => JsString(s)
      case d: DateTime => JsString(d.toIsoDateTimeString())
      case ts: Timestamp => {
        // Return a timestamp that includes the milliseconds.
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.n")
        JsString(ts.toLocalDateTime().format(formatter))
      }
      case bd: BigDecimal => JsNumber(bd.doubleValue())
      case bd: java.math.BigDecimal => JsNumber(bd.doubleValue())
      case db: Double => JsNumber(db)
      case b: Boolean if b == true => JsTrue
      case b: Boolean if b == false => JsFalse
      case b: scala.Array[Byte] => JsString(java.util.Base64.getEncoder().encodeToString(b))
      case x => serializationError("Do not understand object of type " + x.getClass.getName)
    }

    def read(value: JsValue) = value match {
      case JsNumber(l) => scala.math.BigDecimal(l.bigDecimal)
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
      case x => deserializationError("Do not understand how to deserialize " + x)
    }
  }

  implicit val sqlPreparedStatementTypeValueFormat = jsonFormat3(SQLPreparedStatementTypeValue.apply)
  implicit val querySQLRequestFormat = jsonFormat2(QuerySQLRequest.apply)
  implicit val querySQLResultFormat = jsonFormat4(QuerySQLResult.apply)
  implicit val executeSQLRequestFormat = jsonFormat2(ExecuteSQLRequest.apply)
  implicit val executeSQLResultFormat = jsonFormat4(ExecuteSQLResult.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter

  def getConnection(): Connection = {
    val conn_str = config.getString("db.connection")
    // Get the connection
    try {
      DriverManager.getConnection(conn_str)
    } catch {
      case e: Exception => {
        println("ERROR: No connection: " + e.getMessage)
        throw e
      }
    }
  }

  private def getRSValueWithNull(v: Any, rs: ResultSet): Any = {
    if (rs.wasNull())
      null
    else
      v
  }

  private def getResultSetRows(rs: ResultSet): ListBuffer[Map[String, Any]] = {
    val rows = collection.mutable.ListBuffer[Map[String, Any]]()

    while (rs.next) {
      val meta = rs.getMetaData()
      val columnCount = meta.getColumnCount()
      var cols = collection.mutable.Map[String, Any]()
      var a = 1
      for (a <- 1 to columnCount) {
        if (meta.getColumnType(a) == java.sql.Types.BIGINT || meta.getColumnType(a) == java.sql.Types.INTEGER || meta.getColumnType(a) == java.sql.Types.SMALLINT || meta.getColumnType(a) == java.sql.Types.TINYINT) {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getLong(a), rs)
        }
        else if (meta.getColumnType(a) == java.sql.Types.FLOAT || meta.getColumnType(a) == java.sql.Types.DOUBLE || meta.getColumnType(a) == java.sql.Types.REAL) {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getDouble(a), rs)
        }
        else if (meta.getColumnType(a) == java.sql.Types.TIMESTAMP) {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getTimestamp(a), rs)
        }
        else if (meta.getColumnType(a) == java.sql.Types.DATE) {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getTimestamp(a), rs)
        }
        else if (meta.getColumnType(a) == java.sql.Types.DECIMAL || meta.getColumnType(a) == java.sql.Types.NUMERIC) {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getBigDecimal(a), rs)
        }
        else if (meta.getColumnType(a) == java.sql.Types.BINARY || meta.getColumnType(a) == java.sql.Types.VARBINARY) {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getBytes(a), rs)
        }
        else if (meta.getColumnType(a) == java.sql.Types.BOOLEAN) {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getBoolean(a), rs)
        }
        else {
          cols += meta.getColumnName(a) -> getRSValueWithNull(rs.getString(a), rs)
        }
      }
      rows += cols.toMap
    }
    rows
  }


  private def parseQueryString(query: String): String = {
    query.stripPrefix("Query=")
  }


  private def querySQL(query: String, params: Option[Seq[SQLPreparedStatementTypeValue]]): ToResponseMarshallable = {
    try {
      // Setup the connection
      val conn = getConnection()
      try {
        // Configure to be Read Only
        val statement = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        applyParams(statement, params)

        // Execute Query
        val rs = statement.executeQuery()

        // Iterate Over ResultSet
        QuerySQLResult(Option(getResultSetRows(rs)), Option(""), Option(""), Option("success"))
      }
      finally {
        conn.close
      }
    }
    catch {
      case e: Exception => {
        val msg = "ERROR: " + e.getMessage
        QuerySQLResult(None, Option(msg), Option(""), Option("exception"))
      }
    }
  }

  private def setParam(statement: PreparedStatement, param: SQLPreparedStatementTypeValue): Unit = param.columnType match {
    case "String" => statement.setString(param.index, AnyConversions.getStringValue(param.value))
    case "Number" => statement.setBigDecimal(param.index, AnyConversions.getBigDecimalValue(param.value).bigDecimal)
    case "Boolean" => statement.setBoolean(param.index, AnyConversions.getBooleanValue(param.value))
    case "Timestamp" => statement.setTimestamp(param.index, Timestamp.valueOf(AnyConversions.getLocalDateTime(param.value)))
    case "Binary" => statement.setBytes(param.index, java.util.Base64.getDecoder().decode(AnyConversions.getStringValue(param.value)))
    case _ => deserializationError("Do not understand how to deserialize param")
  }


  private def applyParams(statement: PreparedStatement, params: Option[Seq[SQLPreparedStatementTypeValue]]): Unit = {
    params match {
      case Some(x) => x.foreach(p => setParam(statement, p))
      case None =>
    }

  }

  private def executePreparedUpdate(conn: Connection, query: String, params: Option[Seq[SQLPreparedStatementTypeValue]]): Unit = {
    val statement = conn.prepareStatement(query)
    applyParams(statement, params)
    statement.executeUpdate()
  }

  private def executeSQL(executeSQL: Seq[ExecuteSQLRequest]): ExecuteSQLResult = {
    try {
      // Setup the connection
      val conn = getConnection()
      try {
        conn.setAutoCommit(false)
        // Execute Query
        executeSQL.foreach((x: ExecuteSQLRequest) => executePreparedUpdate(conn, x.sql, x.params))

        conn.commit()

        ExecuteSQLResult(1, Option(""), Option(""), Option("success"))
      }
      finally {
        conn.close
      }
    }
    catch {
      case e: Exception => {
        val msg = "ERROR: " + e.getMessage
        ExecuteSQLResult(0, Option(msg), Option(""), Option("exception"))
      }
    }
  }

  private val queryRoute = {
    path("select" / Rest) { trace =>
      (post & entity(as[QuerySQLRequest])) { query =>
        complete {
          querySQL(query.sql, query.params)
        }
      } ~
        (post & entity(as[String])) { query =>
          complete {
            querySQL(parseQueryString(URLDecoder.decode(query, "UTF-8")), None)
          }
        }
    }
  }

  private val executeRoute = {
    path("execute" / Rest) { trace =>
      (post & entity(as[Seq[ExecuteSQLRequest]])) { executeSeq =>
        complete {
          executeSQL(executeSeq)
        }
      } ~
        (post & entity(as[String])) { executeSeq =>
          complete {
            val q = List(ExecuteSQLRequest(parseQueryString(URLDecoder.decode(executeSeq, "UTF-8")), None))
            executeSQL(q)
          }
        }
    }
  }


  val routes = encodeResponse {
    logRequestResult("akka-jdbc-rest") {
      pathPrefix(config.getString("http.url_base"))(queryRoute ~ executeRoute)
    }
  }
}


object AkkaJDBCRestMicroservice extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Class.forName(config.getString("db.driver")).newInstance
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
