import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Convert Any types (usually from JSon) into appropriate type.
object AnyConversions {
  def getStringValue(v: Any): String = {
    val i = v match {
      case x: String => x
      case _ => ""
    }
    i
  }

  def getBigDecimalValue(v: Any): BigDecimal = {
    val i = v match {
      case x: BigDecimal => x
      case x: Double => BigDecimal(x)
      //case x: Float => BigDecimal(x)
      case x: Long => BigDecimal(x)
      case x: Int => BigDecimal(x)
      case s: String => BigDecimal(s)
      case _ => BigDecimal(0)
    }
    i
  }

  def getBooleanValue(v: Any): Boolean = {
    val i = v match {
      case x: Boolean => x
      case s: String => s.toBoolean
      case _ => false
    }
    i
  }

  def getLocalDateTime(v: Any): LocalDateTime = {
    val i = v match {
      case x: LocalDateTime => x
      case s:String => LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.n"))
      case _ => LocalDateTime.now()
    }
    i
  }
}
