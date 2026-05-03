package lab.amateur.learning.others.scala

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 * 拼音工具对象 —— 基于 Trie 的高效拼音判定与切分。
 *
 * 特性：
 * - 不可变 Trie 存储合法音节，线程安全。
 * - 核心匹配基于 String.charAt，不产生临时对象。
 * - isPinyin 返回 Boolean；split 返回 Option[List[String]]。
 */
object PinyinUtils {

  private class TrieNode {
    val children: Array[TrieNode] = new Array[TrieNode](26)
    var isEnd: Boolean = false
  }

  /** 根节点 */
  private val root: TrieNode = new TrieNode()

  /** 最大音节长度 */
  private val MaxLen = 6

  private val syllables: Array[String] = Array(
    "a", "ai", "an", "ang", "ao",
    "ba", "bai", "ban", "bang", "bao", "bei", "ben", "beng", "bi", "bian", "biao", "bie", "bin", "bing", "bo", "bu",
    "ca", "cai", "can", "cang", "cao", "ce", "cen", "ceng", "cha", "chai", "chan", "chang", "chao", "che", "chen", "cheng",
    "chi", "chong", "chou", "chu", "chua", "chuai", "chuan", "chuang", "chui", "chun", "chuo", "ci", "cong", "cou", "cu",
    "cuan", "cui", "cun", "cuo",
    "da", "dai", "dan", "dang", "dao", "de", "dei", "den", "deng", "di", "dia", "dian", "diao", "die", "ding", "diu", "dong",
    "dou", "du", "duan", "dui", "dun", "duo",
    "e", "ei", "en", "eng", "er",
    "fa", "fan", "fang", "fei", "fen", "feng", "fo", "fou", "fu",
    "ga", "gai", "gan", "gang", "gao", "ge", "gei", "gen", "geng", "gong", "gou", "gu", "gua", "guai", "guan", "guang",
    "gui", "gun", "guo",
    "ha", "hai", "han", "hang", "hao", "he", "hei", "hen", "heng", "hong", "hou", "hu", "hua", "huai", "huan", "huang",
    "hui", "hun", "huo",
    "ji", "jia", "jian", "jiang", "jiao", "jie", "jin", "jing", "jiong", "jiu", "ju", "juan", "jue", "jun",
    "ka", "kai", "kan", "kang", "kao", "ke", "ken", "keng", "kong", "kou", "ku", "kua", "kuai", "kuan", "kuang",
    "kui", "kun", "kuo",
    "la", "lai", "lan", "lang", "lao", "le", "lei", "leng", "li", "lia", "lian", "liang", "liao", "lie", "lin", "ling",
    "liu", "long", "lou", "lu", "lv", "luan", "lve", "lun", "luo",
    "ma", "mai", "man", "mang", "mao", "me", "mei", "men", "meng", "mi", "mian", "miao", "mie", "min", "ming", "miu",
    "mo", "mou", "mu",
    "na", "nai", "nan", "nang", "nao", "ne", "nei", "nen", "neng", "ni", "nian", "niang", "niao", "nie", "nin", "ning",
    "niu", "nong", "nou", "nu", "nv", "nuan", "nve", "nuo",
    "o", "ou",
    "pa", "pai", "pan", "pang", "pao", "pei", "pen", "peng", "pi", "pian", "piao", "pie", "pin", "ping", "po", "pou", "pu",
    "qi", "qia", "qian", "qiang", "qiao", "qie", "qin", "qing", "qiong", "qiu", "qu", "quan", "que", "qun",
    "ran", "rang", "rao", "re", "ren", "reng", "ri", "rong", "rou", "ru", "rua", "ruan", "rui", "run", "ruo",
    "sa", "sai", "san", "sang", "sao", "se", "sen", "seng", "sha", "shai", "shan", "shang", "shao", "she", "shei", "shen",
    "sheng", "shi", "shou", "shu", "shua", "shuai", "shuan", "shuang", "shui", "shun", "shuo", "si", "song", "sou",
    "su", "suan", "sui", "sun", "suo",
    "ta", "tai", "tan", "tang", "tao", "te", "teng", "ti", "tian", "tiao", "tie", "ting", "tong", "tou", "tu", "tuan",
    "tui", "tun", "tuo",
    "wa", "wai", "wan", "wang", "wei", "wen", "weng", "wo", "wu",
    "xi", "xia", "xian", "xiang", "xiao", "xie", "xin", "xing", "xiong", "xiu", "xu", "xuan", "xue", "xun",
    "ya", "yan", "yang", "yao", "ye", "yi", "yin", "ying", "yong", "you", "yu", "yuan", "yue", "yun",
    "za", "zai", "zan", "zang", "zao", "ze", "zei", "zen", "zeng", "zha", "zhai", "zhan", "zhang", "zhao", "zhe", "zhei",
    "zhen", "zheng", "zhi", "zhong", "zhou", "zhu", "zhua", "zhuai", "zhuan", "zhuang", "zhui", "zhun", "zhuo",
    "zi", "zong", "zou", "zu", "zuan", "zui", "zun", "zuo"
  )

