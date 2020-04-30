# 第三章：开发社区核心功能

## 过滤敏感词

### 前缀树数据结构定义

### 初始化前缀树

### 编写过滤敏感词方法

```java
@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    private static final String REPLACEMENT = "***";
    private TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init() {
        //类路径加载资源
        try (
                //获得缓冲字节流
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                //添加到前缀树
                this.addKeyWord(keyword);
            }

        } catch (IOException e) {
            logger.error("加载敏感词失败" + e.getMessage());
        }
    }

    private void addKeyWord(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode child = tempNode.getChild(c);
            if (child == null) {
                child = new TrieNode();
                tempNode.addChild(c, child);
            }
            tempNode = child;
        }
        tempNode.setKeywordEnd(true);
    }

    private class TrieNode {
        //关键词标识
        private boolean isKeywordEnd = false;
        //子节点
        private Map<Character, TrieNode> children = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addChild(Character c, TrieNode node) {
            children.put(c, node);
        }

        //获取子节点
        public TrieNode getChild(Character c) {
            return children.get(c);
        }
    }

    //过滤敏感词
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        TrieNode tempNode = rootNode;
        int len = text.length();
        StringBuilder ans = new StringBuilder();
        int start = 0, end = 0;
        while (end < len) {
            char c = text.charAt(end);
            if (isSymbol(c)) {
                if (tempNode == rootNode) {
                    ans.append(c);
                    start++;
                }
                end++;
                continue;
            }
            tempNode = tempNode.getChild(c);
            if (tempNode == null) {
                ans.append(text.charAt(start));
                start++;
                end = start;
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                ans.append(REPLACEMENT);
                start = end + 1;
                end = start;
                tempNode = rootNode;
            } else {
                end++;
            }
        }

        ans.append(text.substring(start));
        return ans.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c) {
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
}
```

