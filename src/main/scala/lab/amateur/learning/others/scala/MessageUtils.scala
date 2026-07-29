package lab.amateur.learning.others.scala

import scala.collection.mutable

/**
 * SWIFT FIN 报文解析工具（MT103 / MT103_COV / MT202Cov_COV 等）
 *
 * 物理分段：第一段 37 字节，后续每段 35 字节，最后一段可能不足 35。
 * 自动识别两种逻辑格式：
 *   - 标记格式：后续段以 “数字/” 开头，相同数字的段合并。
 *   - 无标记格式：仅根据段尾是否有空格填充判断逻辑块边界。
 *
 * 输出清理规则：
 *   - 若第一个逻辑块以 '/' 开头，去掉该 '/'。
 *   - 所有逻辑块去除尾部空格。
 *   - 合并过程中因规则插入的普通空格保留。
 *
 * 线程安全，内存高效，适合 Spark UDF。
 */
object MessageUtils {

  def parseSwiftMessage(msg: String): List[String] = {
    if (msg.isEmpty) return Nil

    // 1. 计算物理段索引
    val segments = {
      val b = Vector.newBuilder[(Int, Int)]
      var pos = 0
      val firstLen = math.min(37, msg.length)
      b += ((pos, firstLen))
      pos += firstLen
      while (pos < msg.length) {
        val segLen = math.min(35, msg.length - pos)
        b += ((pos, segLen))
        pos += segLen
      }
      b.result()
    }

    // 2. 检测是否为标记格式
    val isTagged = segments.length > 1 && {
      var valid = true
      var lastNum = -1
      var i = 1
      while (i < segments.length && valid) {
        val (start, segLen) = segments(i)
        val end = start + segLen
        var slashIdx = -1
        var j = start
        while (j < end && j - start <= 2) {
          if (msg.charAt(j) == '/') { slashIdx = j; j = end }
          j += 1
        }
        if (slashIdx > start && slashIdx - start <= 2) {
          val numStr = msg.substring(start, slashIdx)
          if (numStr.forall(_.isDigit)) {
            val num = numStr.toInt
            if (num < lastNum) valid = false
            else lastNum = num
          } else valid = false
        } else valid = false
        i += 1
      }
      valid
    }

    // 3. 合并逻辑块
    val rawBlocks = if (isTagged) parseTagged(msg, segments) else parseUntagged(msg, segments)

    // 4. 后处理：header 去 '/'，所有块去尾部空格
    rawBlocks.headOption match {
      case Some(header) =>
        val cleanHeader = if (header.startsWith("/")) header.substring(1) else header
        cleanHeader.stripTrailing() :: rawBlocks.tail.map(_.stripTrailing())
      case None => Nil
    }
  }

  // ---------- 标记格式合并 ----------
  private case class TaggedSeg(tag: Int, dataStart: Int, dataLen: Int, hasPad: Boolean)

  private def parseTagged(msg: String, segs: IndexedSeq[(Int, Int)]): List[String] = {
    val header = msg.substring(segs.head._1, segs.head._1 + segs.head._2)
    val parsed = segs.tail.map { case (start, segLen) =>
      val end = start + segLen
      var slashIdx = start
      while (msg.charAt(slashIdx) != '/') slashIdx += 1
      val tagNum = msg.substring(start, slashIdx).toInt
      val dataStart = slashIdx + 1
      val dataLen = end - dataStart
      val hasPad = dataLen > 0 && msg.charAt(end - 1) == ' '
      TaggedSeg(tagNum, dataStart, dataLen, hasPad)
    }

    val groups = mutable.LinkedHashMap[Int, List[TaggedSeg]]()
    for (seg <- parsed) {
      groups(seg.tag) = seg :: groups.getOrElse(seg.tag, Nil)
    }

    val merged = groups.map { case (_, revSegs) =>
      val segsInOrder = revSegs.reverse
      val sb = new StringBuilder
      var prevHasPad = false
      for (s <- segsInOrder) {
        val chunk = msg.substring(s.dataStart, s.dataStart + s.dataLen)
        if (sb.isEmpty) {
          sb.append(chunk)
        } else {
          if (prevHasPad) {
            while (sb.nonEmpty && sb.last == ' ') sb.deleteCharAt(sb.length - 1)
            sb.append(' ')
          }
          sb.append(chunk)
        }
        prevHasPad = s.hasPad
      }
      sb.toString
    }.toList

    header :: merged
  }

  // ---------- 无标记格式合并（已全部改用 substring 显式提取）----------
  private def parseUntagged(msg: String, segs: IndexedSeq[(Int, Int)]): List[String] = {
    val header = msg.substring(segs.head._1, segs.head._1 + segs.head._2)
    if (segs.length == 1) return List(header)

    val result = List.newBuilder[String]
    val curSb = new StringBuilder
    var prevHasPad = false
    var isFirstTail = true

    var i = 1
    while (i < segs.length) {
      val (start, segLen) = segs(i)
      val end = start + segLen
      val hasPad = segLen > 0 && msg.charAt(end - 1) == ' '

      val chunk = msg.substring(start, end)   // 显式提取当前物理段

      if (isFirstTail) {
        curSb.append(chunk)
        isFirstTail = false
      } else {
        if (!prevHasPad) {
          curSb.append(chunk)
        } else {
          result += curSb.toString
          curSb.clear()
          curSb.append(chunk)
        }
      }
      prevHasPad = hasPad
      i += 1
    }
    if (!isFirstTail) result += curSb.toString

    header :: result.result()
  }
}
