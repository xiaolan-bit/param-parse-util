# param-parse-util

这是一个名为 ParamParseUtil 的实用工具类，提供了基于给定查询字符串的解析和字段赋值的方法。

## 功能

### ParamParseUtil 类提供以下功能：

    parse：该方法接受目标类和查询字符串作为输入，将查询字符串解析并将值分配给目标对象的相应字段。
    convertAndSortParameters：这是一个私有方法，将查询字符串转换为参数数组，并根据字段名称进行排序。
    convertToLowerCamelCase：这是一个私有方法，将每个参数的字段名转换为小驼峰命名格式。
    parse：这是一个私有方法，递归地根据解析的参数为目标对象的字段赋值。
    assignValue：这是一个私有方法，将值分配给目标对象的特定字段。
    removeLeadingIndex：这是一个私有方法，如果存在，从字段名中移除前导索引。
    printInstance：这是一个私有方法，打印给定实例的所有字段值。
    getField：这是一个私有方法，根据字段名获取类的字段。
    createInstance：这是一个私有方法，创建类的新实例。
    getGenericType：这是一个私有方法，获取字段的泛型类型（如果是集合类型）。
    isIndexField：这是一个私有方法，检查字段名是否表示索引。
    getIndexFromFieldName：这是一个私有方法，从字段名中获取索引值。
    convertValue：这是一个私有方法，将字符串值转换为适当的类型，根据目标字段的类型进行转换。

## 使用方法

要使用 ParamParseUtil 类，请按照以下步骤进行操作：

    导入 com.ksyun.train.util.ParamParseUtil 包。
    调用 parse 方法，传入目标类和查询字符串作为参数。

### 示例
import com.ksyun.train.util.ParamParseUtil;

public class Main {
    public static void main(String[] args) {
        try {
            MyClass instance = ParamParseUtil.parse(Pod.class, "param1=value1&param2=value2");
            // 对解析后的实例进行处理
        } catch (Exception e) {
            // 处理异常
        }
    }
}

Pod 替换为实际要解析的目标类，并向 parse 方法提供适当的查询字符串。

