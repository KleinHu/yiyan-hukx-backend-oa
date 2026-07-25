# AI 附件问答后端离线依赖

本目录用于交付“AI 对话附件问答一期”新增的 Maven 依赖及其传递依赖，避免在内网环境为 PDF、Word、Excel 解析能力单独联网下载。

## 新增依赖

- `org.apache.pdfbox:pdfbox:2.0.30`
- `org.apache.poi:poi-ooxml:5.2.5`
- `org.apache.poi:poi-scratchpad:5.2.5`
- `org.apache.commons:commons-compress:1.25.0`
- `org.springframework.boot:spring-boot-starter-test`（仅测试）

`repository/` 是标准 Maven 本地仓库目录，包含上述依赖的 JAR、POM 与传递依赖。`pom.xml` 仅用于在可联网环境重新生成该目录，不参与 OA 应用构建。

`yiyan-ai-attachment-maven-deps-1.0.0.tar.gz` 是本目录的便携压缩包，便于转移至内网；解压后保留完整 Maven 坐标层级。

## 内网使用

将 `repository/` 中的坐标目录合并到团队现有的 Maven 本地仓库（其中应已包含 OA 的既有私服依赖）后，继续使用现有构建命令即可。

若已将“既有离线仓库”和本目录合并为一个完整仓库，可在工程根目录执行：

```bash
mvn -o -Dmaven.repo.local=./offline-maven-repository/repository -pl yiyan-demo-biz -am test
```

不要把本目录单独作为 OA 的完整离线仓库：它只包含本期新增依赖及 Maven 解析所需元数据，不包含 `com.cac.yiyan` 等项目既有私服依赖。合并时不要覆盖已有的私服依赖。

## 可联网环境重新生成

在后端工程根目录执行：

```bash
mvn -f offline-maven-repository/pom.xml \
  -Dmaven.repo.local=offline-maven-repository/repository \
  org.apache.maven.plugins:maven-dependency-plugin:3.6.1:go-offline
```

生成后可直接压缩并转移整个 `offline-maven-repository` 目录。
