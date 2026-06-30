# 实训成果智能评价系统

## Training Achievement Intelligent Evaluation System

基于 AI 大模型的实训成果自动评价平台，支持学生提交项目文件、AI 自动评分（Auto-Scoring）、教师手动调分、PDF 报告生成的全流程管理。

---

### ✨ 功能特性

| 功能 | 说明 |
|------|------|
| **AI 自动评分** | 对接智谱 GLM-4（或 Ollama 本地模型），自动对实训成果进行多维评分 |
| **多维评价体系** | 支持完成度、技术质量、创新性、文档规范四个维度，各维度权重可配置 |
| **文件解析核查** | 自动解压压缩包、解析代码/文档/图片，提取内容用于 AI 分析 |
| **成果预检** | 提交后 AI 自动核查文件完整性、文档齐全性，确认是否符合基本要求 |
| **PDF 报告导出** | 一键生成含图表、评分明细、改进建议的专业评价报告 |
| **实时通知** | 基于 WebSocket 的实时状态推送（解析完成/评分完成/新任务等） |
| **角色隔离** | 学生端（成果上传/我的提交）与教师端（任务管理/多维评价/报表导出）分离 |

---

### 🛠 技术栈 (Tech Stack)

| 层级 | 技术 | 版本 |
|------|------|------|
| **Backend** | Java + Spring Boot | JDK 17, Spring Boot 3.2.5 |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **Database** | MySQL | 8.0+ |
| **Security** | Spring Security + JWT | jjwt 0.12.5 |
| **AI Integration** | 智谱 GLM-4 / Ollama | OpenAI-compatible API |
| **File Parsing** | Apache POI + PDFBox + Commons Compress | — |
| **PDF Generation** | Flying Saucer + Batik (SVG) | 9.7.2 |
| **Frontend** | Vue 3 + TypeScript | Vue 3.4, Vite 5 |
| **UI Framework** | Element Plus | 2.7 |
| **Charts** | ECharts | 6.1 |
| **State** | Pinia | 2.1 |
| **Build** | Maven + npm | — |

---

### 📂 目录结构 (Directory Structure)

```
training-evaluation/
├── README.md                    # 项目说明
├── .gitignore                   # Git 忽略规则
├── backend/                     # 后端 (Spring Boot)
│   ├── pom.xml                  # Maven 依赖配置
│   ├── init.sql                 # 数据库建表脚本
│   ├── data.sql                 # 演示数据脚本
│   └── src/
│       ├── main/java/com/example/evaluation/
│       │   ├── EvaluationApplication.java    # 应用入口
│       │   ├── controller/                   # REST 控制器 (8 个)
│       │   ├── service/                      # 服务接口
│       │   │   └── impl/                     # 服务实现
│       │   ├── mapper/                       # MyBatis 映射器
│       │   ├── entity/                       # 数据库实体
│       │   ├── dto/                          # 数据传输对象
│       │   ├── config/                       # Spring 配置
│       │   ├── security/                     # JWT 认证与授权
│       │   ├── ai/                           # AI 服务 (云端/本地)
│       │   ├── websocket/                    # WebSocket 实时通知
│       │   └── utils/                        # 工具类
│       ├── main/resources/
│       │   ├── application.yml               # 主配置文件
│       │   ├── application-prod.yml          # 生产环境配置
│       │   ├── mapper/                       # MyBatis XML 映射
│       │   ├── prompts/                      # AI Prompt 模板
│       │   ├── templates/                    # PDF 报告 HTML 模板
│       │   └── fonts/                        # PDF 渲染字体
│       └── test/                             # 单元测试 (17 个用例)
├── frontend/                    # 前端 (Vue 3)
│   ├── package.json             # npm 依赖
│   ├── vite.config.ts           # Vite 配置 (含 API/WS 代理)
│   ├── tsconfig.json            # TypeScript 配置
│   └── src/
│       ├── main.ts              # 应用入口
│       ├── App.vue              # 根组件 (布局 + 通知栏)
│       ├── router/              # Vue Router 路由定义
│       ├── stores/              # Pinia 状态管理
│       ├── views/               # 页面视图 (11 个)
│       ├── components/          # 公共组件
│       ├── api/                 # API 请求封装
│       ├── composables/         # 组合式函数
│       ├── types/               # TypeScript 类型定义
│       ├── utils/               # 工具函数 (axios/websocket/crypto)
│       └── styles/              # 全局样式
```

---

### 🔧 环境要求 (Prerequisites)

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| **JDK** | 17+ | 推荐 OpenJDK 17 / 21 LTS |
| **Maven** | 3.6+ | 后端构建 |
| **Node.js** | 18+ | 推荐 20 LTS，前端构建 |
| **npm** | 9+ | 随 Node.js 附带 |
| **MySQL** | 8.0+ | 数据库存储 |

> 生产部署额外需要 **Nginx**（反向代理）和 **systemd**（服务管理）。

---

### 🚀 本地开发启动 (Quick Start)

#### 1. 数据库初始化

```bash
# 登录 MySQL，执行建表脚本
mysql -u root -p < backend/init.sql

# 导入演示数据（可选）
mysql -u root -p < backend/data.sql
```

