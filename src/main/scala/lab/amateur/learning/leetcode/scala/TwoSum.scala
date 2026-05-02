package lab.amateur.learning.leetcode.scala

object TwoSum {
  def main(args: Array[String]): Unit = {
    val nums = Array(2, 7, 11, 15)
    val target = 9
    println(twoSum(nums = nums, target = target).mkString("Array(", ", ", ")"))
  }

  def twoSum(nums: Array[Int], target: Int): Array[Int] = {
    val d = scala.collection.mutable.Map[Int, Int]()
    var ans: Array[Int] = Array()
    for (i <- nums.indices if ans.isEmpty) {
      val x = nums(i)
      val y = target - x
      if (d.contains(y)) {
        ans = Array(d(y), i)
      } else {
        d(x) = i
      }
    }
    ans
  }
}
