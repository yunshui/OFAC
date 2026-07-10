# OFAC 黑名单筛查工具

OFAC 制裁名单筛查工具，支持 REST API 和 Excel 批量筛查两种方式。

## 项目结构

```
OFAC/
├── ofac/          # Spring Boot REST API 服务
└── ofac-xlsx/     # Excel 批量筛查 CLI 工具
```

---

## ofac — REST API 服务

Spring Boot 应用，提供 HTTP 接口进行黑名单查询。

### 构建

```bash
cd ofac
mvn clean package
```

### 运行

```bash
java -jar target/ofac-1.0.0.jar
```

服务默认启动在 `http://localhost:8080`。

### API 接口

**`GET /search?name=<查询姓名>`**

查询单个名字，返回 JSON 格式命中结果。

参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `name` | string | 是 | — | 要查询的姓名 |
| `empty2null` | boolean | 否 | `true` | `true` 时过滤空字符串字段 |
| `url` | string | 否 | API 默认地址 | 指定 API 地址 |
| `user` | string | 否 | 默认用户 | API 认证用户 |
| `pass` | string | 否 | 默认密码 | API 认证密码 |
| `unit` | string | 否 | 默认机构 | API 机构代码 |

示例：

```bash
curl "http://localhost:8080/search?name=陈平"
curl "http://localhost:8080/search?name=John&empty2null=false"
```

---

## ofac-xlsx — Excel 批量筛查

命令行工具，读取 Excel 文件中的姓名列表，逐条查询并将结果写入 Excel。

### 构建

```bash
cd ofac-xlsx
mvn clean package
```

### 使用

```bash
java -jar target/ofac-xlsx-1.0.0.jar -i input.xlsx -o output.xlsx
```

参数：

| 参数 | 说明 |
|---|---|
| `-i, --input PATH` | 输入 Excel 文件路径（必填） |
| `-o, --output PATH` | 输出 Excel 文件路径（必填） |
| `-u, --url URL` | API 地址 |
| `--user NAME` | API 用户 |
| `--pass PASSWORD` | API 密码 |
| `--unit CODE` | API 机构代码 |
| `-n, --num N` | 仅处理前 N 行 |
| `-h, --help` | 显示帮助 |

### 输入格式

- Excel 文件（.xlsx）
- 从第 1 行第 1 列开始读取姓名
- 空白单元格自动跳过
- 支持文本、数字格式

### 输出格式

每个查询姓名生成一个独立 Sheet，包含：
- **元数据**：查询姓名、命中状态、Message ID、List Date、List Author 等
- **明细表**（命中时）：Match、ID、ORIGIN、DESIGNATION、Names 等字段

---

## 技术栈

- Java 8
- Spring Boot 2.7.18（仅 ofac）
- Apache POI 5.2.3（Excel 读写）
- jsoup 1.18.3（HTML 解析）
- Jackson 2.18.2（JSON 序列化）
- Maven
