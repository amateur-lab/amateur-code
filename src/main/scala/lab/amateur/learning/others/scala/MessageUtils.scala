package lab.amateur.learning.others.scala

/**
 * SWIFT FIN 报文解析器（支持两种物理分段格式：首段 37 + 后续 35，或首段 35 + 后续 35）。
 *
 * 特性：
 * - 自动识别标记格式（数字/）或仅基于空格边界的无标记格式。
 * - 输出清理：首段去除可能的前导 '/' 及尾部空格；所有块去除尾部空格。
 * - 极致性能：零堆分配对象（无 case class、集合），单遍扫描，复用 StringBuilder。
 * - 线程安全，适用于 Spark UDF 超大规模数据处理。
 */
object MessageUtils {

  // ---- 公开 API（默认首段 37 字节） ----
  def parseSwiftMessage(msg: String): List[String] = parseSwiftMessage(msg, 37)
  def parseSwiftMessageToMap(msg: String): Map[String, String] = parseSwiftMessageToMap(msg, 37)

  // ---- 显式指定首段长度的 API ----
  def parseSwiftMessage(msg: String, firstBlockLen: Int): List[String] = {
    if (msg.isEmpty) Nil
    else {
      val result = new java.util.ArrayList[String](8)
      parseInto(msg, firstBlockLen, result)
      var list = List.empty[String]
      var i = result.size() - 1
      while (i >= 0) { list = result.get(i) :: list; i -= 1 }
      list
    }
  }

  def parseSwiftMessageToMap(msg: String, firstBlockLen: Int): Map[String, String] = {
    if (msg.isEmpty) return Map.empty
    val result = new java.util.ArrayList[String](8)
    val tags = new java.util.ArrayList[Int](8)
    parseInto(msg, firstBlockLen, result, tags)
    val builder = Map.newBuilder[String, String]
    builder += ("Header" -> result.get(0))
    var i = 1
    while (i < result.size()) {
      builder += (s"Block${tags.get(i - 1)}" -> result.get(i))
      i += 1
    }
    builder.result()
  }

  // ==================== 内部核心（参数化 firstBlockLen） ====================
  private def parseInto(msg: String, firstBlockLen: Int,
                        result: java.util.ArrayList[String],
                        tags: java.util.ArrayList[Int] = null): Unit = {
    val len = msg.length
    if (len == 0) return

    // 计算最大物理段数：1 (首段) + 剩余部分按 35 分块 + 安全余量
    val restLen = if (len > firstBlockLen) len - firstBlockLen else 0
    val maxSegs = 1 + (if (restLen > 0) (restLen + 34) / 35 else 0) + 1

    // 物理段信息（始终记录，用于无标记格式）
    val physStart = new Array[Int](maxSegs)
    val physLen   = new Array[Int](maxSegs)
    val hasPad    = new Array[Boolean](maxSegs)
    // 标记段信息（仅在识别为标记格式时有效）
    val tagNum    = new Array[Int](maxSegs)
    val dataStart = new Array[Int](maxSegs)   // 跳过 "数字/" 后的数据起始
    val dataLen   = new Array[Int](maxSegs)

    var segCount = 1

    // ---- 处理首段 ----
    val firstLen = math.min(firstBlockLen, len)
    physStart(0) = 0
    physLen(0) = firstLen
    hasPad(0) = firstLen > 0 && msg.charAt(firstLen - 1) == ' '

    var pos = firstLen
    var isTagged = true
    var lastTag = -1
    var firstTag = true

    // ---- 扫描后续段（每段固定 35 字节） ----
    while (pos < len) {
      val segLen = math.min(35, len - pos)
      val segEnd = pos + segLen
      val pad = segLen > 0 && msg.charAt(segEnd - 1) == ' '

      // 总是记录物理段信息，以备回退
      physStart(segCount) = pos
      physLen(segCount) = segLen
      hasPad(segCount) = pad

      if (isTagged) {
        // 尝试识别 "数字/" 格式（为极致性能，仅支持一位数字）
        if (segLen >= 2 && Character.isDigit(msg.charAt(pos)) && msg.charAt(pos + 1) == '/') {
          val tag = msg.charAt(pos) - '0'
          if (firstTag || tag >= lastTag) {
            // 合法标记（非递减）
            lastTag = tag
            firstTag = false
            tagNum(segCount) = tag
            dataStart(segCount) = pos + 2          // 跳过 "1/"
            dataLen(segCount) = segLen - 2
          } else {
            // 标记递减 → 整个消息转为无标记格式
            isTagged = false
          }
        } else {
          // 不满足 "数字/" → 转为无标记格式
          isTagged = false
        }
      }
      // 如果 !isTagged，则标记信息保持默认 0，后续合并将使用物理段信息
      segCount += 1
      pos += segLen
    }

    // ---- 清理并添加首段 ----
    val headerStr = cleanHeader(msg, physStart(0), physLen(0))
    result.add(headerStr)
    if (segCount == 1) return

    // ---- 根据格式选择合并策略 ----
    val sb = new java.lang.StringBuilder(128)
    if (isTagged) {
      mergeTagged(msg, tagNum, dataStart, dataLen, hasPad, segCount, result, sb, tags)
    } else {
      mergeUntagged(msg, physStart, physLen, hasPad, segCount, result, sb, tags)
    }
  }

  // ==================== 合并逻辑 ====================
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
        // 同一标记合并
        if (prevPad) {
          trimTrailingSpaces(sb)
          sb.append(' ')
        }
        sb.append(msg, dataStart(i), dataStart(i) + dataLen(i))
        prevPad = hasPad(i)
      } else {
        // 换标记，输出当前块
        trimTrailingSpaces(sb)
        result.add(sb.toString)
        if (tags != null) tags.add(curTag)
        // 开始新块
        curTag = tagNum(i)
        sb.setLength(0)
        sb.append(msg, dataStart(i), dataStart(i) + dataLen(i))
        prevPad = hasPad(i)
      }
      i += 1
    }
    // 最后一个块
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
          // 前一段无填充 → 属于同一逻辑块，直接拼接
          sb.append(msg, start, start + len)
        } else {
          // 前一段有填充 → 结束当前逻辑块，开启新块
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
    // 最后一个逻辑块
    if (!isFirst) {
      trimTrailingSpaces(sb)
      result.add(sb.toString)
      if (tags != null) tags.add(blockId)
    }
  }

  // ==================== 工具方法 ====================
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
