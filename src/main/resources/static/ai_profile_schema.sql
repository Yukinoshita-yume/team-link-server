-- ============================================================
-- AI 赛事组队平台 —— 用户画像 & 标准技能标签库
-- 第一阶段·数据层新增表结构
-- 注意：本文件仅追加新表，不修改 database.sql 中的原有代码
-- ============================================================

-- ============================================================
-- 0. 清理旧表（关闭外键检查，按依赖顺序删除）
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS profile_generation_log;
DROP TABLE IF EXISTS user_portfolio;
DROP TABLE IF EXISTS user_competition_experience;
DROP TABLE IF EXISTS user_domain;
DROP TABLE IF EXISTS user_skill_tag;
DROP TABLE IF EXISTS user_unavailable_date;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS skill_tag_synonym;
DROP TABLE IF EXISTS skill_tag;

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 一、标准技能标签库
-- ============================================================

-- 1-1 技能标签主表（三级层级体系）
CREATE TABLE skill_tag (
                           tag_id        INT AUTO_INCREMENT PRIMARY KEY,
                           tag_name      VARCHAR(60) NOT NULL,
                           tag_level     TINYINT NOT NULL DEFAULT 1
                               CHECK (tag_level BETWEEN 1 AND 3),
                           parent_tag_id INT DEFAULT NULL,
                           tag_category  VARCHAR(30) NOT NULL,
                           tag_aliases   VARCHAR(255) DEFAULT NULL,
                           tag_keywords  VARCHAR(255) DEFAULT NULL,
                           is_active     BOOLEAN NOT NULL DEFAULT TRUE,
                           sort_order    INT DEFAULT 0,
                           created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           FOREIGN KEY (parent_tag_id) REFERENCES skill_tag(tag_id)
                               ON DELETE SET NULL ON UPDATE CASCADE,
                           UNIQUE KEY uq_tag_name_parent (tag_name, parent_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='标准技能标签库（三级层级）';

-- 1-2 标签同义词映射表
CREATE TABLE skill_tag_synonym (
                                   synonym_id    INT AUTO_INCREMENT PRIMARY KEY,
                                   tag_id        INT NOT NULL,
                                   synonym_text  VARCHAR(120) NOT NULL,
                                   match_type    ENUM('exact','prefix','fuzzy') NOT NULL DEFAULT 'exact',
                                   created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (tag_id) REFERENCES skill_tag(tag_id)
                                       ON DELETE CASCADE ON UPDATE CASCADE,
                                   UNIQUE KEY uq_synonym (synonym_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='技能标签同义词映射表';


-- ============================================================
-- 二、用户画像数据库表结构
-- ============================================================

-- 2-1 用户画像主表
CREATE TABLE user_profile (
                              profile_id          INT AUTO_INCREMENT PRIMARY KEY,
                              user_id             INT NOT NULL UNIQUE,
                              score_tech_depth    TINYINT UNSIGNED DEFAULT 0   COMMENT '技术深度评分 0-100',
                              score_competition   TINYINT UNSIGNED DEFAULT 0   COMMENT '竞赛经验评分 0-100',
                              score_teamwork      TINYINT UNSIGNED DEFAULT 0   COMMENT '团队协作评分 0-100',
                              score_learning      TINYINT UNSIGNED DEFAULT 0   COMMENT '学习能力评分 0-100',
                              score_availability  TINYINT UNSIGNED DEFAULT 0   COMMENT '时间投入评分 0-100',
                              composite_score     TINYINT UNSIGNED
                                  AS (
                                      ROUND(
                                              score_tech_depth   * 0.30 +
                                              score_competition  * 0.25 +
                                              score_teamwork     * 0.20 +
                                              score_learning     * 0.15 +
                                              score_availability * 0.10
                                      )
                                      ) STORED                     COMMENT '综合加权评分（自动计算）',
                              ability_summary     VARCHAR(200) DEFAULT NULL     COMMENT 'LLM 生成的能力摘要',
                              weekly_hours        TINYINT UNSIGNED DEFAULT 0    COMMENT '每周可投入小时数',
                              available_periods   SET('WEEKDAY_EVENING','WEEKEND','ALL_DAY') DEFAULT NULL COMMENT '可用时段',
                              busy_level          ENUM('FREE','NORMAL','BUSY') NOT NULL DEFAULT 'NORMAL' COMMENT '短期可用等级',
                              raw_input_snapshot  JSON DEFAULT NULL             COMMENT '用户原始输入快照',
                              llm_output_snapshot JSON DEFAULT NULL             COMMENT 'LLM 输出原始 JSON 快照',
                              profile_version     SMALLINT UNSIGNED DEFAULT 1  COMMENT '画像版本号',
                              is_public           BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否对其他用户可见',
                              generated_at        DATETIME  DEFAULT NULL        COMMENT '最近一次画像生成时间',
                              created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              FOREIGN KEY (user_id) REFERENCES user(user_id)
                                  ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='用户画像主表';

-- 2-2 用户技能标签关联表
CREATE TABLE user_skill_tag (
                                id              INT AUTO_INCREMENT PRIMARY KEY,
                                user_id         INT NOT NULL,
                                tag_id          INT NOT NULL,
                                skill_level     ENUM('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL DEFAULT 'BEGINNER' COMMENT '技能熟练度',
                                confidence      DECIMAL(4,3) NOT NULL DEFAULT 0.500
                                    CHECK (confidence BETWEEN 0 AND 1),
                                evidence        VARCHAR(255) DEFAULT NULL COMMENT 'LLM 提取依据',
                                source          ENUM('LLM_EXTRACT','USER_CONFIRM','ADMIN') NOT NULL DEFAULT 'LLM_EXTRACT',
                                is_confirmed    BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                UNIQUE KEY uq_user_tag (user_id, tag_id),
                                FOREIGN KEY (user_id) REFERENCES user(user_id)   ON DELETE CASCADE ON UPDATE CASCADE,
                                FOREIGN KEY (tag_id)  REFERENCES skill_tag(tag_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='用户技能标签关联表';

-- 2-3 用户擅长方向表
CREATE TABLE user_domain (
                             id          INT AUTO_INCREMENT PRIMARY KEY,
                             user_id     INT NOT NULL,
                             domain      ENUM(
                                 'FRONTEND','FULLSTACK','ALGORITHM','AI_ML',
                                 'PRODUCT_DESIGN','DATA_ANALYSIS','EMBEDDED_HARDWARE',
                                 'BACKEND','SECURITY','OTHER'
                                 ) NOT NULL,
                             confidence  DECIMAL(4,3) NOT NULL DEFAULT 0.500
                                 CHECK (confidence BETWEEN 0 AND 1),
                             created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             UNIQUE KEY uq_user_domain (user_id, domain),
                             FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='用户擅长方向表';

-- 2-4 用户参赛经历表
CREATE TABLE user_competition_experience (
                                             exp_id              INT AUTO_INCREMENT PRIMARY KEY,
                                             user_id             INT NOT NULL,
                                             competition_name    VARCHAR(200) NOT NULL,
                                             competition_time    VARCHAR(50) DEFAULT NULL,
                                             role                VARCHAR(60) DEFAULT NULL,
                                             award               VARCHAR(100) DEFAULT NULL,
                                             description         TEXT DEFAULT NULL,
                                             is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
                                             created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='用户参赛经历结构化表';

-- 2-5 用户作品集表
CREATE TABLE user_portfolio (
                                portfolio_id    INT AUTO_INCREMENT PRIMARY KEY,
                                user_id         INT NOT NULL,
                                link_type       ENUM('GITHUB','PERSONAL_SITE','PAPER','OTHER') NOT NULL DEFAULT 'OTHER',
                                url             VARCHAR(512) NOT NULL,
                                title           VARCHAR(100) DEFAULT NULL,
                                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='用户作品集表';

-- 2-6 画像生成日志表
CREATE TABLE profile_generation_log (
                                        log_id          INT AUTO_INCREMENT PRIMARY KEY,
                                        user_id         INT NOT NULL,
                                        trigger_source  ENUM('USER_MANUAL','SYSTEM_AUTO','ADMIN') NOT NULL DEFAULT 'USER_MANUAL',
                                        status          ENUM('SUCCESS','FAILED','TIMEOUT') NOT NULL DEFAULT 'SUCCESS',
                                        latency_ms      INT UNSIGNED DEFAULT NULL,
                                        token_consumed  INT UNSIGNED DEFAULT NULL,
                                        error_message   VARCHAR(500) DEFAULT NULL,
                                        input_hash      CHAR(64) DEFAULT NULL,
                                        profile_version SMALLINT UNSIGNED DEFAULT NULL,
                                        created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
                                        INDEX idx_log_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='用户画像生成日志';

-- 2-7 用户不可用日期表
CREATE TABLE user_unavailable_date (
                                       id          INT AUTO_INCREMENT PRIMARY KEY,
                                       user_id     INT NOT NULL,
                                       start_date  DATE NOT NULL   COMMENT '不可用开始日期',
                                       end_date    DATE NOT NULL   COMMENT '不可用结束日期',
                                       reason      VARCHAR(100) DEFAULT NULL COMMENT '原因（如：期末考试）',
                                       created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       CONSTRAINT chk_date_range CHECK (end_date >= start_date),
                                       FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
                                       INDEX idx_unavailable_user_date (user_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='用户不可用日期表';


-- ============================================================
-- 三、字符集统一
-- ============================================================
ALTER TABLE skill_tag                   CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE skill_tag_synonym           CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_profile                CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_skill_tag              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_domain                 CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_competition_experience CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_portfolio              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE profile_generation_log      CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_unavailable_date       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================
-- 四、标准技能标签库种子数据
-- 所有二级、三级均用子查询动态查找父级 tag_id，不依赖硬编码
-- ============================================================

-- ── 一级领域（8个）─────────────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order) VALUES
                                                                                                      ('前端开发',       1, NULL, '前端开发',       'Frontend,前端',      10),
                                                                                                      ('后端开发',       1, NULL, '后端开发',       'Backend,后端',       20),
                                                                                                      ('算法与数据结构', 1, NULL, '算法与数据结构', 'Algorithm,算法',     30),
                                                                                                      ('人工智能',       1, NULL, '人工智能',       'AI,AI开发',          40),
                                                                                                      ('数据科学',       1, NULL, '数据科学',       'Data Science,数据',  50),
                                                                                                      ('产品与设计',     1, NULL, '产品与设计',     'Design,设计',        60),
                                                                                                      ('嵌入式与硬件',   1, NULL, '嵌入式与硬件',   'Embedded,硬件',      70),
                                                                                                      ('网络安全',       1, NULL, '网络安全',       'Security,安全',      80);

-- ── 二级方向：前端开发 ─────────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'React',      2, tag_id, '前端开发', 'ReactJS,React.js',   10 FROM skill_tag WHERE tag_name = '前端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Vue',        2, tag_id, '前端开发', 'VueJS,Vue.js',       20 FROM skill_tag WHERE tag_name = '前端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Angular',    2, tag_id, '前端开发', 'AngularJS',          30 FROM skill_tag WHERE tag_name = '前端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'TypeScript', 2, tag_id, '前端开发', 'TS',                 40 FROM skill_tag WHERE tag_name = '前端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'CSS/样式',   2, tag_id, '前端开发', 'CSS,Tailwind,SCSS',  50 FROM skill_tag WHERE tag_name = '前端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '小程序开发', 2, tag_id, '前端开发', '微信小程序,uni-app', 60 FROM skill_tag WHERE tag_name = '前端开发' AND tag_level = 1 LIMIT 1;

-- ── 二级方向：后端开发 ─────────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Java',       2, tag_id, '后端开发', 'Java后端',                 10 FROM skill_tag WHERE tag_name = '后端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Python后端', 2, tag_id, '后端开发', 'Flask,Django,FastAPI',     20 FROM skill_tag WHERE tag_name = '后端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Go',         2, tag_id, '后端开发', 'Golang',                   30 FROM skill_tag WHERE tag_name = '后端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Node.js',    2, tag_id, '后端开发', 'NodeJS,Express',           40 FROM skill_tag WHERE tag_name = '后端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Spring',     2, tag_id, '后端开发', 'Spring Boot,SpringMVC',    50 FROM skill_tag WHERE tag_name = '后端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '数据库',     2, tag_id, '后端开发', 'MySQL,PostgreSQL,MongoDB', 60 FROM skill_tag WHERE tag_name = '后端开发' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '微服务',     2, tag_id, '后端开发', 'Microservice,Docker,K8s',  70 FROM skill_tag WHERE tag_name = '后端开发' AND tag_level = 1 LIMIT 1;

-- ── 二级方向：算法与数据结构 ───────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '数据结构', 2, tag_id, '算法与数据结构', '链表,树,图',        10 FROM skill_tag WHERE tag_name = '算法与数据结构' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '动态规划', 2, tag_id, '算法与数据结构', 'DP',               20 FROM skill_tag WHERE tag_name = '算法与数据结构' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '图论',     2, tag_id, '算法与数据结构', '最短路,最小生成树', 30 FROM skill_tag WHERE tag_name = '算法与数据结构' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '竞赛算法', 2, tag_id, '算法与数据结构', 'ACM,OI,蓝桥杯',    40 FROM skill_tag WHERE tag_name = '算法与数据结构' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '数学建模', 2, tag_id, '算法与数据结构', '建模,MATLAB',       50 FROM skill_tag WHERE tag_name = '算法与数据结构' AND tag_level = 1 LIMIT 1;

-- ── 二级方向：人工智能 ─────────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '机器学习',     2, tag_id, '人工智能', 'ML,Machine Learning',      10 FROM skill_tag WHERE tag_name = '人工智能' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '深度学习',     2, tag_id, '人工智能', 'DL,Deep Learning',         20 FROM skill_tag WHERE tag_name = '人工智能' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '自然语言处理', 2, tag_id, '人工智能', 'NLP,LLM',                  30 FROM skill_tag WHERE tag_name = '人工智能' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '计算机视觉',   2, tag_id, '人工智能', 'CV,图像识别',              40 FROM skill_tag WHERE tag_name = '人工智能' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '强化学习',     2, tag_id, '人工智能', 'RL,Reinforcement Learning', 50 FROM skill_tag WHERE tag_name = '人工智能' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '大模型应用',   2, tag_id, '人工智能', 'LLM应用,RAG,Prompt',       60 FROM skill_tag WHERE tag_name = '人工智能' AND tag_level = 1 LIMIT 1;

-- ── 二级方向：数据科学 ─────────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '数据分析',   2, tag_id, '数据科学', '数据挖掘,EDA',       10 FROM skill_tag WHERE tag_name = '数据科学' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '数据可视化', 2, tag_id, '数据科学', 'Echarts,D3,BI',      20 FROM skill_tag WHERE tag_name = '数据科学' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '大数据',     2, tag_id, '数据科学', 'Spark,Hadoop,Flink', 30 FROM skill_tag WHERE tag_name = '数据科学' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '统计学',     2, tag_id, '数据科学', '概率统计,R语言',      40 FROM skill_tag WHERE tag_name = '数据科学' AND tag_level = 1 LIMIT 1;

-- ── 二级方向：产品与设计 ───────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'UI设计',   2, tag_id, '产品与设计', 'UI,界面设计,Figma',     10 FROM skill_tag WHERE tag_name = '产品与设计' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'UX设计',   2, tag_id, '产品与设计', 'UX,用户体验',           20 FROM skill_tag WHERE tag_name = '产品与设计' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '产品经理', 2, tag_id, '产品与设计', 'PM,需求分析',           30 FROM skill_tag WHERE tag_name = '产品与设计' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '视觉设计', 2, tag_id, '产品与设计', 'Photoshop,AI,平面设计', 40 FROM skill_tag WHERE tag_name = '产品与设计' AND tag_level = 1 LIMIT 1;

-- ── 二级方向：嵌入式与硬件 ────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '嵌入式系统', 2, tag_id, '嵌入式与硬件', '单片机,STM32,Arduino', 10 FROM skill_tag WHERE tag_name = '嵌入式与硬件' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '物联网',     2, tag_id, '嵌入式与硬件', 'IoT,MQTT',             20 FROM skill_tag WHERE tag_name = '嵌入式与硬件' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'FPGA',       2, tag_id, '嵌入式与硬件', 'Verilog,VHDL',         30 FROM skill_tag WHERE tag_name = '嵌入式与硬件' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '机器人',     2, tag_id, '嵌入式与硬件', 'ROS,机械臂',           40 FROM skill_tag WHERE tag_name = '嵌入式与硬件' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '无人机',     2, tag_id, '嵌入式与硬件', 'UAV,飞控',             50 FROM skill_tag WHERE tag_name = '嵌入式与硬件' AND tag_level = 1 LIMIT 1;

-- ── 二级方向：网络安全 ─────────────────────────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '渗透测试', 2, tag_id, '网络安全', 'Penetration,CTF',   10 FROM skill_tag WHERE tag_name = '网络安全' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '漏洞挖掘', 2, tag_id, '网络安全', '漏洞研究,逆向',      20 FROM skill_tag WHERE tag_name = '网络安全' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Web安全',  2, tag_id, '网络安全', 'SQL注入,XSS,OWASP', 30 FROM skill_tag WHERE tag_name = '网络安全' AND tag_level = 1 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT '密码学',   2, tag_id, '网络安全', 'Cryptography',       40 FROM skill_tag WHERE tag_name = '网络安全' AND tag_level = 1 LIMIT 1;

-- ── 三级技术点（全部用子查询动态定位父级）─────────────────
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Vue 3 Composition API', 3, tag_id, '前端开发', 'Composition API', 10 FROM skill_tag WHERE tag_name = 'Vue' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Vuex / Pinia', 3, tag_id, '前端开发', 'Pinia,状态管理', 20 FROM skill_tag WHERE tag_name = 'Vue' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'React Hooks', 3, tag_id, '前端开发', 'useState,useEffect', 10 FROM skill_tag WHERE tag_name = 'React' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'PyTorch', 3, tag_id, '人工智能', 'Torch', 10 FROM skill_tag WHERE tag_name = '深度学习' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'TensorFlow', 3, tag_id, '人工智能', 'TF,Keras', 20 FROM skill_tag WHERE tag_name = '深度学习' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Spring Boot', 3, tag_id, '后端开发', 'SpringBoot', 10 FROM skill_tag WHERE tag_name = 'Spring' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'MySQL', 3, tag_id, '后端开发', 'MariaDB', 10 FROM skill_tag WHERE tag_name = '数据库' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag (tag_name, tag_level, parent_tag_id, tag_category, tag_aliases, sort_order)
SELECT 'Redis', 3, tag_id, '后端开发', '缓存', 20 FROM skill_tag WHERE tag_name = '数据库' AND tag_level = 2 LIMIT 1;

-- ── 同义词种子（skill_tag_synonym）────────────────────────
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'vue.js',     'exact' FROM skill_tag WHERE tag_name = 'Vue'          AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'vuejs',      'exact' FROM skill_tag WHERE tag_name = 'Vue'          AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'react.js',   'exact' FROM skill_tag WHERE tag_name = 'React'        AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'reactjs',    'exact' FROM skill_tag WHERE tag_name = 'React'        AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'pytorch',    'exact' FROM skill_tag WHERE tag_name = 'PyTorch'      AND tag_level = 3 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'tensorflow', 'exact' FROM skill_tag WHERE tag_name = 'TensorFlow'   AND tag_level = 3 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'nlp',        'exact' FROM skill_tag WHERE tag_name = '自然语言处理' AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'cv',         'exact' FROM skill_tag WHERE tag_name = '计算机视觉'   AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'golang',     'exact' FROM skill_tag WHERE tag_name = 'Go'           AND tag_level = 2 LIMIT 1;
INSERT INTO skill_tag_synonym (tag_id, synonym_text, match_type)
SELECT tag_id, 'springboot', 'exact' FROM skill_tag WHERE tag_name = 'Spring Boot'  AND tag_level = 3 LIMIT 1;


-- ============================================================
-- 五、user_unavailable_date 测试数据
-- 依赖 test_value.sql 中的 user_id 1~10，请在其之后执行
-- ============================================================

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
                                                                              (1, '2026-06-15', '2026-06-28', '期末考试周'),
                                                                              (1, '2026-07-01', '2026-07-14', '暑假返乡');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
                                                                              (2, '2026-11-01', '2026-12-25', '考研备考'),
                                                                              (2, '2026-12-26', '2026-12-27', '考研考试');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
                                                                              (3, '2026-06-20', '2026-06-22', '毕业答辩'),
                                                                              (3, '2026-01-20', '2026-02-20', '寒假');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
    (4, '2026-07-01', '2026-08-31', '暑期实习');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
                                                                              (5, '2026-04-20', '2026-04-25', '期中考试周'),
                                                                              (5, '2026-06-18', '2026-06-30', '期末考试周');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
    (6, '2026-05-10', '2026-05-14', '学术会议');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
    (7, '2026-01-25', '2026-02-15', '寒假及春节');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
    (8, '2026-09-01', '2026-09-20', '军训');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
    (9, '2026-08-15', '2026-09-10', '海外交流项目');

INSERT INTO user_unavailable_date (user_id, start_date, end_date, reason) VALUES
                                                                              (10, '2026-06-15', '2026-06-28', '期末考试周'),
                                                                              (10, '2026-09-05', '2026-09-06', '软考考试');