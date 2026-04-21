# EchoMind - AI 智能面试平台

一个基于 AI 的智能面试平台，提供模拟面试、简历分析、知识库管理等功能。

## 项目架构

### 技术栈

#### 后端 (Spring Boot)
- **框架**: Spring Boot 4.0
- **语言**: Java 21
- **AI 集成**: Spring AI 2.0
- **数据库**: PostgreSQL + pgvector (向量存储)
- **缓存/消息队列**: Redis
- **对象存储**: MinIO (兼容 S3)
- **ORM**: Spring Data JPA
- **构建工具**: Gradle

#### 前端 (Vue)
- **框架**: Vue 3
- **构建工具**: Vite
- **UI 组件**: VueUse + Tailwind CSS
- **图表库**: Chart.js
- **HTTP 客户端**: Axios
- **其他**: PixiJS (动画), Live2D (虚拟形象)

### 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     用户界面 (Browser)                         │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  EchoMind 前端应用 (Vue 3)                           │       │
│  │  - 模拟面试房间 (InterviewRoom)                      │       │
│  │  - 简历管理 (Resume Management)                       │       │
│  │  - 知识库管理 (Knowledge Base)                       │       │
│  │  - 面试历史/分析 (Interview History & Analytics)      │       │
│  └──────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
                              ↓ HTTP/HTTPS
┌─────────────────────────────────────────────────────────────────┐
│                    后端服务 (Spring Boot)                     │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  1. API 层 (Controllers)                             │       │
│  │     - 用户管理、简历分析、模拟面试、知识库管理         │       │
│  └──────────────────────────────────────────────────────┘       │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  2. 业务逻辑层 (Services)                            │       │
│  │     - 面试流程编排、AI 交互、数据分析                  │       │
│  └──────────────────────────────────────────────────────┘       │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  3. 数据访问层 (Repositories)                        │       │
│  │     - JPA 仓库、向量存储 (pgvector)、Redis 缓存        │       │
│  └──────────────────────────────────────────────────────┘       │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  4. AI 集成层 (Spring AI)                            │       │
│  │     - 大语言模型集成 (阿里云百炼)                     │       │
│  │     - 向量存储检索 (pgvector)                        │       │
│  │     - RAG (检索增强生成)                              │       │
│  └──────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                     基础设施层 (Docker)                     │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  PostgreSQL (pgvector) - 数据存储 & 向量检索         │       │
│  └──────────────────────────────────────────────────────┘       │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  Redis - 缓存 & 消息队列                             │       │
│  └──────────────────────────────────────────────────────┘       │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  MinIO - 对象存储 (简历、知识库文档)                  │       │
│  └──────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

## 快速开始

### 前置要求

- Docker 19.03+
- Docker Compose 2.0+
- 至少 4GB 可用内存

### 启动方式

#### 1. 环境准备

```bash
# 复制环境变量文件
cp .env.example .env

# 编辑 .env 文件，设置 AI 相关配置
# 主要需要设置：AI_BAILIAN_API_KEY (阿里云百炼 API Key)
```

#### 2. 启动服务

```bash
# 启动所有服务
docker-compose up -d
```

#### 3. 验证服务是否正常

```bash
# 检查容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 访问应用
# 前端: http://localhost
# 后端 API: http://localhost:8080
# MinIO 控制台: http://localhost:9001
# PostgreSQL: localhost:5432
# Redis: localhost:6379
```

### 开发模式

#### 后端开发 (本地运行)

```bash
# 进入后端目录
cd app

# 构建项目
./gradlew build

# 本地运行（需要先启动基础设施服务）
./gradlew bootRun
```

#### 前端开发 (本地运行)

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 本地开发模式
npm run dev
```

## 项目结构

```
EchoMind/
├── app/                              # 后端应用 (Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── interview/guide/
│   │   │   │       ├── App.java     # 主应用入口
│   │   │   │       ├── common/      # 通用配置、工具、异常处理
│   │   │   │       ├── infrastructure/  # 基础设施层 (JPA 仓库、向量存储、文件存储)
│   │   │   │       └── modules/     # 业务模块
│   │   │   └── resources/
│   │   │       ├── application.properties  # Spring Boot 配置
│   │   │       └── static/          # 静态资源
│   │   └── test/                    # 测试代码
│   ├── build.gradle                 # Gradle 构建配置
│   └── Dockerfile                   # Docker 构建文件
├── frontend/                        # 前端应用 (Vue 3)
│   ├── src/
│   │   ├── api/                    # API 请求封装
│   │   ├── components/             # 通用组件
│   │   ├── lib/                    # 工具函数
│   │   ├── pages/                  # 页面组件
│   │   ├── stores/                 # 状态管理 (Pinia)
│   │   ├── App.vue                 # 根组件
│   │   └── main.js                 # 应用入口
│   ├── public/                     # 公共资源
│   ├── index.html                  # HTML 入口
│   ├── package.json                # 项目依赖配置
│   └── vite.config.js              # Vite 配置
├── docker/                         # Docker 相关配置
│   └── postgres/
│       └── init.sql                # PostgreSQL 初始化脚本
├── docker-compose.yml              # Docker 服务编排
├── .env.example                    # 环境变量示例
├── settings.gradle                 # Gradle 项目配置
├── gradlew / gradlew.bat          # Gradle 包装脚本
└── README.md                       # 项目说明
```

## 核心功能

### 1. 智能面试系统

#### 模拟面试
- 虚拟 AI 面试官 (Live2D 形象)
- 实时语音识别与合成 (ASR/TTS)
- 多模态交互 (文字、语音、表情)
- 个性化面试题目生成

#### 面试类型
- 基础面试 (General Interview)
- 技术面试 (Technical Interview)
- 行为面试 (Behavioral Interview)
- 案例面试 (Case Interview)

#### 面试过程
- 简历分析与岗位匹配
- 智能题库管理
- 实时评分与反馈
- 面试报告生成

### 2. 简历管理

- 简历上传与解析
- 简历内容分析
- 简历与岗位匹配度评估
- 简历存储与管理

### 3. 知识库管理 (RAG)

- 知识库文档上传 (PDF、DOCX、TXT)
- 文档内容向量化存储 (pgvector)
- 智能检索与问答
- 知识库分组与管理

### 4. 数据分析与报告

- 面试数据统计
- 技能图谱展示 (Radar Chart)
- 面试趋势分析
- 详细面试报告生成与导出 (PDF)

## API 接口

### 基础 URL
- 生产环境: `http://localhost:8080`

