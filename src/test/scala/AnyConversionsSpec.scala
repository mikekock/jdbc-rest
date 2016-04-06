import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by mike on 4/6/16.
  */
class AnyConversionsSpec extends FlatSpec with Matchers {
  "AnyConversions.getBooleanValue" should "convert 'true' to true" in {
    AnyConversions.getBooleanValue("true") should be (true)
    info("OK")
  }

  it should "convert 'false' to false" in {
    AnyConversions.getBooleanValue("false") should be (false)
    info("OK")
  }

  it should "convert true to true" in {
    AnyConversions.getBooleanValue(true) should be (true)
    info("OK")
  }

  it should "convert false to false" in {
    AnyConversions.getBooleanValue(false) should be (false)
    info("OK")
  }

  "AnyConversions.getStringValue" should "convert 'true' to 'true'" in {
    AnyConversions.getStringValue("true") should be ("true")
    info("OK")
  }

  it should "convert '' to ''" in {
    AnyConversions.getStringValue("") should be ("")
    info("OK")
  }

  "AnyConversions.getBigDecimalValue" should "convert '1.23456789' to 1.23456789" in {
    assert(AnyConversions.getBigDecimalValue("1.23456789").doubleValue() === 1.23456789 /*+- 0.0001*/)
    info("OK")
  }
}
