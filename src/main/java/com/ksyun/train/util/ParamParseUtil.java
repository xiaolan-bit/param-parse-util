package com.ksyun.train.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.*;

public class ParamParseUtil {
    private static int nextIndex = 0;
    public static <T> T parse(Class<T> clz, String queryString) throws Exception {
        // 输入异常判断
        if (clz == null) {
            return null;
        }else if (queryString == null || queryString.isEmpty()) {
            return null;
        }else{

            // 将参数转换并按字段名称排序
            String[] sortedParams = convertAndSortParameters(queryString);
            for (String param : sortedParams) {
                // System.out.println(param);
            }

            // 创建类的实例
            T instance;
            try {
                instance = clz.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException("创建" + clz.getSimpleName() + "实例失败", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法访问" + clz.getSimpleName() + "的构造函数", e);
            }

            // 解析并赋值参数
            try {
                ParamParseUtil.parse(instance, sortedParams);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("解析过程中找不到字段", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("解析过程中访问字段非法", e);
            }

            // 打印解析后的实例
            printInstance(instance);

            return instance;}
    }

    // 通过&对字符串进行分割处理和排序
    private static String[] convertAndSortParameters(String input) {
        String[] params = input.split("&");

        Arrays.sort(params, Comparator.comparing(s -> s.split("=")[0]));

        for (int i = 0; i < params.length; i++) {
            params[i] = convertToLowerCamelCase(params[i]);
        }

        return params;
    }

    // 将每个字符串的首字母变为小写
    private static String convertToLowerCamelCase(String str) {
        StringBuilder result = new StringBuilder();
        String[] words = str.split("\\.");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            word = Character.toLowerCase(word.charAt(0)) + word.substring(1);
            result.append(word);
            if (i < words.length - 1) {
                result.append(".");
            }
        }

        return result.toString();
    }

    // 利用反射得到类中的属性和注解，对字符串进行处理
    public static void parse(Object target, String[] assignments) throws NoSuchFieldException, IllegalAccessException {
        for (String assignment : assignments) {
            String[] parts = assignment.split("=", 2);
            String fieldName = parts[0].trim();
            String value = parts[1].trim();
            assignValue(target, fieldName, value);
        }
    }

    //利用字符串对目标对象进行赋值
    private static void assignValue(Object target, String fieldName, String value) throws NoSuchFieldException, IllegalAccessException {
        if (fieldName.contains(".")) {
            int dotIndex = fieldName.indexOf(".");
            String currentFieldName = fieldName.substring(0, dotIndex);
            String remainingFieldName = fieldName.substring(dotIndex + 1);
            // Remove the leading index and dot if exists

            removeLeadingIndex(remainingFieldName);
            removeLeadingIndex(remainingFieldName);


            Field field = getField(target.getClass(), currentFieldName);
            field.setAccessible(true);
            Object fieldValue = field.get(target);

            if (fieldValue == null) {
                Class<?> fieldType = field.getType();
                if (fieldType.isAssignableFrom(List.class)) {
                    fieldValue = new ArrayList<>();
                    field.set(target, fieldValue);  // Set the created list to the field
                } else {
                    fieldValue = createInstance(fieldType);
                    field.set(target, fieldValue);  // Set the created object to the field
                }
            }

            if (fieldValue instanceof List) {
                List<Object> list = (List<Object>) fieldValue;
                if (isIndexField(currentFieldName)) {
                    int index = getIndexFromFieldName(currentFieldName);
                    if (index >= list.size()) {
                        Class<?> genericType = getGenericType(field);
                        Object newItem = createInstance(genericType);
                        list.add(index, newItem);
                    }
                    assignValue(list.get(index), remainingFieldName, value);
                } else {
                    for (Object item : list) {
                        assignValue(item, remainingFieldName, value);
                    }
                }
            } else {
                assignValue(fieldValue, remainingFieldName, value);
            }

        } else {
            // Remove the leading index and dot if exists
            fieldName = removeLeadingIndex(fieldName);

            Field field = getField(target.getClass(), fieldName);
            field.setAccessible(true);

            // Check if the field has the SkipMappingValueAnnotation annotation
            if (field.isAnnotationPresent(SkipMappingValueAnnotation.class)) {
                return;  // Skip assigning value to this field
            }

            Class<?> fieldType = field.getType();
            Object convertedValue = convertValue(value, fieldType);
            field.set(target, convertedValue);
        }
    }

    //用于删除数字和点，更好的去寻找参数
    private static String removeLeadingIndex(String fieldName) {
        //fieldName = "2.command.1";
        if (fieldName.length() >= 2 && Character.isDigit(fieldName.charAt(0)) && fieldName.charAt(1) == '.') {
            return fieldName.substring(2);
        }
        if (fieldName.length()>=2){
            if (fieldName.endsWith(".2")){
                return fieldName.substring(0,fieldName.length()-2);
            }
        }
        return fieldName;
    }

    private static void printInstance(Object instance) {
        Class<?> clazz = instance.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(instance);
                // System.out.println(clazz.getSimpleName() + " " + field.getName() + ": " + value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    //根据字段名获取类的字段
    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getField(superClass, fieldName);
            } else {
                throw new NoSuchFieldException("Field not found: " + fieldName);
            }
        }
    }

    private static Object createInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建" + clazz.getSimpleName() + "实例失败", e);
        }
    }

    private static Class<?> getGenericType(Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

    private static boolean isIndexField(String fieldName) {
        return fieldName.matches("\\d+");
    }

    private static int getIndexFromFieldName(String fieldName) {
        return Integer.parseInt(fieldName) - 1;
    }

    //将字符串等号后面的值根据他在被反射类中所对应的属性进行判断，是否需要重新赋值并赋值
    private static Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        } else if (targetType == Long.class || targetType == long.class) {
            if (value.equalsIgnoreCase("null")) {
                return null;  // 对于 null long，返回 null
            }
            return Long.parseLong(value);
        } else if (targetType == BigDecimal.class) {
            return new BigDecimal(value);
        } else {
            // 根据需要处理其他类型
            try {
                if (targetType.isEnum()) {
                    return Enum.valueOf((Class<Enum>) targetType, value);
                } else {
                    Constructor<?> constructor = targetType.getDeclaredConstructor(String.class);
                    return constructor.newInstance(value);
                }
            } catch (Exception e) {
                throw new RuntimeException("将值转换为" + targetType.getSimpleName() + "时失败", e);
            }
        }
    }
}
