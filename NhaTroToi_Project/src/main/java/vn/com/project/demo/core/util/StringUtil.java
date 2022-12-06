package vn.com.project.demo.core.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.lang.NonNull;
import java.util.Locale;

@UtilityClass
public class StringUtil {

    public String camelToSnake(String str) {

        // Empty String
        StringBuilder result = new StringBuilder();

        // Append first character(in lower case)
        // to result string
        char c = str.charAt(0);
        result.append(Character.toLowerCase(c));

        // Traverse the string from
        // ist index to last index
        for (int i = 1; i < str.length(); i++) {

            char ch = str.charAt(i);

            // Check if the character is upper case
            // then append '_' and such character
            // (in lower case) to result string
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            }

            // If the character is lower case then
            // add such character into result string
            else {
                result.append(ch);
            }
        }

        // return the result
        return result.toString();
    }

    @NonNull
    public boolean isEmptyString(String value, String stringName, StringBuilder stringBuilder, MessageSource messageSource) {
        if (StringUtils.isBlank(value)) {
            stringBuilder.append(messageSource.getMessage("payment.reconcile.is.not.empty",
                    new Object[]{stringName}, Locale.forLanguageTag("vi-VN")));
            return true;
        }
        return false;
    }

}