#### 2. 启动后端

```bash
cd backend

# 方式 A：Maven 直接运行
mvn spring-boot:run

# 方式 B：设置环境变量后运行 JAR
export DB_PASSWORD=your_mysql_root_password
mvn clean package -DskipTests
java -jar target/evaluation-1.0.0.jar
```

后端启动后访问 `http://localhost:8080`。

#### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问 `http://localhost:5173`，API 请求自动代理到后端 `8080` 端口。

#### 4. 登录

初始演示账号（来自 `data.sql`）：

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 教师 | teacher01 | 123456 |
| 学生 | student01 | 123456 |

---

### ⚙️ 必须修改的配置 (Required Configuration)

clone 项目后，**必须修改以下配置**才能启动系统：

#### 1. 数据库密码

文件：`backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:CHANGE_ME_DB_PASSWORD}  # 替换为你的 MySQL root 密码
```

#### 2. JWT 签名密钥

文件：`backend/src/main/resources/application.yml`

```yaml
jwt:
  secret: CHANGE_ME_GENERATE_WITH_openssl_rand_-base64_64
```

生成命令：
```bash
openssl rand -base64 64
```

#### 3. AI 大模型配置

系统支持三种 AI 模式，根据实际需求选择其中一种配置。

**模式 A：智谱 GLM-4 云端 API（默认）**

文件：`backend/src/main/resources/application.yml`

```yaml
ai:
  provider: cloud                     # 保持 cloud
  cloud:
    base-url: ${AI_BASE_URL:https://open.bigmodel.cn/api/paas/v4}  # 无需修改
    api-key: ${AI_API_KEY:CHANGE_ME_GET_FROM_bigmodel_cn}          # 替换为你的 API Key
    model: ${AI_MODEL:CHANGE_ME_MODEL_NAME}                        # 替换为模型名，如 glm-4-flash
    vision-model: ${AI_VISION_MODEL:CHANGE_ME_VISION_MODEL_NAME}   # 替换为视觉模型名，如 glm-4v-flash
```

API Key 获取：前往 [bigmodel.cn](https://open.bigmodel.cn) 注册并创建 API Key。

**模式 B：其他 OpenAI 兼容 API 服务**

修改 `ai.cloud` 中的 `base-url`、`api-key`、`model`，指向目标服务即可：

```yaml
ai:
  provider: cloud
  cloud:
    base-url: https://api.openai.com/v1      # 改为目标 API 地址
    api-key: sk-your-api-key-here            # 改为目标 API Key
    model: gpt-4o                            # 改为目标模型名
    vision-model: gpt-4o                     # 改为目标视觉模型名
```

**模式 C：本地 Ollama**

```yaml
ai:
  provider: local                    # 改为 local
  local:
    base-url: http://localhost:11434/v1  # Ollama 默认地址
    model: CHANGE_ME_LOCAL_MODEL_NAME    # 替换为你已拉取的模型名，如 qwen2.5:7b
```

前提：安装 Ollama 并拉取模型：
```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull qwen2.5:7b
```

---

### 📦 打包部署 (Build & Deploy)

#### 构建

```bash
# 后端
cd backend
mvn clean package -DskipTests
# 产物: target/evaluation-1.0.0.jar

# 前端
cd ../frontend
npm install
npm run build
# 产物: dist/
```

#### 部署

构建完成后，将以下产物部署至服务器：

| 产物 | 路径 | 说明 |
|------|------|------|
| 后端 JAR | `backend/target/evaluation-1.0.0.jar` | 需 JDK 17+ 运行 |
| 前端静态文件 | `frontend/dist/` | 需通过 Nginx 等 Web 服务器提供 |

**部署要点**：

1. 通过环境变量注入敏感配置，避免硬编码：

```bash
export DB_PASSWORD=your_password
export AI_API_KEY=your_api_key
export AI_BASE_URL=https://open.bigmodel.cn/api/paas/v4
export AI_MODEL=glm-4-flash
export AI_VISION_MODEL=glm-4v-flash
export TEACHER_INVITE_CODE=jiaoshi
# 生产环境需额外设置
export SPRING_PROFILES_ACTIVE=prod
```

2. 启动后端：

```bash
java -Djava.awt.headless=true -Xms512m -Xmx1024m -jar evaluation-1.0.0.jar
```

3. 配置 Nginx 反向代理（示例 `location`）：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
location /ws {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
location / {
    root /path/to/frontend/dist;
    try_files $uri $uri/ /index.html;
}
```

4. （可选）配置 systemd 服务实现开机自启和自动重启。

---

### 📄 开源协议 (License)

本项目基于 [MIT License](https://opensource.org/licenses/MIT) 开源。

---

### ⚠️ 免责声明 (Disclaimer)

1. 本仓库中 `backend/data.sql` 包含的演示账号密码哈希、`backend/init.sql` 中的数据库用户凭据**仅供本地开发测试使用**，切勿用于生产环境。
2. 本项目依赖的 AI 大模型（智谱 GLM-4 等）由第三方提供服务，使用前请遵守相应服务商的使用条款。
3. 本系统对实训成果的评价结果仅供参考，不作为最终评判依据。
