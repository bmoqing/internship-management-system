/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.common;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public final class PasswordPolicyUtil {

    private static final int MIN_LEN = 8;
    private static final int MAX_LEN = 32;

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicyUtil() {
    }

    public static String validate(String password) {
        if (!StringUtils.hasText(password)) {
            return "密码不能为空";
        }
        if (password.contains(" ")) {
            return "密码不能包含空格";
        }
        if (password.length() < MIN_LEN || password.length() > MAX_LEN) {
            return "密码长度需在" + MIN_LEN + "-" + MAX_LEN + "位之间";
        }

        int kindCount = 0;
        if (UPPER.matcher(password).find()) {
            kindCount++;
        }
        if (LOWER.matcher(password).find()) {
            kindCount++;
        }
        if (DIGIT.matcher(password).find()) {
            kindCount++;
        }
        if (SYMBOL.matcher(password).find()) {
            kindCount++;
        }

        if (kindCount < 3) {
            return "密码需至少包含大写字母、小写字母、数字、特殊字符中的3类";
        }
        return null;
    }
}
