package lab.amateur.learning.others.java;

import java.util.*;

public class PinyinSplitter {

    // 所有合法拼音音节（无调，用 v 代替 ü）
    private static final Set<String> PINYIN_SET = new HashSet<>();
    // 按长度分组的拼音音节，便于优化查询（本实现直接使用全局集合）
    private static final int MAX_LENGTH = 6;

    static {
        initPinyinSet();
    }

    /**
     * 初始化合法拼音音节集合（含整体认读音节，约 410+ 个）
     */
    private static void initPinyinSet() {
        // 声母（包括零声母用 y/w 开头的，但用组合生成）
        String[] initials = {
                "", "b", "p", "m", "f", "d", "t", "n", "l",
                "g", "k", "h", "j", "q", "x",
                "zh", "ch", "sh", "r", "z", "c", "s"
        };
        // 基本韵母（包含 v 表示 ü）
        String[] finalsBasic = {
                "a", "o", "e", "i", "u", "v",
                "ai", "ei", "ui", "ao", "ou", "iu",
                "ie", "ve", "er",
                "an", "en", "in", "un", "vn",
                "ang", "eng", "ing", "ong"
        };
        // 组合韵母（需要整体拼合）
        String[] finalsCombined = {
                "ia", "iao", "ian", "iang", "iong",
                "ua", "uo", "uai", "uan", "uang", "ueng",
                "van"
        };

        // 生成所有基本声韵组合
        for (String ini : initials) {
            for (String fin : finalsBasic) {
                addValidCombo(ini, fin);
            }
            for (String fin : finalsCombined) {
                addValidCombo(ini, fin);
            }
        }

        // 特殊处理整体认读音节和拼音规则修正
        addSpecificPinyin();
    }

    /**
     * 根据拼音规则添加一个声韵组合（自动处理拼写变化）
     */
    private static void addValidCombo(String ini, String fin) {
        // 韵母中的 v 代表 ü
        // 规则：
        // 1. j,q,x 后面的 ü 写成 u，比如 ju, qu, xu, jue, que, xue, juan, quan, xuan, jun, qun, xun
        //    这里 fin 中有 v 或 ve, van, vn 需要转换
        // 2. ü 单独或前面不是 jqx 时写成 v（我们词典用 v），但零声母时写作 yu(ü), yue(üe), yuan(üan), yun(ün)
        // 3. 零声母时：i -> yi (i 本身除外？i 单独是 yi；in -> yin; ing -> ying; iu -> you; ie -> ye; ian -> yan; iang -> yang; iong -> yong)
        //     u -> wu; ua -> wa; uo -> wo; uai -> wai; ui -> wei; uan -> wan; un -> wen; uang -> wang; ueng -> weng
        //     ü -> yu; üe -> yue; üan -> yuan; ün -> yun
        // 我们先生成拼音字母形式，存入集合（用 v 代替 ü）

        // 先处理特殊情况：零声母时，韵母书写变化
        if (ini.isEmpty()) {
            // 零声母拼写规则
            switch (fin) {
                case "i" -> {
                    PINYIN_SET.add("yi");
                    return;
                }
                case "ia" -> {
                    PINYIN_SET.add("ya");
                    return;
                }
                case "ie" -> {
                    PINYIN_SET.add("ye");
                    return;
                }
                case "iao" -> {
                    PINYIN_SET.add("yao");
                    return;
                }
                case "iu" -> {
                    PINYIN_SET.add("you");
                    return;
                }
                case "ian" -> {
                    PINYIN_SET.add("yan");
                    return;
                }
                case "in" -> {
                    PINYIN_SET.add("yin");
                    return;
                }
                case "iang" -> {
                    PINYIN_SET.add("yang");
                    return;
                }
                case "ing" -> {
                    PINYIN_SET.add("ying");
                    return;
                }
                case "iong" -> {
                    PINYIN_SET.add("yong");
                    return;
                }
                case "u" -> {
                    PINYIN_SET.add("wu");
                    return;
                }
                case "ua" -> {
                    PINYIN_SET.add("wa");
                    return;
                }
                case "uo" -> {
                    PINYIN_SET.add("wo");
                    return;
                }
                case "uai" -> {
                    PINYIN_SET.add("wai");
                    return;
                }
                case "ui" -> {
                    PINYIN_SET.add("wei");
                    return;
                }
                case "uan" -> {
                    PINYIN_SET.add("wan");
                    return;
                }
                case "un" -> {
                    PINYIN_SET.add("wen");
                    return;
                }
                case "uang" -> {
                    PINYIN_SET.add("wang");
                    return;
                }
                case "ueng" -> {
                    PINYIN_SET.add("weng");
                    return;
                }

                // ü 系列
                case "v" -> {
                    PINYIN_SET.add("yu");
                    return;
                }
                case "ve" -> {
                    PINYIN_SET.add("yue");
                    return;
                }
                case "van" -> {
                    PINYIN_SET.add("yuan");
                    return;
                }
                case "vn" -> {
                    PINYIN_SET.add("yun");
                    return;
                }
            }
            // 其余：a,o,e,ai,ei,ao,ou,an,en,ang,eng,er 等直接使用韵母本身
            PINYIN_SET.add(fin);
            return;
        }

        // 非零声母
        // 特殊规则：b,p,m,f 与 o 拼；不能与 uo 拼（buo -> bo 等等）
        if (fin.equals("uo") && "bpmf".contains(ini)) {
            // 实际拼写为 bo, po, mo, fo
            PINYIN_SET.add(ini + "o");
            return;
        }
        // ü 系列：j,q,x 后写为 u
        if ("jqx".contains(ini)) {
            switch (fin) {
                case "v" -> {
                    PINYIN_SET.add(ini + "u");
                    return;
                }
                case "ve" -> {
                    PINYIN_SET.add(ini + "ue");
                    return;
                }
                case "van" -> {
                    PINYIN_SET.add(ini + "uan");
                    return;
                }
                case "vn" -> {
                    PINYIN_SET.add(ini + "un");
                    return;
                }
            }
        } else {
            // 非 jqx，ü 保留 v
            switch (fin) {
                case "v" -> {
                    PINYIN_SET.add(ini + "v");
                    return;
                }
                case "ve" -> {
                    PINYIN_SET.add(ini + "ve");
                    return;
                }
                case "van" -> {
                    PINYIN_SET.add(ini + "van");
                    return;
                }
                case "vn" -> {
                    PINYIN_SET.add(ini + "vn");
                    return;
                }
            }
        }
        // 常规拼写
        // 注意：zh,ch,sh,z,c,s,r + i 产生整体认读音节 zhi chi shi ri zi ci si，i 是舌尖元音
        // 但已经是合法音节，直接添加
        PINYIN_SET.add(ini + fin);
    }

