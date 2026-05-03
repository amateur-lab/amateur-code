package lab.amateur.learning.others.java;

import java.util.*;

/**
 * 拼音工具类 —— 基于 Trie 的高效拼音判定与切分。
 * <p>
 * 特性：
 * <ul>
 *   <li>内置全部合法汉语拼音音节（无声调，v 代 ü），以 Trie 形式存储，查询 O(len)。</li>
 *   <li>所有方法均为静态、线程安全、无状态，适合直接在 Spark 等分布式环境中调用。</li>
 *   <li>核心匹配通过 {@code String.charAt} + Trie 完成，不产生临时子字符串，GC 友好。</li>
 *   <li>算法复杂度 O(n)。</li>
 * </ul>
 * </p>
 *
 * <p>
 * 提供两个主要功能：
 * <ul>
 *   <li>{@link #isPinyin(String)} : 判断字符串能否完全切分为合法拼音音节（仅返回 {@code boolean}）。</li>
 *   <li>{@link #split(String)}   : 按最长匹配原则切分拼音，返回音节列表；若无法切分则返回 {@code null}。</li>
 * </ul>
 * </p>
 */
public class PinyinUtils {

    // ==================== Trie 节点 ====================
    private static class TrieNode {
        final TrieNode[] children = new TrieNode[26];
        boolean isEnd; // 该节点是否为一个完整音节的结尾
    }

    private static final TrieNode ROOT = new TrieNode();

    /**
     * 拼音最大长度（zhua, chuang, shuang 等）
     */
    private static final int MAX_LEN = 6;

    /* 静态加载所有合法拼音音节并构建 Trie */
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
            for (int i = 0; i < syl.length(); i++) {
                int idx = syl.charAt(i) - 'a';
                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }
                node = node.children[idx];
            }
            node.isEnd = true;
        }
    }

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
     * 判断字符串能否完全切分为合法拼音音节。
     *
     * <p>算法：正向动态规划，dp[i] 表示前缀 s[0..i) 是否可切分。
     * 对于每个 i，尝试所有可能的音节长度 l（从长到短），
     * 只要找到一个 isSyllable(s, i-l, l) 为真且 dp[i-l] 为真，则 dp[i] 为真。
     * 最终返回 dp[n]。</p>
     *
     * @param input 待判定字符串（内部转为小写）
     * @return true 若能完全切分
     */
    public static boolean isPinyin(String input) {
        if (input == null || input.isEmpty()) return false;

        String s = input.toLowerCase();  // 保证小写（若外部已保证，可移除此行提升性能）
        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true;

        for (int i = 1; i <= n; i++) {
            for (int l = Math.min(MAX_LEN, i); l >= 1; l--) {
                if (isSyllable(s, i - l, l) && dp[i - l]) {
                    dp[i] = true;
                    break;
                }
            }
        }
        return dp[n];
    }

    /**
     * 对输入字符串进行拼音切分。
     *
     * <p>算法与 {@link #isPinyin(String)} 基本相同，但额外记录了每个位置所选择的音节长度。
     * 同样采用正向 DP + 从长到短的尝试策略，找到第一个合法音节即记录并跳出内循环，
     * 从而实现最大匹配（长音节优先）。</p>
     *
     * <p>注意：这种策略对于某些模糊边界（如 "dangan"）会切分为 "dan" "gan"，
     * 而非更符合习惯的 "dang" "an"。若需后者，请使用更为复杂的回溯策略。
     * 但在纯粹“拼音判定与切分”的场景下，本方法已足够正确且高效。</p>
     *
     * @param input 待切分的拼音字符串（自动转为小写）
     * @return 音节列表（如 ["ni", "hao"]）；若无法完全切分则返回 null
     */
    public static List<String> split(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String s = input.toLowerCase();
        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true;
        int[] fromLen = new int[n + 1]; // fromLen[i] 记录以 i 结尾的最长音节的长度

        for (int i = 1; i <= n; i++) {
            for (int l = Math.min(MAX_LEN, i); l >= 1; l--) {
                if (isSyllable(s, i - l, l) && dp[i - l]) {
                    dp[i] = true;
                    fromLen[i] = l;
                    break; // 最长匹配
                }
            }
        }

        if (!dp[n]) {
            return null;
        }

        // 回溯：从末尾开始，根据 fromLen 提取音节
        LinkedList<String> result = new LinkedList<>();
        int pos = n;
        while (pos > 0) {
            int len = fromLen[pos];
            result.addFirst(s.substring(pos - len, pos));
            pos -= len;
        }
        return result;
    }

    // ==================== 简单测试 ====================
    public static void main(String[] args) {
        String[] tests = {
                "nihao", "xian", "shanghai", "dangan", "women",
                "nvren", "lveduo", "zhuang", "zhuaang", "beijing",
                "rai", "hello", "kevin", "englishword", ""
        };
        for (String t : tests) {
            System.out.println(t + " -> isPinyin=" + isPinyin(t) +
                    ", split=" + (split(t) == null ? "非拼音" : String.join(" ", split(t))));
        }
    }
}