  // 初始化 Trie
  syllables.foreach { syl =>
    var node = root
    for (c <- syl) {
      val idx = c - 'a'
      if (node.children(idx) == null)
        node.children(idx) = new TrieNode()
      node = node.children(idx)
    }
    node.isEnd = true
  }

  /**
   * 直接在字符串上通过索引检查是否为合法拼音音节。
   *
   * @param s     全小写字符串
   * @param start 起始索引
   * @param len   长度
   * @return 是否合法音节
   */
  private def isSyllable(s: String, start: Int, len: Int): Boolean = {
    var node = root
    var i = 0
    while (i < len) {
      val idx = s.charAt(start + i) - 'a'
      if (idx < 0 || idx >= 26) return false
      node = node.children(idx)
      if (node == null) return false
      i += 1
    }
    node.isEnd
  }

  /**
   * 判断字符串能否完全切分为拼音。
   */
  def isPinyin(input: String): Boolean = {
    if (input == null || input.isEmpty) return false
    val s = input.toLowerCase
    val n = s.length
    val dp = Array.fill(n + 1)(false)
    dp(0) = true

    var i = 1
    while (i <= n) {
      var l = Math.min(MaxLen, i)
      while (l >= 1) {
        if (isSyllable(s, i - l, l) && dp(i - l)) {
          dp(i) = true
          l = 0 // 跳出内循环
        }
        l -= 1
      }
      // 不因 dp(i) 为 false 而提前返回，必须完整扫描
      i += 1
    }
    dp(n)
  }

  /**
   * 对输入字符串进行拼音切分。
   *
   * @param input 待切分的拼音字符串（自动转为小写）
   * @return 音节列表（如 ["ni", "hao"]）；若无法完全切分则返回 null
   */
  def split(input: String): Option[List[String]] = {
    if (input == null || input.isEmpty) return None

    val s = input.toLowerCase
    val n = s.length
    val dp = Array.fill(n + 1)(false)
    dp(0) = true
    val fromLen = new Array[Int](n + 1) // 记录以 i 结尾的最长音节长度

    var i = 1
    while (i <= n) {
      var l = Math.min(MaxLen, i)
      while (l >= 1 && !dp(i)) {
        if (isSyllable(s, i - l, l) && dp(i - l)) {
          dp(i) = true
          fromLen(i) = l
        }
        l -= 1
      }
      i += 1
    }

    if (!dp(n)) {
      None
    } else {
      val buf = ListBuffer.empty[String]
      var pos = n
      while (pos > 0) {
        val len = fromLen(pos)
        buf += s.substring(pos - len, pos)
        pos -= len
      }
      Some(buf.reverse.toList)
    }
  }

  /** 方便 Java 互操作：返回 java.util.List，无法切分时返回 null */
  def splitOrNull(input: String): java.util.List[String] =
    split(input).map(_.asJava).orNull

  def main(args: Array[String]): Unit = {
    val tests = Seq("nihao", "xian", "shanghai", "dangan", "women",
      "nvren", "lveduo", "zhuang", "beijing",
      "rai", "hello", "kevin", "englishword", "", "zhuaang")
    tests.foreach { t =>
      println(s"$t -> isPinyin=${isPinyin(t)}, split=${split(t).getOrElse(Seq("非拼音")).mkString(" ")}")
    }
  }
}
