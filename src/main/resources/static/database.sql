-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS competition_member;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS competition;
DROP TABLE IF EXISTS user;

-- 用户表（user）
CREATE TABLE user (
    user_id INT AUTO_INCREMENT PRIMARY KEY, -- 用户ID，主键，自增
    user_name VARCHAR(18) NOT NULL, -- 用户名，支持中文字符（6个中文字符约18字节）
    user_email VARCHAR(255) NOT NULL UNIQUE, -- 用户邮箱，非空，唯一键
    user_password VARCHAR(255) NOT NULL, -- 用户密码，非空
    user_gender ENUM('unknown', 'male', 'female') DEFAULT 'unknown', -- 用户性别，枚举值：unknown/male/female
    user_registration_time DATETIME DEFAULT CURRENT_TIMESTAMP, -- 用户注册时间，非空
    user_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 用户信息更新时间，自动更新
    user_university VARCHAR(100), -- 用户所在学校，支持中文字符
    user_major VARCHAR(100), -- 用户专业，支持中文字符
    user_information TEXT -- 用户信息，长文本，支持100个中文字符以上
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 竞赛表（competition）
CREATE TABLE competition (
    competition_id INT AUTO_INCREMENT PRIMARY KEY, -- 竞赛ID，主键，自增
    title VARCHAR(100) NOT NULL, -- 竞赛标题，支持中文字符，非空
    tag1 VARCHAR(15), -- 标签1，较短文本，不超过5个中文字符
    tag2 VARCHAR(15), -- 标签2，较短文本，不超过5个中文字符
    tag3 VARCHAR(15), -- 标签3，较短文本，不超过5个中文字符
    tag4 VARCHAR(15), -- 标签4，较短文本，不超过5个中文字符
    tag5 VARCHAR(15), -- 标签5，较短文本，不超过5个中文字符
    competition_details TEXT, -- 竞赛详情，长文本，支持中文
    max_participants INT NOT NULL, -- 最大参与人数，非空
    user_id INT NOT NULL, -- 创建者ID，外键，关联user表
    school_requirements TEXT, -- 学校要求，文本，默认为NULL
    deadline DATETIME, -- 截止时间
    competition_created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动生成
    competition_updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE -- 外键约束，级联删除和更新
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 竞赛成员表（competition_member）
CREATE TABLE competition_member (
    competition_id INT NOT NULL, -- 竞赛ID，外键，关联competition表
    user_id INT NOT NULL, -- 用户ID，外键，关联user表
    admission_status BOOLEAN DEFAULT FALSE, -- 录取状态，布尔型，默认未录取
    competition_member_created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动生成
    competition_member_updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
    PRIMARY KEY (competition_id, user_id), -- 联合主键
    FOREIGN KEY (competition_id) REFERENCES competition(competition_id) ON DELETE CASCADE ON UPDATE CASCADE, -- 外键约束，级联删除和更新
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE -- 外键约束，级联删除和更新
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表（message）
CREATE TABLE message (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL ,
    competition_id INT ,
    message_type ENUM(   
    'APPLICATION_SUBMITTED',  -- 报名提交
    'APPLICATION_APPROVED',   -- 报名通过
    'APPLICATION_REJECTED',   -- 报名被拒
    'APPLICATION_CANCELLED',  -- 报名取消
    'TEAM_QUIT',             -- 退出队伍
    'TEAM_KICKED',           -- 被踢出队伍
    'TEAM_DISBANDED'         -- 队伍解散
    ) NOT NULL ,
    message_content TEXT,
    is_read BOOLEAN DEFAULT FALSE ,
    message_created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (competition_id) REFERENCES competition(competition_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX idx_user_email ON user(user_email);

ALTER TABLE user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE competition CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE competition_member CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

