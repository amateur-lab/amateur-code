package lab.amateur.learning.others.java;

import java.util.*;

/**
 * 拼音切分工具 —— 基于 Trie 的最长匹配 + 动态规划。
 * <p>
 * 特性：
 * 1. 使用静态 Trie 存储全部合法汉语拼音音节（无声调，v 代 ü），初始化后只读，线程安全。
 * 2. 采用正向动态规划，每个位置从长到短尝试（最长 6），实现“长音节优先”策略。
 * 3. 核心匹配方法 {@link #isSyllable(String, int, int)} 直接在输入字符串的底层字符数组上
 * 通过 Trie 查找，完全避免创建临时子字符串，GC 压力极低。
 * 4. 算法时间复杂度 O(n)，n 为字符串长度（≤ 30）；空间复杂度 O(n)。
 * 5. 无法完全切分为合法拼音时返回 null，否则返回音节列表。
 * </p>
 */
public class PinyinSplitter {

    // ==================== Trie 定义与初始化 ====================

    /**
     * Trie 节点
     */
    private static class TrieNode {
        final TrieNode[] children = new TrieNode[26];
        boolean isEnd; // 标记是否为一个完整音节的结尾
    }

    /**
     * 根节点
     */
    private static final TrieNode ROOT = new TrieNode();

    /**
     * 拼音最大长度（zhua, chuang, shuang 等）
     */
    private static final int MAX_LEN = 6;

    /* 静态代码块：加载所有合法拼音音节并构建 Trie */
    static {
        String[] syllables = {
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
        };
        for (String syl : syllables) {
            TrieNode node = ROOT;
            for (int j = 0; j < syl.length(); j++) {
                int idx = syl.charAt(j) - 'a';
                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }
                node = node.children[idx];
            }
            node.isEnd = true;
        }
    }

    // ==================== 核心方法 ====================

    /**
     * 直接在字符串上通过索引判断子串是否为合法拼音音节。
     * <p>此方法不分配任何临时对象，仅通过 {@link String#charAt} 读取字符并沿 Trie 下降。</p>
     *
     * @param s     已规范化为全小写的输入字符串
     * @param start 子串起始索引（包含）
     * @param len   子串长度
     * @return 若子串是合法音节返回 true，否则 false
     */
    private static boolean isSyllable(String s, int start, int len) {
        TrieNode node = ROOT;
        for (int i = 0; i < len; i++) {
            int idx = s.charAt(start + i) - 'a';
            // 非小写字母直接判为非法（本算法假设输入已为小写，此处防御性检查）
            if (idx < 0 || idx >= 26) {
                return false;
            }
            node = node.children[idx];
            if (node == null) {
                return false;
            }
        }
        return node.isEnd;
    }

    /**
     * 对输入字符串进行最长匹配拼音切分。
     *
     * <p>算法：动态规划。定义 dp[i] 为前缀 s[0..i-1] 是否能被完全切分。
     * 从 i = 1 到 n 扫描，对于每个位置 i，从最长音节长度（6）向下尝试：
     * 若 s.substring(i-l, i) 是合法音节且 dp[i-l] 为真，则 dp[i] 为真，
     * 并记录该位置对应的音节长度 l。由于按长度降序尝试，天然实现最大匹配。</p>
     *
     * @param input 待切分的拼音字符串（建议全小写，内部会转为小写）
     * @return 若能完全切分，返回音节列表（如 ["ni", "hao"]）；否则返回 null
     */
    public static List<String> split(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList(); // 空字符串视为空列表，而非 null
        }

        // 1. 转为小写并初始化
        String s = input.toLowerCase();
        int n = s.length();

        boolean[] dp = new boolean[n + 1];
        dp[0] = true; // 空前缀可切分

        // fromLen[i] 记录以位置 i 结尾的最长音节的长度（仅在 dp[i]==true 时有效）
        int[] fromLen = new int[n + 1];

        // 2. 动态规划
        for (int i = 1; i <= n; i++) {
            // 从最长音节开始尝试，实现最大匹配
            for (int l = Math.min(MAX_LEN, i); l >= 1; l--) {
                if (isSyllable(s, i - l, l) && dp[i - l]) {
                    dp[i] = true;
                    fromLen[i] = l;
                    break; // 已找到最长合法音节，跳出内循环
                }
            }
        }

        // 3. 无法完全切分
        if (!dp[n]) {
            return null;
        }

        // 4. 回溯构建音节列表
        LinkedList<String> result = new LinkedList<>();
        int pos = n;
        while (pos > 0) {
            int len = fromLen[pos];
            // 仅在这里创建子字符串，数量 = 音节个数，通常 1~5 个，开销极小
            result.addFirst(s.substring(pos - len, pos));
            pos -= len;
        }
        return result;
    }

    // ==================== 测试 ====================
    public static void main(String[] args) {
        String[] tests = {
                "nihao",
                "xian",
                "shanghai",
                "rai",       // 非法
                "hello",     // 非法（英文）
                "dangan",
                "women",
                "nvren",
                "lveduo",
                "zhuang",
                "beijing",
                "englishword",
                "kevin"      // 非法（英文名）
        };
        for (String t : tests) {
            List<String> res = split(t);
            System.out.println(t + " -> " + (res == null ? "非拼音" : String.join(" ", res)));
        }
    }
}
