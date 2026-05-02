package lab.amateur.learning.others.java;

import java.util.*;

public class PinyinSplitter {

    /**
     * 合法拼音音节集合（含 v 代替 ü）
     */
    private static final Set<String> PINYIN_SET;

    /**
     * 最大音节长度
     */
    private static final int MAX_LENGTH = 6;

    static {
        // 使用不可修改的集合确保线程安全且内容固定
        Set<String> set = new HashSet<>(410);
        // 所有合法音节
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
        Collections.addAll(set, syllables);
        PINYIN_SET = Collections.unmodifiableSet(set);
    }

    /**
     * 尝试将输入字符串按拼音最长匹配切分。
     *
     * @param input 全小写字母字符串
     * @return 若能完全切分，返回拼音音节列表；否则返回 null
     */
    public static List<String> split(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        String s = input.toLowerCase();
        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true;
        String[] from = new String[n + 1]; // from[i] 记录结尾在 i 的最长合法音节

        for (int i = 1; i <= n; i++) {
            // 从长到短尝试，实现最大匹配
            for (int l = Math.min(MAX_LENGTH, i); l >= 1; l--) {
                String sub = s.substring(i - l, i);
                if (PINYIN_SET.contains(sub) && dp[i - l]) {
                    dp[i] = true;
                    from[i] = sub;
                    break; // 找到最长即停止
                }
            }
        }

        if (!dp[n]) {
            return null; // 无法完全切分为拼音
        }

        // 回溯
        LinkedList<String> result = new LinkedList<>();
        int pos = n;
        while (pos > 0) {
            String syllable = from[pos];
            result.addFirst(syllable);
            pos -= syllable.length();
        }
        return result;
    }

    // ------------------- 测试示例 -------------------
    public static void main(String[] args) {
        String[] tests = {
                "nihao",       // ["ni", "hao"]
                "xian",        // ["xian"]
                "shanghai",    // ["shang", "hai"]
                "women",       // ["wo", "men"] (也会判为拼音)
                "hello",       // null (非拼音)
                "dangan",      // ["dang", "an"] (档案)
                "beijing",     // ["bei", "jing"]
                "zhuang",      // ["zhuang"]
                "juede",       // ["jue", "de"]
                "nvren",       // ["nv", "ren"] (女人，v 代 ü)
                "lveduo",      // ["lve", "duo"] (掠夺)
                "yuanwang",    // ["yuan", "wang"]
                "gongzuo",     // ["gong", "zuo"]
                "xianzai",     // ["xian", "zai"]
                "englishword", // null (非拼音)
                "rai"
        };

        for (String test : tests) {
            List<String> result = split(test);
            System.out.println(test + " -> " + (result == null ? "非拼音" : String.join(" ", result)));
        }
    }
}
