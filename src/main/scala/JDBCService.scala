import java.sql.{Connection, DriverManager, ResultSet, Timestamp, PreparedStatement}

import com.typesafe.config.Config

import scala.collection.mutable.ListBuffer

case class SQLPreparedStatementTypeValue(columnType: String, index: Int, value: Option[Any])

case class QuerySQLRequest(sql: String, params: Option[Seq[SQLPreparedStatementTypeValue]])

case class QuerySQLResult(result: Option[Seq[Map[String, Any]]], error: Option[String], message: Option[String], status: Option[String])

case class ExecuteSQLRequest(sql: String, params: Option[Seq[SQLPreparedStatementTypeValue]])

case class ExecuteSQLResult(result: Long, error: Option[String], message: Option[String], status: Option[String])

class JDBCService(c: Config) {
  var con: Config = c

  def getConnection(): Connection = {
    val conn_str = con.getString("db.connection")
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

  def querySQL(query: String, params: Option[Seq[SQLPreparedStatementTypeValue]]): QuerySQLResult = {
    try {
      // Setup the connection
      val conn = getConnection()
      try {
        // Configure to be Read Only
        val statement = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        JDBCService.applyParams(statement, params)

        // Execute Query
        val rs = statement.executeQuery()

        // Iterate Over ResultSet
        QuerySQLResult(Option(JDBCService.getResultSetRows(rs)), Option(""), Option(""), Option("success"))
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

  def executeSQL(executeSQL: Seq[ExecuteSQLRequest]): ExecuteSQLResult = {
    try {
      // Setup the connection
      val conn = getConnection()
      try {
        conn.setAutoCommit(false)
        // Execute Query
        executeSQL.foreach((x: ExecuteSQLRequest) => JDBCService.executePreparedUpdate(conn, x.sql, x.params))

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
}

object JDBCService {
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
        val columnType = meta.getColumnType(a)
        val columnName = meta.getColumnName(a)
        columnType match {
          case java.sql.Types.BIGINT => cols += columnName -> getRSValueWithNull(rs.getLong(a), rs)
          case java.sql.Types.INTEGER => cols += columnName -> getRSValueWithNull(rs.getLong(a), rs)
          case java.sql.Types.SMALLINT => cols += columnName -> getRSValueWithNull(rs.getLong(a), rs)
          case java.sql.Types.TINYINT => cols += columnName -> getRSValueWithNull(rs.getLong(a), rs)
          case java.sql.Types.FLOAT => cols += columnName -> getRSValueWithNull(rs.getDouble(a), rs)
          case java.sql.Types.DOUBLE => cols += columnName -> getRSValueWithNull(rs.getDouble(a), rs)
          case java.sql.Types.REAL => cols += columnName -> getRSValueWithNull(rs.getDouble(a), rs)
          case java.sql.Types.TIMESTAMP => cols += columnName -> getRSValueWithNull(rs.getTimestamp(a), rs)
          case java.sql.Types.DATE => cols += columnName -> getRSValueWithNull(rs.getTimestamp(a), rs)
          case java.sql.Types.DECIMAL => cols += columnName -> getRSValueWithNull(rs.getBigDecimal(a), rs)
          case java.sql.Types.NUMERIC => cols += columnName -> getRSValueWithNull(rs.getBigDecimal(a), rs)
          case java.sql.Types.BINARY => cols += columnName -> getRSValueWithNull(rs.getBytes(a), rs)
          case java.sql.Types.VARBINARY => cols += columnName -> getRSValueWithNull(rs.getBytes(a), rs)
          case java.sql.Types.BOOLEAN => cols += columnName -> getRSValueWithNull(rs.getBoolean(a), rs)
          case _ => cols += columnName -> getRSValueWithNull(rs.getString(a), rs)
        }
      }
      rows += cols.toMap
    }
    rows
  }

  private def valueOrNull(v: Option[Any]) : Any = {
    v match {
      case Some(r) => r
      case None => null
    }
  }

  private def setParam(statement: PreparedStatement, param: SQLPreparedStatementTypeValue): Unit = {
    val v = valueOrNull(param.value)
    param.columnType match {
      case "String" => statement.setString(param.index, AnyConversions.getStringValue(v))
      case "Number" => {
        if (v == null)
          statement.setBigDecimal(param.index, null)
        else
          statement.setBigDecimal(param.index, AnyConversions.getBigDecimalValue(v).bigDecimal)
      }
      case "Boolean" => statement.setBoolean(param.index, AnyConversions.getBooleanValue(v))
      case "Timestamp" => statement.setTimestamp(param.index, Timestamp.valueOf(AnyConversions.getLocalDateTime(v)))
      case "Binary" => statement.setBytes(param.index, java.util.Base64.getDecoder().decode(AnyConversions.getStringValue(v)))
      //case _ => deserializationError("Do not understand how to deserialize param")
    }
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


}
