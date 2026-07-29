package lab.amateur.learning.others.scala

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MessageUtilsSpec extends AnyFlatSpec with Matchers {

  import MessageUtils.parseSwiftMessage

  "parseSwiftMessage" should "handle empty input" in {
    parseSwiftMessage("") shouldBe Nil
  }

  it should "handle only header (shorter than 37)" in {
    parseSwiftMessage("ABC") shouldBe List("ABC")
  }

  it should "remove leading slash from header and strip trailing spaces" in {
    // 37 字节 header: "/HEADER________________________      " (下划线表示非空格，末尾有6个空格？需要数对)
    // 输入构造："/HEADER________________________" + 6个空格？我们直接用之前正确的用例。
    // 简单用例："/HEADER" + 30个下划线 + "  AB" 使总长 37+2=39
    // 这里我们直接使用你给出的用例：
    val msg = "/HEADER________________________      AB"
    val result = parseSwiftMessage(msg)
    // header 去掉 '/' 和尾部空格后为 "HEADER________________________"
    // 第二段 "AB" 无填充
    result shouldBe List("HEADER________________________", "AB")
  }

  // 标记格式测试
  it should "parse tagged format with padding" in {
    val msg = "/HEADER________________________      " + // 37 无前导 /
      "1/AAA                        " // 35
    parseSwiftMessage(msg) shouldBe List("HEADER________________________", "AAA")
  }

  it should "merge tagged segments: no pad then pad" in {
    val msg = "/HEADER________________________      " +
      "1/AAA_NO_PAD_______________________" +
      "1/BBB_HAS_PAD                      "
    // 第二段有尾部空格，但最终 stripTrailing，所以尾部空格被去除
    parseSwiftMessage(msg) shouldBe List(
      "HEADER________________________",
      "AAA_NO_PAD_______________________BBB_HAS_PAD" // 注意尾部空格去掉了
    )
  }

  it should "merge tagged segments: pad then no pad (insert space)" in {
    val msg = "/HEADER________________________      " +
      "1/AAA                              " +
      "1/BBB_NO_PAD_______________"
    parseSwiftMessage(msg) shouldBe List(
      "HEADER________________________",
      "AAA BBB_NO_PAD_______________" // 中间的空格来自合并规则，保留
    )
  }

  // 无标记格式测试
  it should "merge untagged: no-padding continues" in {
    val msg = "/HEADER________________________      " +
      "PART1_NO_PAD_______________________" +
      "PART2_NO_PAD_______________________" +
      "PART3_HAS_PAD                      " +
      "PART_4_NO_PAD"
    parseSwiftMessage(msg) shouldBe List(
      "HEADER________________________",
      "PART1_NO_PAD_______________________PART2_NO_PAD_______________________PART3_HAS_PAD", // 尾部空格被清除
      "PART_4_NO_PAD"
    )
  }

  it should "split untagged on padding boundary" in {
    val msg = "/HEADER________________________      " +
      "PART1_HAS_PAD                      " +
      "PART2_NO_PAD_______________________" +
      "PART3_HAS_PAD                      "
    parseSwiftMessage(msg) shouldBe List(
      "HEADER________________________",
      "PART1_HAS_PAD", // 尾部空格清除
      "PART2_NO_PAD_______________________PART3_HAS_PAD" // 尾部空格清除
    )
  }

  it should "spilt detect untagged format" in {
    val msg = "/HEADER________________________      " +
      "1/PART1_HAS_PAD                    " +
      "PART2_NO_PAD_______________________" +
      "2/PART3_HAS_PAD                    "
    parseSwiftMessage(msg) shouldBe List(
      "HEADER________________________",
      "1/PART1_HAS_PAD", // 尾部空格清除
      "PART2_NO_PAD_______________________2/PART3_HAS_PAD" // 尾部空格清除
    )
  }

  it should "spilt detect tagged format" in {
    val msg = "/HEADER________________________      " +
      "1/PART1_NO_PAD_____________________" +
      "1/ PART1_2_HAS_PAD                 " +
      "2/PART2_HAS_PAD                    " +
      "3/PART3_HAS_PAD                    "
    parseSwiftMessage(msg) shouldBe List(
      "HEADER________________________",
      "PART1_NO_PAD_____________________ PART1_2_HAS_PAD", // 尾部空格清除
      "PART2_HAS_PAD",
      "PART3_HAS_PAD"// 尾部空格清除
    )
  }

  // 在 MessageUtilsSpec 中添加以下测试

  // ================================
  // parseSwiftMessageToMap 测试
  // ================================

  "parseSwiftMessageToMap" should "return empty Map for empty input" in {
    MessageUtils.parseSwiftMessageToMap("") shouldBe Map.empty[String, String]
  }

  it should "handle message with only header (shorter than 37)" in {
    val result = MessageUtils.parseSwiftMessageToMap("SHORT")
    result shouldBe Map("Header" -> "SHORT")
  }

  it should "remove leading slash from header in map" in {
    val msg = "/HEADER________________________      AB"
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1" -> "AB"          // 因为不是标记格式，所以是 Block1
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  // ---------- 标记格式的 Map 测试 ----------

  it should "map tagged format with single block and padding" in {
    val msg = "/HEADER________________________      " +
      "1/AAA                              "
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1"      -> "AAA"
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  it should "map tagged format with multiple tags in increasing order" in {
    val msg = "/HEADER________________________      " +
      "1/AAA                              " +
      "1/111                              " +
      "2/BBB                              " +
      "5/CCC                              "
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1"      -> "AAA 111",
      "Block2"      -> "BBB",
      "Block5"      -> "CCC"
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  it should "map tagged format with merged segments (no pad then pad)" in {
    val msg = "/HEADER________________________      " +
      "1/AAA_NO_PAD_______________________" +
      "1/BBB_HAS_PAD                      "
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1"      -> "AAA_NO_PAD_______________________BBB_HAS_PAD" // 尾部空格已被 strip
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  it should "map tagged format with merged segments (pad then no pad, space inserted)" in {
    val msg = "/HEADER________________________      " +
      "1/AAA                              " +
      "1/BBB_NO_PAD_______________"
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1"      -> "AAA BBB_NO_PAD_______________"
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  // ---------- 无标记格式的 Map 测试 ----------

  it should "map untagged format with multiple blocks" in {
    val msg = "/HEADER________________________      " +
      "PART1_HAS_PAD                      " +
      "PART2_NO_PAD_______________________" +
      "PART3_HAS_PAD                      "
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1" -> "PART1_HAS_PAD",               // 尾部空格已去除
      "Block2" -> "PART2_NO_PAD_______________________PART3_HAS_PAD"
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  it should "map untagged format with no-padding continuing" in {
    val msg = "/HEADER________________________      " +
      "PART1_NO_PAD_______________________" +
      "PART2_NO_PAD_______________________" +
      "PART3_HAS_PAD                      "
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1" -> "PART1_NO_PAD_______________________PART2_NO_PAD_______________________PART3_HAS_PAD"
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  it should "map untagged format with trailing short segment" in {
    val msg = "/HEADER________________________      " +
      "SHORT_PART1_______________" +
      "END"
    val expected = Map(
      "Header" -> "HEADER________________________",
      "Block1" -> "SHORT_PART1_______________END"
    )
    MessageUtils.parseSwiftMessageToMap(msg) shouldBe expected
  }

  // ---------- 格式回退时的 Map 测试 ----------

  it should "fallback to untagged map when tags are decreasing" in {
    val msg = "HEADER________________________" +
      "2/AAA                        " +
      "1/BBB                        " +
      "3/CCC                        "
    val result = MessageUtils.parseSwiftMessageToMap(msg)
    result should contain key "Header"
    result should contain key "Block1"
    // 因为回退为无标记格式，所以不会有 "1", "2" 这样的键
    result.keys should not contain "1"
  }

  it should "fallback to untagged map when a segment lacks a valid tag" in {
    val msg = "HEADER________________________" +
      "1/AAA                        " +
      "NOT_A_TAG______________________" +
      "2/BBB                        "
    val result = MessageUtils.parseSwiftMessageToMap(msg)
    result should contain key "Header"
    result.keys should not contain "1"
  }

  // ---------- 边界情况 ----------

  it should "preserve original header in map (with leading slash removal)" in {
    val headerWithSlash = "/HEADER" + " " * 31  // 共37字符，尾部空格
    val msg = headerWithSlash + "1/AAA                        "
    val result = MessageUtils.parseSwiftMessageToMap(msg)
    result("Header") shouldBe headerWithSlash.substring(1).stripTrailing()
  }

  it should "handle message with only one short segment after header" in {
    val msg = "/HEADER________________________      " + "AB"
    val result = MessageUtils.parseSwiftMessageToMap(msg)
    result shouldBe Map(
      "Header" -> "HEADER________________________",
      "Block1" -> "AB"
    )
  }

  // 其他边界测试可自行添加，注意期望值中不再有尾部空格
}
