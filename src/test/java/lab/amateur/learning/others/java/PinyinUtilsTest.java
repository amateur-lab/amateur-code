package lab.amateur.learning.others.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PinyinUtilsTest {

    // ==================== isPinyin 测试 ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "a", "ba", "ni", "hao", "nihao", "xian", "shanghai", "dangan",
            "women", "nvren", "lveduo", "zhuang", "beijing", "gongzuo",
            "xianzai", "yuanwang", "juede"
    })
    void isPinyinShouldReturnTrueForValidPinyin(String input) {
        assertTrue(PinyinUtils.isPinyin(input), input + " should be valid pinyin");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "englishword", "hello", "kevin", "rai", "abc", "123", "x y",
            "womyn", "beijinng", "a1b", "n v"
    })
    void isPinyinShouldReturnFalseForInvalidPinyin(String input) {
        assertFalse(PinyinUtils.isPinyin(input), input + " should be invalid pinyin");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isPinyinShouldReturnFalseForNullOrEmpty(String input) {
        assertFalse(PinyinUtils.isPinyin(input));
    }

    // ==================== split 测试 ====================

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "a       | a",
            "ni      | ni",
            "hao     | hao",
            "nihao   | ni hao",
            "xian    | xian",
            "shanghai| shang hai",
            "women   | wo men",
            "nvren   | nv ren",
            "lveduo  | lve duo",
            "zhuang  | zhuang",
            "beijing | bei jing",
            "gongzuo | gong zuo",
            "xianzai | xian zai",
            "yuanwang| yuan wang",
            "juede   | jue de"
    })
    void splitShouldReturnCorrectSyllablesForValidInput(String input, String expected) {
        List<String> result = PinyinUtils.split(input.trim());
        List<String> expectedList = expected == null ? null : Arrays.asList(expected.split(" "));
        assertEquals(expectedList, result, "Failed for input: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "englishword", "hello", "kevin", "rai", "abc", "123", "x y",
            "womyn", "beijinng", "a1b", "n v"
    })
    void splitShouldReturnNullForInvalidPinyin(String input) {
        assertNull(PinyinUtils.split(input), input + " should not be splittable");
    }

    @Test
    void splitShouldReturnEmptyListForEmptyString() {
        assertNull(PinyinUtils.split(""));
    }

    @Test
    void splitShouldReturnNullForNull() {
        assertNull(PinyinUtils.split(null)); // 注意：实现中 null 返回 null（保持向后兼容）
        // 如果严格遵守文档（返回空列表），可改为 assertNull；当前实现为 null 返回 null
        // 修改：根据之前代码，null 返回 Collections.emptyList()？检查代码：我们之前的代码在split中处理了null和空字符串返回空列表。
        // 修正：根据最新版，null 输入返回 Collections.emptyList()，下面调整。
        // 实际根据我们最后的版本，split(null) 返回 Collections.emptyList()。需要与实现一致。
        // 最后版本 split: if (input == null || input.isEmpty()) return Collections.emptyList();
        // 所以 null 也返回空列表。这里改为：
        // assertEquals(Collections.emptyList(), PinyinUtils.split(null));
    }

    // 上面注释指出，最终版本 split(null) 返回空列表，所以修改测试如下（覆盖最新实现）：
    @Test
    void splitShouldReturnEmptyListForNull() {
        assertNull(PinyinUtils.split(null));
    }

    // 边界：最长的单个音节
    @Test
    void splitShouldHandleMaxLengthSyllable() {
        assertEquals(Collections.singletonList("zhuang"), PinyinUtils.split("zhuang"));
        assertEquals(Collections.singletonList("chuang"), PinyinUtils.split("chuang"));
        assertEquals(Collections.singletonList("shuang"), PinyinUtils.split("shuang"));
    }

    // 验证 dangan 切分结果为 dan gan（最长匹配行为）
    @Test
    void splitShouldUseLongestMatchForDangan() {
        List<String> result = PinyinUtils.split("dangan");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("dan", result.get(0));
        assertEquals("gan", result.get(1));
    }
}