    /**
     * 补充手动修正及确保整体认读等无误
     */
    private static void addSpecificPinyin() {
        // 一些生成规则无法覆盖的合法音节或方言/特殊
        String[] extras = {
                // 确保整体认读无一遗漏
                "zhi", "chi", "shi", "ri", "zi", "ci", "si",
                // 零声母er
                "er",
                // 补充：某些拼法如 hm, hng, m, n, ng 等语气词通常不计入标准拼音，这里略去
                // 如果需要可加
        };
        Collections.addAll(PINYIN_SET, extras);

        // 剔除不合法组合：如 b, p, m, f + e 或 ei? 实际上 be, pe, me, fe 合法（么 me）
        // 已经存在。不再处理。
    }

    /**
     * 判断输入是否完全是拼音，若是则按最大匹配切分并返回音节列表；否则返回 null
     *
     * @param input 全小写字母字符串（可在调用前处理大小写）
     * @return 拼音音节列表，若无法完全切分则返回 null
     */
    public static List<String> split(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        String s = input.toLowerCase(); // 确保小写
        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true;
        String[] from = new String[n + 1]; // 回溯用音节

        for (int i = 1; i <= n; i++) {
            // 尝试最长音节优先，实现最大匹配
            for (int l = Math.min(MAX_LENGTH, i); l >= 1; l--) {
                String sub = s.substring(i - l, i);
                if (PINYIN_SET.contains(sub) && dp[i - l]) {
                    dp[i] = true;
                    from[i] = sub; // 记录最长的那个音节
                    break; // 只取最长匹配
                }
            }
        }

        if (!dp[n]) {
            return null; // 无法完全切分
        }

        // 回溯构建结果
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
        };

        for (String test : tests) {
            List<String> result = split(test);
            System.out.println(test + " -> " + (result == null ? "非拼音" : String.join(" ", result)));
        }
    }
}
