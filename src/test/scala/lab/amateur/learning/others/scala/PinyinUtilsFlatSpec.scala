package lab.amateur.learning.others.scala

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PinyinUtilsFlatSpec extends AnyFlatSpec with Matchers {

  // ---------- isPinyin ----------

  "isPinyin" should "return true for valid single-syllable strings" in {
    val valid = Seq("a", "ba", "ni", "hao", "xian", "zhuang")
    valid.foreach { s => PinyinUtils.isPinyin(s) shouldBe true }
  }

  it should "return true for valid multi-syllable strings" in {
    val valid = Seq("nihao", "shanghai", "women", "nvren", "lveduo",
      "beijing", "gongzuo", "xianzai", "yuanwang", "juede")
    valid.foreach { s => PinyinUtils.isPinyin(s) shouldBe true }
  }

  it should "return true for dangan (ambiguous but still valid)" in {
    PinyinUtils.isPinyin("dangan") shouldBe true
  }

  it should "return false for English words" in {
    val words = Seq("hello", "world", "kevin", "englishword")
    words.foreach { w => PinyinUtils.isPinyin(w) shouldBe false }
  }

  it should "return false for strings with invalid syllables" in {
    val invalid = Seq("rai", "abc", "womyn", "beijinng", "zhuaang")
    invalid.foreach { s => PinyinUtils.isPinyin(s) shouldBe false }
  }

  it should "return false for strings containing non-alpha characters" in {
    val inputs = Seq("123", "a1b", "x y", "n v", "ni hao")
    inputs.foreach { s => PinyinUtils.isPinyin(s) shouldBe false }
  }

  it should "return false for null and empty string" in {
    PinyinUtils.isPinyin(null) shouldBe false
    PinyinUtils.isPinyin("") shouldBe false
  }

  // ---------- split ----------

  "split" should "return correct syllables for single-syllable input" in {
    val cases = Map(
      "a" -> List("a"),
      "ni" -> List("ni"),
      "hao" -> List("hao"),
      "zhuang" -> List("zhuang"),
      "xian" -> List("xian")
    )
    cases.foreach { case (in, expected) =>
      PinyinUtils.split(in) shouldBe Some(expected)
    }
  }

  it should "return correct syllables for multi-syllable input" in {
    val cases = Map(
      "nihao" -> List("ni", "hao"),
      "shanghai" -> List("shang", "hai"),
      "women" -> List("wo", "men"),
      "nvren" -> List("nv", "ren"),
      "lveduo" -> List("lve", "duo"),
      "beijing" -> List("bei", "jing"),
      "gongzuo" -> List("gong", "zuo"),
      "xianzai" -> List("xian", "zai"),
      "yuanwang" -> List("yuan", "wang"),
      "juede" -> List("jue", "de")
    )
    cases.foreach { case (in, expected) =>
      PinyinUtils.split(in) shouldBe Some(expected)
    }
  }

  it should "apply longest match for ambiguous boundaries (e.g., dangan -> dan gan)" in {
    val result = PinyinUtils.split("dangan")
    result shouldBe defined
    result.get should contain inOrderOnly("dan", "gan")
  }

  it should "return None for strings that cannot be fully split" in {
    val invalid = Seq("hello", "kevin", "rai", "abc", "123", "x y", "womyn",
      "beijinng", "zhuaang", "a1b", "n v")
    invalid.foreach { s => PinyinUtils.split(s) shouldBe None }
  }

  it should "return Some(empty list) for empty string" in {
    PinyinUtils.split("") shouldBe Some(List.empty)
  }

  it should "return Some(empty list) for null input" in {
    PinyinUtils.split(null) shouldBe Some(List.empty)
  }

  // 一致性
  "Consistency" should "hold: split returns Some(list) if isPinyin returns true" in {
    val testStrings = Seq(
      "a", "ni", "nihao", "xian", "shanghai", "dangan", "women",
      "hello", "kevin", "rai", "abc", "", "123", "x y"
    )
    testStrings.foreach { s =>
      val canSplit = PinyinUtils.split(s).isDefined
      val canIdentify = PinyinUtils.isPinyin(s)
      withClue(s"Failed for input '$s': split.isDefined=$canSplit, isPinyin=$canIdentify") {
        canSplit shouldBe canIdentify
      }
    }
  }
}
