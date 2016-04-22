import java.sql.{Timestamp}
import java.net.URLDecoder
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import spray.json._

import scala.concurrent.ExecutionContextExecutor

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
      case b: Array[Byte] => JsString(java.util.Base64.getEncoder().encodeToString(b))
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

  private def parseQueryString(query: String): String = {
    query.stripPrefix("Query=")
  }

  private def JDBCConnectionString() : String = {
    config.getString("db.connection")
  }

  def querySQL = JDBCService.querySQL(JDBCConnectionString, _: String, _: Option[Seq[SQLPreparedStatementTypeValue]])

  def executeSQL = JDBCService.executeSQL(JDBCConnectionString, _: Seq[ExecuteSQLRequest])

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
