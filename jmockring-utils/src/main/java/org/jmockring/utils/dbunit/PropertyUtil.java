package org.jmockring.utils.dbunit;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Pavel Lechev
 * @since 25/06/13
 */
public final class PropertyUtil {

    public static final Pattern FN_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private PropertyUtil() {
    }


    /**
     * @param property
     * @return
     */
    public static String configurePlaceholders(String property) {
        String parsedProperty = property;
        Matcher matcher = FN_PATTERN.matcher(property);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = getSystemOrEnvProperty(placeholder, true);
            parsedProperty = parsedProperty.replace("{" + placeholder + "}", value);
        }
        return parsedProperty;
    }

    /**
     * @param key
     * @param required
     * @return
     */
    public static String getSystemOrEnvProperty(String key, boolean required) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if (required && value == null) {
            throw new IllegalArgumentException("Can't find system or environment value for null property");
        }
        return value;
    }

    /**
     * @param properties
     * @param propertyName
     * @return
     */
    public static String getProperty(final Properties properties, String propertyName) {
        String value = properties.getProperty(propertyName);
        if (value != null && value.startsWith("${")) {
            propertyName = value.substring(2, value.length() - 1);
            value = getSystemOrEnvProperty(propertyName, false);
        }
        return value;
    }


}
