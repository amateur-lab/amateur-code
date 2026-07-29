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

  // 其他边界测试可自行添加，注意期望值中不再有尾部空格
}
