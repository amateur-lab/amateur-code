package lab.amateur.learning.others.scala

import scala.collection.mutable
import scala.jdk.CollectionConverters.SeqHasAsJava

object PinyinSplitter {

  /** 最大音节长度 */
  private val MaxLength = 6

  /** 合法拼音音节集合（不可变，线程安全） */
  private val pinyinSet: Set[String] = {
    val set = mutable.HashSet.empty[String]
    initPinyinSet(set)
    set.toSet // 转为不可变，后续只读
  }

  /**
   * 初始化合法拼音音节集合
   */
  private def initPinyinSet(set: mutable.HashSet[String]): Unit = {
    val initials = Seq(
      "", "b", "p", "m", "f", "d", "t", "n", "l",
      "g", "k", "h", "j", "q", "x",
      "zh", "ch", "sh", "r", "z", "c", "s"
    )
    val finalsBasic = Seq(
      "a", "o", "e", "i", "u", "v",
      "ai", "ei", "ui", "ao", "ou", "iu",
      "ie", "ve", "er",
      "an", "en", "in", "un", "vn",
      "ang", "eng", "ing", "ong"
    )
    val finalsCombined = Seq(
      "ia", "iao", "ian", "iang", "iong",
      "ua", "uo", "uai", "uan", "uang", "ueng",
      "van"
    )

    for (ini <- initials; fin <- finalsBasic) addValidCombo(set, ini, fin)
    for (ini <- initials; fin <- finalsCombined) addValidCombo(set, ini, fin)
    addSpecificPinyin(set)
  }

  /**
   * 按拼音规则添加一个声韵组合
   */
  private def addValidCombo(set: mutable.HashSet[String], ini: String, fin: String): Unit = {
    if (ini.isEmpty) {
      // 零声母拼写规则
      fin match {
        case "i" => set += "yi"
        case "ia" => set += "ya"
        case "ie" => set += "ye"
        case "iao" => set += "yao"
        case "iu" => set += "you"
        case "ian" => set += "yan"
        case "in" => set += "yin"
        case "iang" => set += "yang"
        case "ing" => set += "ying"
        case "iong" => set += "yong"
        case "u" => set += "wu"
        case "ua" => set += "wa"
        case "uo" => set += "wo"
        case "uai" => set += "wai"
        case "ui" => set += "wei"
        case "uan" => set += "wan"
        case "un" => set += "wen"
        case "uang" => set += "wang"
        case "ueng" => set += "weng"
        case "v" => set += "yu"
        case "ve" => set += "yue"
        case "van" => set += "yuan"
        case "vn" => set += "yun"
        case _ => set += fin // a,o,e,ai,ei,ao,ou,an,en,ang,eng,er...
      }
      return
    }

    // 非零声母
    // bpmf + uo → bo, po, mo, fo
    if (fin == "uo" && "bpmf".contains(ini)) {
      set += ini + "o"
      return
    }
    // jqx 后 ü 写成 u
    if ("jqx".contains(ini)) {
      fin match {
        case "v" => set += ini + "u"
        case "ve" => set += ini + "ue"
        case "van" => set += ini + "uan"
        case "vn" => set += ini + "un"
        case _ => set += ini + fin
      }
    } else {
      // 其他声母保留 v
      if (fin == "v" || fin == "ve" || fin == "van" || fin == "vn") {
        set += ini + fin
      } else {
        set += ini + fin
      }
    }
  }

  /**
   * 补充特殊音节
   */
  private def addSpecificPinyin(set: mutable.HashSet[String]): Unit = {
    set ++= Seq("zhi", "chi", "shi", "ri", "zi", "ci", "si", "er")
  }

  /**
   * 尝试将输入字符串按拼音切分。
   *
   * @param input 待切分的全小写字母字符串
   * @return 若可完全切分，返回 Some(拼音音节列表)；否则返回 None
   */
  def split(input: String): Option[List[String]] = {
    if (input == null || input.isEmpty) return Some(List.empty)

    val s = input.toLowerCase
    val n = s.length
    val dp = Array.fill(n + 1)(false)
    val from = new Array[String](n + 1) // 回溯用音节
    dp(0) = true

    var i = 1
    while (i <= n) {
      var l = Math.min(MaxLength, i)
      while (l >= 1 && !dp(i)) {
        val sub = s.substring(i - l, i)
        if (pinyinSet.contains(sub) && dp(i - l)) {
          dp(i) = true
          from(i) = sub
        }
        l -= 1
      }
      i += 1
    }

    if (!dp(n)) None
    else {
      // 回溯，得到反序列表
      val buf = List.newBuilder[String]
      var pos = n
      while (pos > 0) {
        val syllable = from(pos)
        buf += syllable
        pos -= syllable.length
      }
      Some(buf.result().reverse)
    }
  }

  /** 方便 Java 调用的兼容版本（返回 null 表示非拼音） */
  def splitOrNull(input: String): java.util.List[String] = {
    split(input).map(_.asJava).orNull
  }

  // 测试
  def main(args: Array[String]): Unit = {
    val tests = Seq(
      "nihao",
      "xian",
      "shanghai",
      "women",
      "hello",
      "dangan",
      "beijing",
      "zhuang",
      "juede",
      "nvren",
      "lveduo",
      "yuanwang",
      "gongzuo",
      "xianzai",
      "englishword"
    )
    tests.foreach { t =>
      println(s"$t -> ${split(t).getOrElse(Seq("非拼音")).mkString(" ")}")
    }
  }
}