### 主要接口

#### 认证
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/me` - 获取当前用户信息

#### 简历管理
- `POST /api/resumes` - 上传简历
- `GET /api/resumes` - 获取简历列表
- `GET /api/resumes/{id}` - 获取简历详情
- `DELETE /api/resumes/{id}` - 删除简历

#### 面试管理
- `POST /api/interviews` - 开始新面试
- `GET /api/interviews` - 获取面试历史
- `GET /api/interviews/{id}` - 获取面试详情
- `POST /api/interviews/{id}/feedback` - 提交反馈

#### 知识库管理
- `POST /api/knowledge-base` - 上传知识库文档
- `GET /api/knowledge-base` - 获取知识库列表
- `GET /api/knowledge-base/{id}` - 获取知识库详情
- `DELETE /api/knowledge-base/{id}` - 删除知识库

## 部署说明

### 生产部署

#### 1. 环境变量配置

创建 `.env` 文件并配置以下变量：

```env
# AI 配置 (必填)
AI_BAILIAN_API_KEY=your_api_key_here

# AI 模型配置 (可选)
AI_MODEL=qwen-plus

# 数据库配置 (可选，默认值如下)
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=interview_guide
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password

# Redis 配置
REDIS_HOST=redis
REDIS_PORT=6379

# MinIO 配置
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=interview-guide

# 服务器配置
SERVER_PORT=8080
```

#### 2. 启动服务

```bash
# 生产环境启动（建议）
docker-compose up -d

# 或单机模式（仅后端）
cd app
./gradlew bootRun --prod
```

### 数据持久化

- **PostgreSQL**: 数据存储在 `postgres_data` Docker 卷中
- **Redis**: 数据存储在 `redis_data` Docker 卷中
- **MinIO**: 数据存储在 `minio_data` Docker 卷中

这些卷在执行 `docker-compose down` 时不会被删除，数据会持久化保存。

## 开发注意事项

### 代码风格

#### 后端
- 使用 Lombok 简化代码
- 统一异常处理
- 接口返回格式统一
- 使用 MapStruct 进行对象转换

#### 前端
- 使用 Vue 3 组合式 API
- 遵循 TypeScript 语法
- 使用 ESLint 进行代码检查
- 使用 Prettier 格式化代码

### 数据库设计

#### 核心表结构
- `users`: 用户信息
- `resumes`: 简历信息
- `interviews`: 面试记录
- `interview_questions`: 面试题目
- `interview_answers`: 面试答案
- `knowledge_base`: 知识库
- `knowledge_base_chunks`: 知识库段落（向量存储）

## 常见问题

### 1. 如何获取阿里云百炼 API Key？

1. 访问 [阿里云百炼控制台](https://dashscope.aliyun.com/)
2. 注册/登录账号
3. 创建应用
4. 获取 API Key

### 2. 如何重置数据库？

```bash
# 停止服务
docker-compose down

# 删除数据卷
docker volume rm echomind_postgres_data echomind_redis_data echomind_minio_data

# 重新启动
docker-compose up -d
```

### 3. 如何查看日志？

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f app       # 后端
docker-compose logs -f frontend  # 前端
docker-compose logs -f postgres  # 数据库
```

## 技术文档

### 开发指南

- [Spring Boot 4.0 文档](https://docs.spring.io/spring-boot/docs/4.0.x/reference/html/)
- [Vue 3 文档](https://vuejs.org/)
- [Vite 文档](https://vitejs.dev/)
- [PostgreSQL 文档](https://www.postgresql.org/docs/)
- [Redis 文档](https://redis.io/)
- [MinIO 文档](https://min.io/)

### AI 相关

- [Spring AI 2.0 文档](https://docs.spring.io/spring-ai/reference/)
- [阿里云百炼 SDK](https://help.aliyun.com/document_detail/2511816.html)

## 贡献指南

### 开发流程

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/XXX`)
3. 提交更改 (`git commit -am 'Add feature XXX'`)
4. 推送到分支 (`git push origin feature/XXX`)
5. 创建 Pull Request

### 代码规范

- 遵循项目现有的代码风格
- 确保所有测试通过
- 提交信息清晰、规范

## 许可证

本项目使用 MIT 许可证 - 详见 LICENSE 文件

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件
- 项目讨论区

---

*EchoMind - 让面试更智能，让招聘更高效*
