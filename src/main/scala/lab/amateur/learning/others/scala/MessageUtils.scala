package lab.amateur.learning.others.scala

/**
 * SWIFT FIN 报文解析器（MT103 / MT103_COV / MT202Cov_COV 等）
 *
 * 专为超大规模数据（如 20 亿条）设计，完全基于基础类型数组，零堆分配。
 * 若数据量小，可替换为更易读的集合版本，输出语义完全相同。
 */
object MessageUtils {

  /**
   * 返回清理后的逻辑块列表。
   */
  def parseSwiftMessage(msg: String): List[String] = {
    if (msg.isEmpty) Nil
    else {
      val result = new java.util.ArrayList[String](8)
      parseInto(msg, result)
      var list = List.empty[String]
      var i = result.size() - 1
      while (i >= 0) {
        list = result.get(i) :: list; i -= 1
      }
      list
    }
  }

  /**
   * 返回 Map，键为 "Header" 和 "Block"+标记数字（或顺序编号）。
   */
  def parseSwiftMessageToMap(msg: String): Map[String, String] = {
    if (msg.isEmpty) return Map.empty
    val result = new java.util.ArrayList[String](8)
    val tags = new java.util.ArrayList[Int](8)
    parseInto(msg, result, tags)
    val b = Map.newBuilder[String, String]
    b += ("Header" -> result.get(0))
    var i = 1
    while (i < result.size()) {
      b += (s"Block${tags.get(i - 1)}" -> result.get(i))
      i += 1
    }
    b.result()
  }

  // ==================== 核心解析（完全基于数组） ====================
  private def parseInto(msg: String, result: java.util.ArrayList[String], tags: java.util.ArrayList[Int] = null): Unit = {
    val len = msg.length
    if (len == 0) return

    // 预分配最大段数（每段 35 字节）
    val maxSegs = 1 + (if (len > 37) ((len - 37 + 34) / 35) else 0) + 1
    // 物理段信息（无标记格式使用）
    val physStart = new Array[Int](maxSegs)
    val physLen = new Array[Int](maxSegs)
    val hasPad = new Array[Boolean](maxSegs)
    // 标记段信息（仅标记格式时有效）
    val tagNum = new Array[Int](maxSegs)
    val dataStart = new Array[Int](maxSegs)
    val dataLen = new Array[Int](maxSegs)

    var segCount = 1

    // Header（第一段 37 字节）
    val headerLen = math.min(37, len)
    physStart(0) = 0
    physLen(0) = headerLen
    hasPad(0) = headerLen > 0 && msg.charAt(headerLen - 1) == ' '

    var pos = headerLen
    var isTagged = true
    var lastTag = -1
    var firstTag = true

    // 单次扫描：记录段信息 + 检测格式
    while (pos < len) {
      val segLen = math.min(35, len - pos)
      val segEnd = pos + segLen
      val pad = segLen > 0 && msg.charAt(segEnd - 1) == ' '

      // 始终记录物理段信息（回退时直接使用）
      physStart(segCount) = pos
      physLen(segCount) = segLen
      hasPad(segCount) = pad

      if (isTagged) {
        // 检查是否为 "数字/" 格式（仅支持一位数字以极致优化）
        if (segLen >= 2 && Character.isDigit(msg.charAt(pos)) && msg.charAt(pos + 1) == '/') {
          val tag = msg.charAt(pos) - '0'
          if (firstTag || tag >= lastTag) {
            lastTag = tag
            firstTag = false
            tagNum(segCount) = tag
            dataStart(segCount) = pos + 2
            dataLen(segCount) = segLen - 2
          } else {
            isTagged = false // 标记递减 → 整个消息按无标记处理
          }
        } else {
          isTagged = false // 出现非标记段 → 整个消息按无标记处理
        }
      }
      segCount += 1
      pos += segLen
    }

    // 合并 Header（清理前导 '/' 和尾部空格）
    val headerStr = cleanHeader(msg, physStart(0), physLen(0))
    result.add(headerStr)
    if (segCount == 1) return

    // 根据最终格式选择合并策略
    val sb = new java.lang.StringBuilder(128)
    if (isTagged) {
      mergeTagged(msg, tagNum, dataStart, dataLen, hasPad, segCount, result, sb, tags)
    } else {
      mergeUntagged(msg, physStart, physLen, hasPad, segCount, result, sb, tags)
    }
  }

  private def mergeTagged(msg: String, tagNum: Array[Int], dataStart: Array[Int],
                          dataLen: Array[Int], hasPad: Array[Boolean], segCount: Int,
                          result: java.util.ArrayList[String], sb: java.lang.StringBuilder,
                          tags: java.util.ArrayList[Int]): Unit = {
    var i = 1
    var curTag = tagNum(1)
    sb.setLength(0)
    sb.append(msg, dataStart(1), dataStart(1) + dataLen(1))
    var prevPad = hasPad(1)

    i = 2
    while (i < segCount) {
      if (tagNum(i) == curTag) {
        if (prevPad) {
          trimTrailingSpaces(sb); sb.append(' ')
        }
        sb.append(msg, dataStart(i), dataStart(i) + dataLen(i))
        prevPad = hasPad(i)
      } else {
        trimTrailingSpaces(sb)
        result.add(sb.toString)
        if (tags != null) tags.add(curTag)
        curTag = tagNum(i)
        sb.setLength(0)
        sb.append(msg, dataStart(i), dataStart(i) + dataLen(i))
        prevPad = hasPad(i)
      }
      i += 1
    }
    trimTrailingSpaces(sb)
    result.add(sb.toString)
    if (tags != null) tags.add(curTag)
  }

  private def mergeUntagged(msg: String, physStart: Array[Int], physLen: Array[Int],
                            hasPad: Array[Boolean], segCount: Int,
                            result: java.util.ArrayList[String], sb: java.lang.StringBuilder,
                            tags: java.util.ArrayList[Int]): Unit = {
    sb.setLength(0)
    var isFirst = true
    var prevPad = false
    var blockId = 1

    var i = 1
    while (i < segCount) {
      val start = physStart(i)
      val len = physLen(i)
      val pad = hasPad(i)

      if (isFirst) {
        sb.append(msg, start, start + len)
        isFirst = false
      } else {
        if (!prevPad) {
          sb.append(msg, start, start + len)
        } else {
          trimTrailingSpaces(sb)
          result.add(sb.toString)
          if (tags != null) tags.add(blockId)
          blockId += 1
          sb.setLength(0)
          sb.append(msg, start, start + len)
        }
      }
      prevPad = pad
      i += 1
    }
    if (!isFirst) {
      trimTrailingSpaces(sb)
      result.add(sb.toString)
      if (tags != null) tags.add(blockId)
    }
  }

  private def cleanHeader(msg: String, start: Int, len: Int): String = {
    var s = start
    var e = start + len
    if (s < e && msg.charAt(s) == '/') s += 1
    while (e > s && msg.charAt(e - 1) == ' ') e -= 1
    if (s == start && e == start + len) msg.substring(start, start + len)
    else if (s == e) ""
    else msg.substring(s, e)
  }

  private def trimTrailingSpaces(sb: java.lang.StringBuilder): Unit = {
    var len = sb.length()
    while (len > 0 && sb.charAt(len - 1) == ' ') len -= 1
    sb.setLength(len)
  }
}
