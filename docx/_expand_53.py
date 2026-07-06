# -*- coding: utf-8 -*-
"""
扩展系统设计说明书5.3节（模块分解描述）的功能子节
从备份文件恢复，确保幂等性
"""

import shutil
from pathlib import Path
from docx import Document
from docx.shared import Pt, Inches, Cm
from docx.enum.text import WD_PARAGRAPH_ALIGNMENT
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

# 文件路径（根据实际目录调整）
BASE_DIR = Path(r"D:\Project\anti_fraud_platform\docx\《项目阶段二》评审")
BACKUP_FILE = BASE_DIR / "专业综合课程设计-系统设计说明书-江乐霖-23201317-v2-backup.docx"
TARGET_FILE = BASE_DIR / "专业综合课程设计-系统设计说明书-江乐霖-23201317.docx"
# 临时输出文件（避免权限问题）
OUTPUT_FILE = BASE_DIR / "专业综合课程设计-系统设计说明书-江乐霖-23201317-expanded.docx"

# 功能模块定义 - 包含所有需要添加的功能子节
MODULES = {
    "5.3.1 用户与认证模块": {
        "existing_subsections": ["5.3.1.1", "5.3.1.2"],  # 已存在的子节
        "functions": [
            {
                "id": "5.3.1.3",
                "name": "用户信息查询",
                "desc": """本功能用于获取当前登录用户的详细信息。前端通过GET /api/user/info接口发起请求，后端从JWT Token中解析用户ID，查询sys_user表获取用户基本信息，同时关联user_score表获取积分等级信息，关联user_profile表获取用户画像数据，最终组装成完整的用户信息返回。该接口支持鉴权校验，确保用户只能查询自己的信息。返回数据包括用户ID、用户名、昵称、头像、角色、学号、院系、专业、年级、积分、等级等完整信息。""",
                "classes": [
                    ("UserController", "用户控制器，处理用户相关请求，提供/api/user/info接口"),
                    ("UserService", "用户服务接口，定义用户信息查询等业务方法"),
                    ("UserServiceImpl", "用户服务实现类，实现用户信息查询的具体业务逻辑"),
                    ("UserMapper", "用户数据访问接口，继承MyBatis-Plus BaseMapper"),
                    ("UserVO", "用户视图对象，封装用户信息响应数据")
                ],
                "files": [
                    ("UserController.java", "Java", "backend/src/main/java/com/anti/controller/", "用户控制器"),
                    ("UserService.java", "Java", "backend/src/main/java/com/anti/service/", "用户服务接口"),
                    ("UserServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "用户服务实现"),
                    ("UserMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "用户数据访问"),
                    ("UserVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "用户视图对象")
                ]
            },
            {
                "id": "5.3.1.4",
                "name": "用户资料更新",
                "desc": """本功能用于用户更新个人资料信息。用户通过PUT /api/user/update接口提交需要修改的字段，包括昵称、头像URL、学号、院系、专业、年级等。后端校验用户权限，确保用户只能修改自己的资料。系统对学号进行唯一性校验，避免与其他用户冲突。更新操作同步触发用户画像的更新，如果用户院系或专业发生变化，系统会更新user_profile表中的相关属性。更新完成后清除相关的缓存数据。""",
                "classes": [
                    ("UserController", "用户控制器，提供/api/user/update接口"),
                    ("UserService", "用户服务接口，定义用户资料更新方法"),
                    ("UserServiceImpl", "用户服务实现类，实现资料更新逻辑，包含唯一性校验"),
                    ("UserMapper", "用户数据访问接口，执行用户信息更新操作"),
                    ("UserUpdateDTO", "用户更新数据传输对象，封装更新请求参数")
                ],
                "files": [
                    ("UserController.java", "Java", "backend/src/main/java/com/anti/controller/", "用户控制器"),
                    ("UserServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "用户服务实现"),
                    ("UserMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "用户数据访问"),
                    ("UserUpdateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "更新请求DTO"),
                    ("UserProfile.java", "Java", "backend/src/main/java/com/anti/entity/", "用户画像实体")
                ]
            },
            {
                "id": "5.3.1.5",
                "name": "密码修改",
                "desc": """本功能用于用户修改登录密码。用户通过PUT /api/user/password接口提交旧密码和新密码。后端首先从JWT Token中获取当前用户ID，查询用户记录，使用BCrypt密码编码器验证旧密码是否正确。验证通过后，对新密码进行BCrypt加密存储。密码修改成功后，系统会清除该用户的所有登录会话（将当前JWT加入黑名单），用户需要重新登录。该接口对密码强度有基本要求，新密码不能与旧密码相同。""",
                "classes": [
                    ("UserController", "用户控制器，提供/api/user/password接口"),
                    ("UserService", "用户服务接口，定义密码修改方法"),
                    ("UserServiceImpl", "用户服务实现类，实现密码验证和更新逻辑"),
                    ("BCryptPasswordEncoder", "Spring Security提供的密码编码器，用于密码加密和验证"),
                    ("PasswordChangeDTO", "密码修改数据传输对象，封装新旧密码")
                ],
                "files": [
                    ("UserController.java", "Java", "backend/src/main/java/com/anti/controller/", "用户控制器"),
                    ("UserServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "用户服务实现"),
                    ("PasswordConfig.java", "Java", "backend/src/main/java/com/anti/config/", "密码编码器配置"),
                    ("PasswordChangeDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "密码修改DTO"),
                    ("JwtUtils.java", "Java", "backend/src/main/java/com/anti/security/", "JWT工具类")
                ]
            },
            {
                "id": "5.3.1.6",
                "name": "用户登出",
                "desc": """本功能用于用户安全退出登录。用户通过POST /api/user/logout接口发起登出请求。后端从请求头的Authorization中提取JWT Token，计算Token的剩余有效期，然后将Token添加到Redis黑名单中（键名为jwt:blacklist:{token}），设置过期时间为Token的剩余有效期。这样即使Token未过期，也无法再次使用。该机制确保了用户登出的安全性，防止Token被盗用。登出成功后返回成功响应，前端清除本地存储的Token信息。""",
                "classes": [
                    ("UserController", "用户控制器，提供/api/user/logout接口"),
                    ("UserService", "用户服务接口，定义登出方法"),
                    ("UserServiceImpl", "用户服务实现类，实现Token黑名单逻辑"),
                    ("JwtUtils", "JWT工具类，解析Token获取过期时间"),
                    ("RedisCacheUtil", "Redis缓存工具类，管理Token黑名单")
                ],
                "files": [
                    ("UserController.java", "Java", "backend/src/main/java/com/anti/controller/", "用户控制器"),
                    ("UserServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "用户服务实现"),
                    ("JwtUtils.java", "Java", "backend/src/main/java/com/anti/security/", "JWT工具类"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "Redis缓存工具"),
                    ("CacheConstants.java", "Java", "backend/src/main/java/com/anti/common/", "缓存常量定义")
                ]
            },
            {
                "id": "5.3.1.7",
                "name": "用户管理",
                "desc": """本功能为管理员提供用户管理能力，包括用户列表查询、用户详情查看、用户启用/禁用操作。管理员通过GET /api/user/list接口获取分页用户列表，支持按用户名、学号、角色、状态等条件筛选。GET /api/user/{id}接口获取指定用户的详细信息。PUT /api/user/{id}/enable和PUT /api/user/{id}/disable接口用于启用或禁用用户账号，禁用后的用户无法登录系统。所有用户管理接口都需要ADMIN角色权限，通过Spring Security的@PreAuthorize注解进行权限控制。系统记录用户的最后登录时间、登录IP等信息，便于管理员审计。""",
                "classes": [
                    ("UserController", "用户控制器，提供用户管理的所有接口"),
                    ("UserService", "用户服务接口，定义用户管理相关方法"),
                    ("UserServiceImpl", "用户服务实现类，实现用户列表查询、详情查询、状态更新等逻辑"),
                    ("UserMapper", "用户数据访问接口，包含复杂查询的XML映射"),
                    ("SecurityConfig", "安全配置类，配置ADMIN角色权限")
                ],
                "files": [
                    ("UserController.java", "Java", "backend/src/main/java/com/anti/controller/", "用户控制器"),
                    ("UserServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "用户服务实现"),
                    ("UserMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "用户数据访问"),
                    ("UserMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件"),
                    ("SecurityConfig.java", "Java", "backend/src/main/java/com/anti/config/", "安全配置")
                ]
            }
        ]
    },
    "5.3.2 资讯学习模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.2.1",
                "name": "资讯分页列表",
                "desc": """本功能用于获取资讯列表的分页数据。用户通过GET /api/news/page接口请求，支持按分类ID（categoryId）、资讯类型（newsType）、关键词（keyword）进行筛选。系统默认按置顶优先、发布时间倒序排列。返回的每条资讯包含ID、标题、摘要、封面图、浏览量、点赞数、发布时间等基本信息。列表数据会填充isLiked字段表示当前用户是否已点赞。热点资讯数据会进行Redis缓存，缓存键为cache:hot_news:，有效期300秒，减少数据库访问压力。分页参数包括当前页码和每页条数，默认每页10条。""",
                "classes": [
                    ("NewsController", "资讯控制器，提供/api/news/page接口"),
                    ("NewsService", "资讯服务接口，定义分页查询方法"),
                    ("NewsServiceImpl", "资讯服务实现类，实现分页查询和缓存逻辑"),
                    ("NewsMapper", "资讯数据访问接口，继承MyBatis-Plus BaseMapper"),
                    ("NewsVO", "资讯视图对象，封装列表响应数据")
                ],
                "files": [
                    ("NewsController.java", "Java", "backend/src/main/java/com/anti/controller/", "资讯控制器"),
                    ("NewsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "资讯服务实现"),
                    ("NewsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "资讯数据访问"),
                    ("NewsVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "资讯视图对象"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "Redis缓存工具")
                ]
            },
            {
                "id": "5.3.2.2",
                "name": "资讯详情查看",
                "desc": """本功能用于查看单条资讯的详细内容。用户通过GET /api/news/{id}接口请求，系统查询news表获取资讯完整信息，包括标题、内容（富文本）、分类、类型、作者、发布时间、浏览量、点赞数等。查询成功后自动将浏览量加1。系统会查询当前用户是否已点赞该资讯，填充isLiked字段。详情数据会进行Redis缓存，键为cache:news:detail:{id}，有效期600秒。如果是必读资讯，系统会记录用户的阅读状态。返回的资讯内容为富文本格式，前端使用富文本渲染器展示。""",
                "classes": [
                    ("NewsController", "资讯控制器，提供/api/news/{id}接口"),
                    ("NewsService", "资讯服务接口，定义详情查询方法"),
                    ("NewsServiceImpl", "资讯服务实现类，实现详情查询和浏览量更新"),
                    ("NewsMapper", "资讯数据访问接口"),
                    ("News", "资讯实体类，映射news表")
                ],
                "files": [
                    ("NewsController.java", "Java", "backend/src/main/java/com/anti/controller/", "资讯控制器"),
                    ("NewsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "资讯服务实现"),
                    ("News.java", "Java", "backend/src/main/java/com/anti/entity/", "资讯实体"),
                    ("NewsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "资讯数据访问"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "缓存工具")
                ]
            },
            {
                "id": "5.3.2.3",
                "name": "资讯发布",
                "desc": """本功能为管理员提供资讯发布能力。管理员通过POST /api/news接口提交资讯数据，包括标题、内容、分类ID、资讯类型（news/warning/policy）、封面图URL等。系统校验必填字段，自动设置发布时间、作者（当前管理员）、初始状态。如果设置isTop为true，该资讯会在列表中优先显示。如果设置isMandatory为true，该资讯会标记为必读资讯。发布成功后，系统会清除资讯列表缓存和热点资讯缓存，确保用户能立即看到新发布的资讯。需要ADMIN角色权限。""",
                "classes": [
                    ("NewsController", "资讯控制器，提供POST /api/news接口"),
                    ("NewsService", "资讯服务接口，定义资讯创建方法"),
                    ("NewsServiceImpl", "资讯服务实现类，实现资讯创建和缓存清除"),
                    ("News", "资讯实体类，映射news表"),
                    ("NewsCreateDTO", "资讯创建数据传输对象，封装创建请求参数")
                ],
                "files": [
                    ("NewsController.java", "Java", "backend/src/main/java/com/anti/controller/", "资讯控制器"),
                    ("NewsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "资讯服务实现"),
                    ("News.java", "Java", "backend/src/main/java/com/anti/entity/", "资讯实体"),
                    ("NewsCreateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "资讯创建DTO"),
                    ("CacheRefreshService.java", "Java", "backend/src/main/java/com/anti/service/", "缓存刷新服务")
                ]
            },
            {
                "id": "5.3.2.4",
                "name": "资讯编辑",
                "desc": """本功能为管理员提供资讯编辑能力。管理员通过PUT /api/news/{id}接口更新资讯信息，可修改标题、内容、分类、类型、封面图、置顶状态、必读状态等。系统校验资讯是否存在，验证管理员权限。更新时会自动设置更新时间字段。如果修改了影响排序的字段（如置顶状态），会清除列表缓存。内容修改后也会清除详情缓存。编辑操作不影响已有的浏览量和点赞数据。需要ADMIN角色权限。""",
                "classes": [
                    ("NewsController", "资讯控制器，提供PUT /api/news/{id}接口"),
                    ("NewsService", "资讯服务接口，定义资讯更新方法"),
                    ("NewsServiceImpl", "资讯服务实现类，实现资讯更新和缓存失效"),
                    ("NewsMapper", "资讯数据访问接口，执行更新操作"),
                    ("NewsUpdateDTO", "资讯更新数据传输对象，封装更新请求参数")
                ],
                "files": [
                    ("NewsController.java", "Java", "backend/src/main/java/com/anti/controller/", "资讯控制器"),
                    ("NewsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "资讯服务实现"),
                    ("NewsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "资讯数据访问"),
                    ("NewsUpdateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "资讯更新DTO"),
                    ("CacheRefreshService.java", "Java", "backend/src/main/java/com/anti/service/", "缓存刷新服务")
                ]
            },
            {
                "id": "5.3.2.5",
                "name": "资讯删除",
                "desc": """本功能为管理员提供资讯删除能力。管理员通过DELETE /api/news/{id}接口删除指定资讯。系统采用逻辑删除策略，将deleted字段设置为1，而非物理删除数据。删除前会检查资讯是否存在，验证管理员权限。删除操作会级联删除相关的点赞记录（news_like表）和浏览记录（news_browse_log表）。删除后清除资讯列表缓存、详情缓存和热点缓存。需要ADMIN角色权限。""",
                "classes": [
                    ("NewsController", "资讯控制器，提供DELETE /api/news/{id}接口"),
                    ("NewsService", "资讯服务接口，定义资讯删除方法"),
                    ("NewsServiceImpl", "资讯服务实现类，实现逻辑删除和级联清理"),
                    ("NewsMapper", "资讯数据访问接口，执行逻辑删除"),
                    ("NewsLikeMapper", "资讯点赞数据访问，级联删除点赞记录")
                ],
                "files": [
                    ("NewsController.java", "Java", "backend/src/main/java/com/anti/controller/", "资讯控制器"),
                    ("NewsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "资讯服务实现"),
                    ("NewsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "资讯数据访问"),
                    ("NewsLikeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "点赞数据访问"),
                    ("CacheRefreshService.java", "Java", "backend/src/main/java/com/anti/service/", "缓存刷新服务")
                ]
            },
            {
                "id": "5.3.2.6",
                "name": "资讯浏览记录",
                "desc": """本功能用于记录用户浏览资讯的行为数据。用户通过POST /api/news/{id}/browse接口提交浏览记录，包含停留时长（stayDuration）等信息。系统创建news_browse_log记录，关联用户ID和资讯ID。浏览记录用于统计分析用户阅读偏好，并触发游戏化系统：首次浏览奖励积分、更新日榜/周榜/总榜、检查浏览成就、更新连续学习天数、更新用户画像中的知识水平。这些副作用操作用try/catch包裹，不影响主流程。浏览数据也用于个性化推荐算法的训练。""",
                "classes": [
                    ("NewsController", "资讯控制器，提供/api/news/{id}/browse接口"),
                    ("NewsService", "资讯服务接口，定义浏览记录方法"),
                    ("NewsServiceImpl", "资讯服务实现类，实现浏览记录和游戏化触发"),
                    ("NewsBrowseLog", "资讯浏览日志实体，映射news_browse_log表"),
                    ("NewsBrowseLogMapper", "浏览日志数据访问接口")
                ],
                "files": [
                    ("NewsController.java", "Java", "backend/src/main/java/com/anti/controller/", "资讯控制器"),
                    ("NewsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "资讯服务实现"),
                    ("NewsBrowseLog.java", "Java", "backend/src/main/java/com/anti/entity/", "浏览日志实体"),
                    ("NewsBrowseLogMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "浏览日志数据访问"),
                    ("ScoreService.java", "Java", "backend/src/main/java/com/anti/service/", "积分服务")
                ]
            },
            {
                "id": "5.3.2.7",
                "name": "资讯点赞",
                "desc": """本功能用于用户对资讯进行点赞或取消点赞操作。用户通过POST /api/news/{id}/like接口进行点赞，通过DELETE /api/news/{id}/like取消点赞。系统在news_like表中创建或删除记录，同时更新news表中的like_count字段。点赞操作具有幂等性，重复点赞不会创建重复记录。点赞数据会影响资讯的热度排序，用于Wilson置信区间计算点赞率。点赞行为也被记录到用户行为矩阵，用于个性化推荐。取消点赞后相关数据同步更新。""",
                "classes": [
                    ("NewsController", "资讯控制器，提供点赞/取消点赞接口"),
                    ("NewsService", "资讯服务接口，定义点赞方法"),
                    ("NewsServiceImpl", "资讯服务实现类，实现点赞逻辑和计数更新"),
                    ("NewsLike", "资讯点赞实体，映射news_like表"),
                    ("NewsLikeMapper", "点赞数据访问接口")
                ],
                "files": [
                    ("NewsController.java", "Java", "backend/src/main/java/com/anti/controller/", "资讯控制器"),
                    ("NewsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "资讯服务实现"),
                    ("NewsLike.java", "Java", "backend/src/main/java/com/anti/entity/", "点赞实体"),
                    ("NewsLikeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "点赞数据访问"),
                    ("NewsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "资讯数据访问")
                ]
            }
        ]
    },
    "5.3.3 案例展示模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.3.1",
                "name": "案例分页列表",
                "desc": """本功能用于获取诈骗案例列表的分页数据。用户通过GET /api/case/page接口请求，支持按标签ID（tagId）、关键词（keyword）进行筛选。系统默认按精选优先、Wilson分降序、发布时间倒序排列。返回的每条案例包含ID、标题、摘要、封面图、诈骗类型、风险等级、难度、浏览量、点赞数、Wilson分等。列表数据会填充isLiked字段表示当前用户是否已点赞。热点案例数据会进行Redis缓存，键为cache:hot_case:，有效期300秒。Wilson分使用z=1.645的置信区间下界算法，避免样本量差异导致的排序偏差。""",
                "classes": [
                    ("CaseController", "案例控制器，提供/api/case/page接口"),
                    ("FraudCaseService", "案例服务接口，定义分页查询方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现分页查询和Wilson分计算"),
                    ("FraudCaseMapper", "案例数据访问接口"),
                    ("CaseVO", "案例视图对象，封装列表响应数据")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("FraudCaseMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "案例数据访问"),
                    ("CaseVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "案例视图对象"),
                    ("RecommendationAlgorithmUtil.java", "Java", "backend/src/main/java/com/anti/util/", "Wilson分计算工具")
                ]
            },
            {
                "id": "5.3.3.2",
                "name": "案例详情查看",
                "desc": """本功能用于查看单个诈骗案例的详细内容。用户通过GET /api/case/{id}接口请求，系统查询fraud_case表获取案例完整信息，包括标题、详细描述、诈骗手法、防范建议、案例脚本（JSON格式的决策树）、目标年级、目标专业、风险等级、难度等。系统计算风险等级名称和难度名称，判断案例是否匹配当前用户（目标年级/专业）。详情数据会进行Redis缓存，键为cache:case:detail:{id}，有效期600秒。返回数据包含isLiked和isMatched字段。""",
                "classes": [
                    ("CaseController", "案例控制器，提供/api/case/{id}接口"),
                    ("FraudCaseService", "案例服务接口，定义详情查询方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现详情查询和匹配判断"),
                    ("FraudCase", "案例实体类，映射fraud_case表，含JSON类型的scripts字段"),
                    ("FraudCaseMapper", "案例数据访问接口")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("FraudCase.java", "Java", "backend/src/main/java/com/anti/entity/", "案例实体"),
                    ("FraudCaseMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "案例数据访问"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "缓存工具")
                ]
            },
            {
                "id": "5.3.3.3",
                "name": "案例发布",
                "desc": """本功能为管理员提供案例发布能力。管理员通过POST /api/case接口提交案例数据，包括标题、描述、诈骗手法、防范建议、标签ID列表、目标年级、目标专业、风险等级、难度等。系统校验必填字段，处理标签关联（case_tag_relation表），自动计算初始Wilson分和点赞率。如果设置isFeatured为true，该案例会在列表中优先显示。发布成功后清除案例列表缓存和热点缓存。需要ADMIN角色权限。""",
                "classes": [
                    ("CaseController", "案例控制器，提供POST /api/case接口"),
                    ("FraudCaseService", "案例服务接口，定义案例创建方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现案例创建和标签关联"),
                    ("FraudCase", "案例实体类"),
                    ("CaseCreateDTO", "案例创建数据传输对象")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("FraudCase.java", "Java", "backend/src/main/java/com/anti/entity/", "案例实体"),
                    ("CaseCreateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "案例创建DTO"),
                    ("CaseTagRelationMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "标签关联数据访问")
                ]
            },
            {
                "id": "5.3.3.4",
                "name": "案例编辑",
                "desc": """本功能为管理员提供案例编辑能力。管理员通过PUT /api/case/{id}接口更新案例信息，可修改标题、描述、诈骗手法、防范建议、标签、目标人群、风险等级、难度、精选状态等。系统校验案例是否存在，验证管理员权限。更新时自动设置更新时间，重新计算Wilson分。如果修改了标签，会先删除旧的标签关联，再插入新的关联记录。修改后清除详情缓存和列表缓存。需要ADMIN角色权限。""",
                "classes": [
                    ("CaseController", "案例控制器，提供PUT /api/case/{id}接口"),
                    ("FraudCaseService", "案例服务接口，定义案例更新方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现案例更新和标签重关联"),
                    ("FraudCaseMapper", "案例数据访问接口"),
                    ("CaseUpdateDTO", "案例更新数据传输对象")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("FraudCaseMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "案例数据访问"),
                    ("CaseUpdateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "案例更新DTO"),
                    ("CaseTagRelationMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "标签关联数据访问")
                ]
            },
            {
                "id": "5.3.3.5",
                "name": "案例删除",
                "desc": """本功能为管理员提供案例删除能力。管理员通过DELETE /api/case/{id}接口删除指定案例。系统采用逻辑删除策略，将deleted字段设置为1。删除前会检查案例是否存在，验证管理员权限。删除操作会级联删除相关的标签关联（case_tag_relation）、点赞记录（case_like）、浏览记录（case_browse_log）。删除后清除案例列表缓存、详情缓存和热点缓存。需要ADMIN角色权限。""",
                "classes": [
                    ("CaseController", "案例控制器，提供DELETE /api/case/{id}接口"),
                    ("FraudCaseService", "案例服务接口，定义案例删除方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现逻辑删除和级联清理"),
                    ("FraudCaseMapper", "案例数据访问接口"),
                    ("CaseLikeMapper", "案例点赞数据访问接口")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("FraudCaseMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "案例数据访问"),
                    ("CaseLikeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "点赞数据访问"),
                    ("CaseBrowseLogMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "浏览日志数据访问")
                ]
            },
            {
                "id": "5.3.3.6",
                "name": "案例浏览记录",
                "desc": """本功能用于记录用户浏览案例的行为数据。用户通过POST /api/case/{id}/browse接口提交浏览记录，包含停留时长（stayDuration）。系统创建case_browse_log记录，关联用户ID和案例ID。浏览行为触发游戏化系统：首次浏览奖励+2积分、更新日榜/周榜/总榜、检查浏览成就（browse_count）、更新连续学习天数、按停留时长和案例难度更新用户知识水平。副作用用try/catch包裹。浏览数据用于用户行为画像和推荐算法。""",
                "classes": [
                    ("CaseController", "案例控制器，提供/api/case/{id}/browse接口"),
                    ("FraudCaseService", "案例服务接口，定义浏览记录方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现浏览记录和游戏化触发"),
                    ("CaseBrowseLog", "案例浏览日志实体，映射case_browse_log表"),
                    ("CaseBrowseLogMapper", "浏览日志数据访问接口")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("CaseBrowseLog.java", "Java", "backend/src/main/java/com/anti/entity/", "浏览日志实体"),
                    ("CaseBrowseLogMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "浏览日志数据访问"),
                    ("ScoreServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "积分服务实现")
                ]
            },
            {
                "id": "5.3.3.7",
                "name": "案例点赞",
                "desc": """本功能用于用户对案例进行点赞或取消点赞操作。用户通过POST /api/case/{id}/like接口进行点赞，DELETE /api/case/{id}/like取消点赞。系统在case_like表中创建或删除记录，同时更新fraud_case表中的like_count字段，并重新计算Wilson分和点赞率。Wilson分使用z=1.645的置信区间下界算法，用于热门案例排序。点赞操作具有幂等性。点赞行为被记录到用户行为矩阵，用于个性化推荐。""",
                "classes": [
                    ("CaseController", "案例控制器，提供点赞/取消点赞接口"),
                    ("FraudCaseService", "案例服务接口，定义点赞方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现点赞逻辑和Wilson分更新"),
                    ("CaseLike", "案例点赞实体，映射case_like表"),
                    ("RecommendationAlgorithmUtil", "推荐算法工具类，计算Wilson分")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("CaseLike.java", "Java", "backend/src/main/java/com/anti/entity/", "点赞实体"),
                    ("CaseLikeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "点赞数据访问"),
                    ("RecommendationAlgorithmUtil.java", "Java", "backend/src/main/java/com/anti/util/", "Wilson分计算")
                ]
            },
            {
                "id": "5.3.3.8",
                "name": "热点案例推荐",
                "desc": """本功能用于获取热点诈骗案例推荐列表。用户通过GET /api/case/hot接口请求，系统根据Wilson置信区间分、浏览量、点赞数计算综合热度分，返回排名前N的案例。热点案例列表会进行Redis缓存，键为cache:hot_case:，有效期300秒。计算热度分时综合考虑Wilson分（避免样本量偏差）、浏览量权重0.3、点赞量权重0.5、评论量权重0.2。该接口无需登录即可访问，用于首页热点案例展示。""",
                "classes": [
                    ("CaseController", "案例控制器，提供/api/case/hot接口"),
                    ("FraudCaseService", "案例服务接口，定义热点案例方法"),
                    ("FraudCaseServiceImpl", "案例服务实现类，实现热度计算和缓存"),
                    ("RecommendationAlgorithmUtil", "推荐算法工具类，提供Wilson分和热度分计算"),
                    ("RedisCacheUtil", "Redis缓存工具类，管理热点案例缓存")
                ],
                "files": [
                    ("CaseController.java", "Java", "backend/src/main/java/com/anti/controller/", "案例控制器"),
                    ("FraudCaseServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "案例服务实现"),
                    ("RecommendationAlgorithmUtil.java", "Java", "backend/src/main/java/com/anti/util/", "热度分计算"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "缓存工具"),
                    ("CacheConstants.java", "Java", "backend/src/main/java/com/anti/common/", "缓存常量")
                ]
            }
        ]
    },
    "5.3.4 知识闯关与情景模拟模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.4.1",
                "name": "关卡列表查询",
                "desc": """本功能用于获取闯关关卡列表。用户通过GET /api/challenge/list接口请求，系统查询challenge表获取所有关卡信息。返回的关卡列表包含ID、标题、类型（quiz/scenario）、难度、积分奖励、关卡序号等。系统根据用户的闯关记录计算每个关卡的解锁状态和通关状态：已通关、已解锁未通关、未解锁。解锁规则为levelOrder <= maxPassedLevel + 1（顺序解锁）。用户只能挑战已解锁的关卡，未解锁的关卡显示为锁定状态。列表按关卡序号排序。""",
                "classes": [
                    ("ChallengeController", "闯关控制器，提供/api/challenge/list接口"),
                    ("ChallengeService", "闯关服务接口，定义关卡列表查询方法"),
                    ("ChallengeServiceImpl", "闯关服务实现类，实现关卡解锁逻辑"),
                    ("ChallengeMapper", "闯关数据访问接口"),
                    ("ChallengeVO", "关卡视图对象，包含解锁和通关状态")
                ],
                "files": [
                    ("ChallengeController.java", "Java", "backend/src/main/java/com/anti/controller/", "闯关控制器"),
                    ("ChallengeServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "闯关服务实现"),
                    ("ChallengeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "闯关数据访问"),
                    ("ChallengeVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "关卡视图对象"),
                    ("UserChallengeRecordMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "闯关记录数据访问")
                ]
            },
            {
                "id": "5.3.4.2",
                "name": "关卡详情查看",
                "desc": """本功能用于查看单个关卡的详细内容。用户通过GET /api/challenge/{id}接口请求，系统查询challenge表获取关卡完整信息。对于quiz类型关卡，返回题库内容（ChallengeContent，包含Question和Option，但不暴露正确答案）。对于scenario类型关卡，返回情景剧本的基本信息（起始节点、节点列表，但不暴露安全选择标记）。系统校验关卡是否已解锁，未解锁的关卡不允许查看详情。返回数据包含关卡标题、描述、类型、难度、奖励积分、题目数量等。""",
                "classes": [
                    ("ChallengeController", "闯关控制器，提供/api/challenge/{id}接口"),
                    ("ChallengeService", "闯关服务接口，定义关卡详情查询方法"),
                    ("ChallengeServiceImpl", "闯关服务实现类，实现详情查询和权限校验"),
                    ("Challenge", "关卡实体类，包含嵌套类ChallengeContent/Question/Option"),
                    ("ChallengeDetailVO", "关卡详情视图对象，过滤敏感信息")
                ],
                "files": [
                    ("ChallengeController.java", "Java", "backend/src/main/java/com/anti/controller/", "闯关控制器"),
                    ("ChallengeServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "闯关服务实现"),
                    ("Challenge.java", "Java", "backend/src/main/java/com/anti/entity/", "关卡实体"),
                    ("ChallengeDetailVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "关卡详情VO"),
                    ("ChallengeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "闯关数据访问")
                ]
            },
            {
                "id": "5.3.4.3",
                "name": "答题闯关提交",
                "desc": """本功能用于提交闯关答题结果并计算得分。用户通过POST /api/challenge/submit接口提交答案，包含关卡ID和用户选择的选项索引（correctIndexes）。系统校验关卡类型必须为quiz，校验关卡是否已解锁。根据题库的正确答案进行判分，计算正确率和得分。如果得分>=60分则视为通关，奖励关卡配置的scoreReward积分，更新日榜/周榜/总榜，检查成就（challenge_count/perfect_score/challenge_complete），按得分和难度更新用户知识水平。创建UserChallengeRecord记录答题详情（answer_detail为JSON）。""",
                "classes": [
                    ("ChallengeController", "闯关控制器，提供/api/challenge/submit接口"),
                    ("ChallengeService", "闯关服务接口，定义答题提交方法"),
                    ("ChallengeServiceImpl", "闯关服务实现类，实现判分和游戏化触发"),
                    ("ChallengeSubmitDTO", "答题提交数据传输对象"),
                    ("UserChallengeRecord", "闯关记录实体，映射user_challenge_record表")
                ],
                "files": [
                    ("ChallengeController.java", "Java", "backend/src/main/java/com/anti/controller/", "闯关控制器"),
                    ("ChallengeServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "闯关服务实现"),
                    ("ChallengeSubmitDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "答题提交DTO"),
                    ("UserChallengeRecord.java", "Java", "backend/src/main/java/com/anti/entity/", "闯关记录实体"),
                    ("UserChallengeRecordMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "闯关记录数据访问")
                ]
            },
            {
                "id": "5.3.4.4",
                "name": "闯关记录查询",
                "desc": """本功能用于查询用户的闯关历史记录。用户通过GET /api/challenge/records接口请求，系统查询user_challenge_record表获取该用户的所有闯关记录。支持按关卡ID、通关状态进行筛选，支持分页查询。返回的记录包含关卡标题、类型、难度、得分、是否通关、答题详情（JSON）、挑战时间等。系统同时返回用户的总体进度统计：总关卡数、已通关数、通关率、总积分等。记录按挑战时间倒序排列。""",
                "classes": [
                    ("ChallengeController", "闯关控制器，提供/api/challenge/records接口"),
                    ("ChallengeService", "闯关服务接口，定义记录查询方法"),
                    ("ChallengeServiceImpl", "闯关服务实现类，实现记录查询和统计"),
                    ("UserChallengeRecordMapper", "闯关记录数据访问接口，包含复杂查询"),
                    ("ChallengeRecordVO", "闯关记录视图对象")
                ],
                "files": [
                    ("ChallengeController.java", "Java", "backend/src/main/java/com/anti/controller/", "闯关控制器"),
                    ("ChallengeServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "闯关服务实现"),
                    ("UserChallengeRecordMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "闯关记录数据访问"),
                    ("UserChallengeRecordMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件"),
                    ("ChallengeRecordVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "记录视图对象")
                ]
            },
            {
                "id": "5.3.4.5",
                "name": "情景模拟开始",
                "desc": """本功能用于开始情景模拟闯关。用户通过POST /api/scenario/start/{challengeId}接口请求，系统查询challenge表获取情景剧本（ScenarioScript，包含节点和边）。创建scenario_progress记录，初始化当前节点为startNodeId，状态为in_progress，决策历史为空。系统校验关卡类型必须为scenario，校验关卡是否已解锁。如果已有进行中的进度，则返回现有进度而非创建新记录。返回初始节点信息（类型、角色、对话内容、可选项），不暴露isSafeChoice标记。""",
                "classes": [
                    ("ScenarioController", "情景控制器，提供/api/scenario/start/{challengeId}接口"),
                    ("ScenarioService", "情景服务接口，定义开始情景方法"),
                    ("ScenarioServiceImpl", "情景服务实现类，实现情景初始化"),
                    ("ScenarioProgress", "情景进度实体，映射scenario_progress表"),
                    ("Challenge", "关卡实体类，包含嵌套类ScenarioScript/ScenarioNode/ScenarioEdge")
                ],
                "files": [
                    ("ScenarioController.java", "Java", "backend/src/main/java/com/anti/controller/", "情景控制器"),
                    ("ScenarioServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "情景服务实现"),
                    ("ScenarioProgress.java", "Java", "backend/src/main/java/com/anti/entity/", "情景进度实体"),
                    ("Challenge.java", "Java", "backend/src/main/java/com/anti/entity/", "关卡实体"),
                    ("ScenarioProgressMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "情景进度数据访问")
                ]
            },
            {
                "id": "5.3.4.6",
                "name": "情景决策推进",
                "desc": """本功能用于在情景模拟中做出决策并推进剧情。用户通过POST /api/scenario/decision接口提交决策，包含关卡ID、当前节点ID、选择的边ID。系统验证进度状态必须为in_progress，验证当前节点与用户提交的一致。根据ScenarioScript的边信息找到匹配的边，创建DecisionRecord记录（包含isSafeChoice标记）。推进到下一个节点，如果下一个节点在endNodeIds中则标记状态为completed或failed（根据安全选择比例），计算finalScore = 100 * 安全选择数 / 总选择数，>=60分通关并奖励积分（默认20分）。更新排行榜、检查成就、更新知识水平。返回新节点信息或结局信息。""",
                "classes": [
                    ("ScenarioController", "情景控制器，提供/api/scenario/decision接口"),
                    ("ScenarioService", "情景服务接口，定义决策推进方法"),
                    ("ScenarioServiceImpl", "情景服务实现类，实现FSM状态机逻辑"),
                    ("ScenarioDecisionDTO", "情景决策数据传输对象"),
                    ("DecisionRecord", "决策记录，嵌入ScenarioProgress的JSON字段")
                ],
                "files": [
                    ("ScenarioController.java", "Java", "backend/src/main/java/com/anti/controller/", "情景控制器"),
                    ("ScenarioServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "情景服务实现"),
                    ("ScenarioDecisionDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "决策DTO"),
                    ("ScenarioProgress.java", "Java", "backend/src/main/java/com/anti/entity/", "情景进度实体"),
                    ("ScoreServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "积分服务")
                ]
            },
            {
                "id": "5.3.4.7",
                "name": "情景进度查询",
                "desc": """本功能用于查询用户当前情景模拟的进度。用户通过GET /api/scenario/progress/{challengeId}接口请求，系统查询scenario_progress表获取该用户对指定关卡的进度记录。返回数据包含当前节点ID、节点类型、节点对话内容、可选项列表、决策历史、状态（in_progress/completed/failed）、最终得分（如果已结束）。如果用户未开始该情景，返回null或初始状态。该接口用于用户重新进入情景时恢复进度，或在结束后查看详细决策路径。""",
                "classes": [
                    ("ScenarioController", "情景控制器，提供/api/scenario/progress/{challengeId}接口"),
                    ("ScenarioService", "情景服务接口，定义进度查询方法"),
                    ("ScenarioServiceImpl", "情景服务实现类，实现进度查询"),
                    ("ScenarioProgressMapper", "情景进度数据访问接口"),
                    ("ScenarioProgressVO", "情景进度视图对象")
                ],
                "files": [
                    ("ScenarioController.java", "Java", "backend/src/main/java/com/anti/controller/", "情景控制器"),
                    ("ScenarioServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "情景服务实现"),
                    ("ScenarioProgressMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "情景进度数据访问"),
                    ("ScenarioProgressVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "进度视图对象"),
                    ("Challenge.java", "Java", "backend/src/main/java/com/anti/entity/", "关卡实体")
                ]
            }
        ]
    },
    "5.3.5 社区互动模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.5.1",
                "name": "帖子分页列表",
                "desc": """本功能用于获取社区帖子的分页列表。用户通过GET /api/forum/post/page接口请求，支持按帖子类型（postType: experience/question/discussion）、排序方式（sortBy: time/like/comment）、关键词进行筛选。默认按时间倒序排列。返回的每条帖子包含ID、标题、内容摘要、作者信息、帖子类型、浏览量、点赞数、评论数、是否置顶、创建时间等。列表数据会填充isLiked字段表示当前用户是否已点赞。置顶帖子会在列表顶部优先显示。分页参数包括当前页码和每页条数，默认每页10条。""",
                "classes": [
                    ("ForumController", "社区控制器，提供/api/forum/post/page接口"),
                    ("ForumPostService", "帖子服务接口，定义分页查询方法"),
                    ("ForumPostServiceImpl", "帖子服务实现类，实现分页查询"),
                    ("ForumPostMapper", "帖子数据访问接口"),
                    ("ForumPostVO", "帖子视图对象")
                ],
                "files": [
                    ("ForumController.java", "Java", "backend/src/main/java/com/anti/controller/", "社区控制器"),
                    ("ForumPostServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "帖子服务实现"),
                    ("ForumPostMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "帖子数据访问"),
                    ("ForumPostVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "帖子视图对象"),
                    ("ForumPostMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件")
                ]
            },
            {
                "id": "5.3.5.2",
                "name": "帖子详情查看",
                "desc": """本功能用于查看单个社区帖子的详细内容。用户通过GET /api/forum/post/{postId}接口请求，系统查询forum_post表获取帖子完整信息，包括标题、内容（富文本）、作者信息、帖子类型、浏览量、点赞数、评论数、是否置顶、是否精选等。查询成功后自动将浏览量加1。系统会查询当前用户是否已点赞该帖子，填充isLiked字段。返回数据还包含作者的基本信息（昵称、头像）。帖子的评论需要通过单独的接口获取。""",
                "classes": [
                    ("ForumController", "社区控制器，提供/api/forum/post/{postId}接口"),
                    ("ForumPostService", "帖子服务接口，定义详情查询方法"),
                    ("ForumPostServiceImpl", "帖子服务实现类，实现详情查询和浏览量更新"),
                    ("ForumPost", "帖子实体类，映射forum_post表"),
                    ("ForumPostMapper", "帖子数据访问接口")
                ],
                "files": [
                    ("ForumController.java", "Java", "backend/src/main/java/com/anti/controller/", "社区控制器"),
                    ("ForumPostServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "帖子服务实现"),
                    ("ForumPost.java", "Java", "backend/src/main/java/com/anti/entity/", "帖子实体"),
                    ("ForumPostMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "帖子数据访问"),
                    ("UserMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "用户数据访问")
                ]
            },
            {
                "id": "5.3.5.3",
                "name": "帖子发布",
                "desc": """本功能用于用户发布新的社区帖子。用户通过POST /api/forum/post接口提交帖子数据，包括标题、内容（富文本）、帖子类型（experience/question/discussion）。系统校验必填字段，自动设置作者为当前用户、初始浏览量、点赞数、评论数为0。发帖成功后奖励+3积分，更新日榜/周榜/总榜，检查发帖成就（post_count）。副作用用try/catch包裹不影响主流程。返回新创建的帖子ID。需要用户登录权限。""",
                "classes": [
                    ("ForumController", "社区控制器，提供POST /api/forum/post接口"),
                    ("ForumPostService", "帖子服务接口，定义帖子创建方法"),
                    ("ForumPostServiceImpl", "帖子服务实现类，实现帖子创建和游戏化触发"),
                    ("ForumPost", "帖子实体类"),
                    ("ForumPostCreateDTO", "帖子创建数据传输对象")
                ],
                "files": [
                    ("ForumController.java", "Java", "backend/src/main/java/com/anti/controller/", "社区控制器"),
                    ("ForumPostServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "帖子服务实现"),
                    ("ForumPost.java", "Java", "backend/src/main/java/com/anti/entity/", "帖子实体"),
                    ("ForumPostCreateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "帖子创建DTO"),
                    ("ScoreService.java", "Java", "backend/src/main/java/com/anti/service/", "积分服务")
                ]
            },
            {
                "id": "5.3.5.4",
                "name": "帖子编辑",
                "desc": """本功能用于用户编辑自己发布的帖子。用户通过PUT /api/forum/post/{postId}接口提交更新数据，可修改标题、内容、帖子类型。系统校验帖子是否存在，验证操作者是否为帖子作者或管理员。非作者且非管理员的操作会被拒绝。更新时自动设置更新时间字段。帖子被编辑后，系统会标记为已编辑状态，但不影响已有的浏览量、点赞数、评论数。管理员可以编辑任何帖子。""",
                "classes": [
                    ("ForumController", "社区控制器，提供PUT /api/forum/post/{postId}接口"),
                    ("ForumPostService", "帖子服务接口，定义帖子更新方法"),
                    ("ForumPostServiceImpl", "帖子服务实现类，实现权限校验和更新"),
                    ("ForumPostMapper", "帖子数据访问接口"),
                    ("ForumPostUpdateDTO", "帖子更新数据传输对象")
                ],
                "files": [
                    ("ForumController.java", "Java", "backend/src/main/java/com/anti/controller/", "社区控制器"),
                    ("ForumPostServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "帖子服务实现"),
                    ("ForumPostMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "帖子数据访问"),
                    ("ForumPostUpdateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "帖子更新DTO"),
                    ("SecurityConfig.java", "Java", "backend/src/main/java/com/anti/config/", "安全配置")
                ]
            },
            {
                "id": "5.3.5.5",
                "name": "帖子删除",
                "desc": """本功能用于删除社区帖子。用户通过DELETE /api/forum/post/{postId}接口请求删除。系统校验帖子是否存在，验证操作者是否为帖子作者或管理员。删除操作采用逻辑删除策略，将deleted字段设置为1。同时级联删除帖子相关的所有数据：点赞记录（post_like）、评论（comment）、评论点赞（comment_like）。非作者且非管理员的删除操作会被拒绝。删除后返回成功响应。""",
                "classes": [
                    ("ForumController", "社区控制器，提供DELETE /api/forum/post/{postId}接口"),
                    ("ForumPostService", "帖子服务接口，定义帖子删除方法"),
                    ("ForumPostServiceImpl", "帖子服务实现类，实现权限校验和级联删除"),
                    ("ForumPostMapper", "帖子数据访问接口"),
                    ("CommentMapper", "评论数据访问接口")
                ],
                "files": [
                    ("ForumController.java", "Java", "backend/src/main/java/com/anti/controller/", "社区控制器"),
                    ("ForumPostServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "帖子服务实现"),
                    ("ForumPostMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "帖子数据访问"),
                    ("CommentMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "评论数据访问"),
                    ("PostLikeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "点赞数据访问")
                ]
            },
            {
                "id": "5.3.5.6",
                "name": "帖子点赞",
                "desc": """本功能用于用户对帖子进行点赞或取消点赞。用户通过POST /api/forum/post/{postId}/like接口进行点赞，通过DELETE /api/forum/post/{postId}/like取消点赞。系统在post_like表中创建或删除记录，同时更新forum_post表中的like_count字段。点赞操作具有幂等性，重复点赞不会创建重复记录。点赞数据会影响帖子的排序权重。点赞行为被记录到用户行为画像，用于个性化推荐。""",
                "classes": [
                    ("ForumController", "社区控制器，提供点赞/取消点赞接口"),
                    ("ForumPostService", "帖子服务接口，定义点赞方法"),
                    ("ForumPostServiceImpl", "帖子服务实现类，实现点赞逻辑和计数更新"),
                    ("PostLike", "帖子点赞实体，映射post_like表"),
                    ("PostLikeMapper", "点赞数据访问接口")
                ],
                "files": [
                    ("ForumController.java", "Java", "backend/src/main/java/com/anti/controller/", "社区控制器"),
                    ("ForumPostServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "帖子服务实现"),
                    ("PostLike.java", "Java", "backend/src/main/java/com/anti/entity/", "点赞实体"),
                    ("PostLikeMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "点赞数据访问"),
                    ("ForumPostMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "帖子数据访问")
                ]
            },
            {
                "id": "5.3.5.7",
                "name": "评论查看",
                "desc": """本功能用于查看帖子的评论列表。用户通过GET /api/forum/post/{postId}/comments接口请求，系统查询comment表获取该帖子的所有评论。评论采用树形结构（parent_id），系统递归构建评论树，返回嵌套的评论列表。每条评论包含ID、内容、作者信息、点赞数、创建时间、子评论列表。系统会查询当前用户是否已点赞各条评论，填充isLiked字段。系统还会标记当前用户是否为评论作者（isAuthor）。评论按创建时间排序。""",
                "classes": [
                    ("ForumController", "社区控制器，提供/api/forum/post/{postId}/comments接口"),
                    ("ForumPostService", "帖子服务接口，定义评论查询方法"),
                    ("ForumPostServiceImpl", "帖子服务实现类，实现评论树构建"),
                    ("CommentMapper", "评论数据访问接口，包含树形查询"),
                    ("CommentVO", "评论视图对象，包含子评论列表")
                ],
                "files": [
                    ("ForumController.java", "Java", "backend/src/main/java/com/anti/controller/", "社区控制器"),
                    ("ForumPostServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "帖子服务实现"),
                    ("CommentMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "评论数据访问"),
                    ("CommentMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件"),
                    ("CommentVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "评论视图对象")
                ]
            },
            {
                "id": "5.3.5.8",
                "name": "评论发布",
                "desc": """本功能用于用户对帖子发表评论或回复评论。用户通过POST /api/comment接口提交评论数据，包括帖子ID、评论内容、父评论ID（可选，回复评论时必填）。系统校验帖子是否存在，创建comment记录，关联用户ID和帖子ID。如果是回复评论（parent_id不为空），校验父评论是否属于同一帖子。评论成功后，更新帖子的comment_count字段。评论内容支持纯文本，长度有限制。需要用户登录权限。""",
                "classes": [
                    ("CommentController", "评论控制器，提供POST /api/comment接口"),
                    ("CommentService", "评论服务接口，定义评论创建方法"),
                    ("CommentServiceImpl", "评论服务实现类，实现评论创建"),
                    ("Comment", "评论实体类，映射comment表"),
                    ("CommentCreateDTO", "评论创建数据传输对象")
                ],
                "files": [
                    ("CommentController.java", "Java", "backend/src/main/java/com/anti/controller/", "评论控制器"),
                    ("CommentServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "评论服务实现"),
                    ("Comment.java", "Java", "backend/src/main/java/com/anti/entity/", "评论实体"),
                    ("CommentCreateDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "评论创建DTO"),
                    ("ForumPostMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "帖子数据访问")
                ]
            }
        ]
    },
    "5.3.6 AI智能客服模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.6.1",
                "name": "智能问答",
                "desc": """本功能用于用户与AI进行反诈知识问答。用户通过POST /api/chat/ask接口提交问题，可选提供会话ID（sessionId）以保持上下文连贯。系统从JWT中获取用户ID，如果没有sessionId则创建新会话（格式：session_{userId}_{millis}）。加载该会话的历史对话记录，组装系统提示词（AntiFraudPromptTemplate.getSystemPrompt，包含角色设定、常见诈骗类型、回答原则），调用DeepSeek API（DeepSeekClient.chat）。持久化QAConversation记录（含token消耗），返回AI回答和sessionId。API调用超时60秒，异常捕获返回友好错误提示。""",
                "classes": [
                    ("ChatController", "AI聊天控制器，提供/api/chat/ask接口"),
                    ("QAConversationService", "问答会话服务接口，定义问答方法"),
                    ("QAConversationServiceImpl", "问答会话服务实现类，实现问答流程和持久化"),
                    ("DeepSeekClient", "DeepSeek API客户端，封装HTTP调用"),
                    ("AntiFraudPromptTemplate", "反诈提示词模板，提供系统提示词")
                ],
                "files": [
                    ("ChatController.java", "Java", "backend/src/main/java/com/anti/controller/", "AI聊天控制器"),
                    ("QAConversationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "问答会话实现"),
                    ("DeepSeekClient.java", "Java", "backend/src/main/java/com/anti/util/", "DeepSeek客户端"),
                    ("AntiFraudPromptTemplate.java", "Java", "backend/src/main/java/com/anti/util/", "提示词模板"),
                    ("QAConversation.java", "Java", "backend/src/main/java/com/anti/entity/", "问答会话实体")
                ]
            },
            {
                "id": "5.3.6.2",
                "name": "会话历史查询",
                "desc": """本功能用于查询指定会话的完整对话历史。用户通过GET /api/chat/history/{sessionId}接口请求，系统验证会话是否属于当前用户，然后查询qa_conversation表获取该会话的所有对话记录。返回数据按时间顺序排列，每条记录包含问题、答案、创建时间、模型名称、token消耗等。该接口用于用户查看历史对话、恢复中断的会话上下文。会话历史数据用于AI上下文组装，确保多轮对话的连贯性。""",
                "classes": [
                    ("ChatController", "AI聊天控制器，提供/api/chat/history/{sessionId}接口"),
                    ("QAConversationService", "问答会话服务接口，定义历史查询方法"),
                    ("QAConversationServiceImpl", "问答会话服务实现类，实现历史查询"),
                    ("QAConversationMapper", "问答会话数据访问接口"),
                    ("ChatHistoryVO", "会话历史视图对象")
                ],
                "files": [
                    ("ChatController.java", "Java", "backend/src/main/java/com/anti/controller/", "AI聊天控制器"),
                    ("QAConversationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "问答会话实现"),
                    ("QAConversationMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "会话数据访问"),
                    ("ChatHistoryVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "会话历史VO"),
                    ("QAConversation.java", "Java", "backend/src/main/java/com/anti/entity/", "问答会话实体")
                ]
            },
            {
                "id": "5.3.6.3",
                "name": "会话列表查询",
                "desc": """本功能用于查询用户的所有AI会话列表。用户通过GET /api/chat/sessions接口请求，系统查询qa_conversation表，按会话ID分组，获取每个会话的最新一条记录作为代表。返回会话列表包含会话ID、最后一条问题摘要、最后活动时间、总对话轮数等。支持分页查询，按最后活动时间倒序排列。该接口用于用户管理多个会话、选择继续对话的会话。用户可以删除不需要的会话。""",
                "classes": [
                    ("ChatController", "AI聊天控制器，提供/api/chat/sessions接口"),
                    ("QAConversationService", "问答会话服务接口，定义会话列表方法"),
                    ("QAConversationServiceImpl", "问答会话服务实现类，实现会话列表查询"),
                    ("QAConversationMapper", "问答会话数据访问接口，包含分组查询"),
                    ("ChatSessionVO", "会话列表视图对象")
                ],
                "files": [
                    ("ChatController.java", "Java", "backend/src/main/java/com/anti/controller/", "AI聊天控制器"),
                    ("QAConversationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "问答会话实现"),
                    ("QAConversationMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "会话数据访问"),
                    ("ChatSessionVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "会话列表VO"),
                    ("QAConversationMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件")
                ]
            },
            {
                "id": "5.3.6.4",
                "name": "反馈提交",
                "desc": """本功能用于用户对AI回答进行满意度反馈。用户通过POST /api/chat/feedback接口提交反馈，包含问答记录ID和反馈类型（1满意/-1不满意）。系统更新qa_conversation表中的feedback字段。反馈数据用于评估AI回答质量、优化提示词模板、统计AI服务满意度。系统可以定期分析不满意反馈，调整反诈知识库或改进回答策略。反馈提交后返回成功响应。""",
                "classes": [
                    ("ChatController", "AI聊天控制器，提供/api/chat/feedback接口"),
                    ("QAConversationService", "问答会话服务接口，定义反馈提交方法"),
                    ("QAConversationServiceImpl", "问答会话服务实现类，实现反馈记录"),
                    ("QAConversationMapper", "问答会话数据访问接口"),
                    ("FeedbackDTO", "反馈数据传输对象")
                ],
                "files": [
                    ("ChatController.java", "Java", "backend/src/main/java/com/anti/controller/", "AI聊天控制器"),
                    ("QAConversationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "问答会话实现"),
                    ("QAConversationMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "会话数据访问"),
                    ("FeedbackDTO.java", "Java", "backend/src/main/java/com/anti/entity/dto/", "反馈DTO"),
                    ("QAConversation.java", "Java", "backend/src/main/java/com/anti/entity/", "问答会话实体")
                ]
            }
        ]
    },
    "5.3.7 个性化推荐模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.7.1",
                "name": "自动策略推荐",
                "desc": """本功能用于根据用户生命周期自动选择推荐策略。用户通过GET /api/recommendation/list接口请求，可选指定返回数量（limit）和内容类型（itemType）。系统首先查询用户的UserProfile获取lifecycleStage（newbie/growing/mature），根据阶段自动分发到对应的推荐策略：新手策略（必读资讯+静态属性匹配+热点补充）、成长策略（内容推荐+SPM预测+上下文修正）、成熟策略（协同过滤）。返回推荐结果列表，包含推荐原因和推荐分。结果缓存1小时（cache:recommend:{userId}）。""",
                "classes": [
                    ("RecommendationController", "推荐控制器，提供/api/recommendation/list接口"),
                    ("RecommendationService", "推荐服务接口，定义自动推荐方法"),
                    ("RecommendationServiceImpl", "推荐服务实现类，实现策略分发"),
                    ("UserProfileService", "用户画像服务，提供生命周期判断"),
                    ("RecommendationVO", "推荐结果视图对象")
                ],
                "files": [
                    ("RecommendationController.java", "Java", "backend/src/main/java/com/anti/controller/", "推荐控制器"),
                    ("RecommendationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "推荐服务实现"),
                    ("UserProfileService.java", "Java", "backend/src/main/java/com/anti/service/", "用户画像服务"),
                    ("RecommendationVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "推荐结果VO"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "缓存工具")
                ]
            },
            {
                "id": "5.3.7.2",
                "name": "新手推荐",
                "desc": """本功能为新手用户提供冷启动推荐。用户通过GET /api/recommendation/newbie接口请求。系统判断用户生命周期为新手（注册天数<7或浏览数<5）。推荐策略：优先推荐必读资讯（is_mandatory=true），然后按用户静态属性（年级、专业）匹配案例的targetGrades和targetMajors（支持"all"通配），最后用热点案例补充。评分算法：Wilson分*0.6 + 热度分*0.4。返回推荐列表，每项包含推荐原因（"必读资讯"/"属性匹配"/"热点推荐"）和评分。结果缓存1小时。""",
                "classes": [
                    ("RecommendationController", "推荐控制器，提供/api/recommendation/newbie接口"),
                    ("RecommendationService", "推荐服务接口，定义新手推荐方法"),
                    ("RecommendationServiceImpl", "推荐服务实现类，实现新手推荐策略"),
                    ("RecommendationAlgorithmUtil", "推荐算法工具类，提供Wilson分和热度分计算"),
                    ("UserProfile", "用户画像实体，存储用户属性")
                ],
                "files": [
                    ("RecommendationController.java", "Java", "backend/src/main/java/com/anti/controller/", "推荐控制器"),
                    ("RecommendationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "推荐服务实现"),
                    ("RecommendationAlgorithmUtil.java", "Java", "backend/src/main/java/com/anti/util/", "推荐算法工具"),
                    ("UserProfile.java", "Java", "backend/src/main/java/com/anti/entity/", "用户画像实体"),
                    ("FraudCaseMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "案例数据访问")
                ]
            },
            {
                "id": "5.3.7.3",
                "name": "成长期推荐",
                "desc": """本功能为成长期用户提供个性化推荐。用户通过GET /api/recommendation/growing接口请求。系统判断用户生命周期为成长（注册天数7-30或浏览数5-20）。推荐策略：从user_behavior_matrix获取用户行为向量，与案例标签向量计算余弦相似度（内容推荐）；查询association_rule表获取SPM预测标签（序列模式挖掘）；应用上下文修正（弱点标签权重*1.2、低知识水平权重*1.1）。综合评分：0.5*内容分 + 0.3*上下文分 + 0.2*热度分。返回推荐列表和推荐原因。""",
                "classes": [
                    ("RecommendationController", "推荐控制器，提供/api/recommendation/growing接口"),
                    ("RecommendationService", "推荐服务接口，定义成长期推荐方法"),
                    ("RecommendationServiceImpl", "推荐服务实现类，实现成长期推荐策略"),
                    ("RecommendationAlgorithmUtil", "推荐算法工具类，提供余弦相似度和综合分计算"),
                    ("UserBehaviorMatrix", "用户行为矩阵实体")
                ],
                "files": [
                    ("RecommendationController.java", "Java", "backend/src/main/java/com/anti/controller/", "推荐控制器"),
                    ("RecommendationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "推荐服务实现"),
                    ("RecommendationAlgorithmUtil.java", "Java", "backend/src/main/java/com/anti/util/", "推荐算法工具"),
                    ("UserBehaviorMatrix.java", "Java", "backend/src/main/java/com/anti/entity/", "用户行为矩阵"),
                    ("AssociationRule.java", "Java", "backend/src/main/java/com/anti/entity/", "关联规则实体")
                ]
            },
            {
                "id": "5.3.7.4",
                "name": "成熟期推荐",
                "desc": """本功能为成熟期用户提供协同过滤推荐。用户通过GET /api/recommendation/mature接口请求。系统判断用户生命周期为成熟（注册天数>30且浏览数>20）。推荐策略：从user_similarity表获取相似用户列表（皮尔逊相关系数），使用KNN算法（k=20）找到最相似的20个用户，获取这些用户的行为记录，偏置加权平均预测当前用户对未交互案例的评分，按预测分排序返回推荐列表。同年级预过滤提高相关性。返回推荐列表和推荐原因（"相似用户推荐"）。""",
                "classes": [
                    ("RecommendationController", "推荐控制器，提供/api/recommendation/mature接口"),
                    ("RecommendationService", "推荐服务接口，定义成熟期推荐方法"),
                    ("RecommendationServiceImpl", "推荐服务实现类，实现协同过滤策略"),
                    ("RecommendationAlgorithmUtil", "推荐算法工具类，提供皮尔逊相关系数计算"),
                    ("UserSimilarity", "用户相似度实体")
                ],
                "files": [
                    ("RecommendationController.java", "Java", "backend/src/main/java/com/anti/controller/", "推荐控制器"),
                    ("RecommendationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "推荐服务实现"),
                    ("RecommendationAlgorithmUtil.java", "Java", "backend/src/main/java/com/anti/util/", "皮尔逊相关系数"),
                    ("UserSimilarity.java", "Java", "backend/src/main/java/com/anti/entity/", "用户相似度实体"),
                    ("UserSimilarityMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "相似度数据访问")
                ]
            },
            {
                "id": "5.3.7.5",
                "name": "用户兴趣分析",
                "desc": """本功能用于分析并返回用户的兴趣标签分布。用户通过GET /api/recommendation/interest接口请求。系统从user_profile表获取用户的interestTags字段（JSON数组），包含标签ID和行为分数。将分数归一化到0-100范围，按分数降序排列。返回兴趣标签列表，每项包含标签ID、标签名称、归一化分数。该数据用于前端展示用户的兴趣画像、解释推荐原因。结果缓存2小时（cache:user_interest:{userId}）。如果用户无兴趣数据，返回空列表。""",
                "classes": [
                    ("RecommendationController", "推荐控制器，提供/api/recommendation/interest接口"),
                    ("RecommendationService", "推荐服务接口，定义兴趣分析方法"),
                    ("RecommendationServiceImpl", "推荐服务实现类，实现兴趣分析和归一化"),
                    ("UserProfile", "用户画像实体，包含interestTags字段"),
                    ("UserProfileMapper", "用户画像数据访问接口")
                ],
                "files": [
                    ("RecommendationController.java", "Java", "backend/src/main/java/com/anti/controller/", "推荐控制器"),
                    ("RecommendationServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "推荐服务实现"),
                    ("UserProfile.java", "Java", "backend/src/main/java/com/anti/entity/", "用户画像实体"),
                    ("UserProfileMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "用户画像数据访问"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "缓存工具")
                ]
            }
        ]
    },
    "5.3.8 数据统计模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.8.1",
                "name": "数据看板聚合",
                "desc": """本功能为管理员提供综合数据看板。管理员通过GET /api/statistics/dashboard接口请求，系统聚合多个统计指标：今日浏览量、今日新增用户数、今日活跃用户数、今日通关人数、平均闯关分数、案例总数、资讯总数、帖子总数、用户总数等。每个子查询用try/catch包裹，失败时返回0或mock数据，确保看板不会因单个指标失败而崩溃。数据从daily_statistics表聚合或实时查询。需要ADMIN角色权限。返回DashboardVO聚合对象。""",
                "classes": [
                    ("StatisticsController", "统计控制器，提供/api/statistics/dashboard接口"),
                    ("StatisticsService", "统计服务接口，定义看板聚合方法"),
                    ("StatisticsServiceImpl", "统计服务实现类，实现多指标聚合和容错"),
                    ("DashboardVO", "看板视图对象，封装多个统计指标"),
                    ("DailyStatisticsMapper", "每日统计数据访问接口")
                ],
                "files": [
                    ("StatisticsController.java", "Java", "backend/src/main/java/com/anti/controller/", "统计控制器"),
                    ("StatisticsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "统计服务实现"),
                    ("DashboardVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "看板VO"),
                    ("DailyStatisticsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "每日统计数据访问"),
                    ("StatisticsQueryMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "统计查询Mapper")
                ]
            },
            {
                "id": "5.3.8.2",
                "name": "访问趋势分析",
                "desc": """本功能用于分析平台访问趋势。管理员通过GET /api/statistics/visit/trend接口请求，支持指定时间范围（startDate/endDate）。系统聚合daily_statistics表的数据，按日期分组统计浏览量、活跃用户数、新增用户数、通关人数等。返回时间序列数据，用于前端ECharts折线图展示。趋势数据帮助管理员了解平台使用情况、发现异常波动、评估活动效果。需要ADMIN角色权限。""",
                "classes": [
                    ("StatisticsController", "统计控制器，提供/api/statistics/visit/trend接口"),
                    ("StatisticsService", "统计服务接口，定义趋势分析方法"),
                    ("StatisticsServiceImpl", "统计服务实现类，实现时间序列聚合"),
                    ("DailyStatisticsMapper", "每日统计数据访问接口"),
                    ("VisitTrendVO", "访问趋势视图对象")
                ],
                "files": [
                    ("StatisticsController.java", "Java", "backend/src/main/java/com/anti/controller/", "统计控制器"),
                    ("StatisticsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "统计服务实现"),
                    ("DailyStatisticsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "每日统计数据访问"),
                    ("VisitTrendVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "趋势VO"),
                    ("DailyStatisticsMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件")
                ]
            },
            {
                "id": "5.3.8.3",
                "name": "诈骗类型分布",
                "desc": """本功能用于统计诈骗案例的类型分布。管理员通过GET /api/statistics/fraud/types接口请求。系统从fraud_case表按标签分组统计案例数量，关联case_tag表获取标签名称。返回各类型案例的数量和占比，按数量降序排列。数据用于前端饼图或柱状图展示，帮助管理员了解哪些诈骗类型最多、需要重点防范教育。需要ADMIN角色权限。如果某个案例有多个标签，会在多个类型中分别计数。""",
                "classes": [
                    ("StatisticsController", "统计控制器，提供/api/statistics/fraud/types接口"),
                    ("StatisticsService", "统计服务接口，定义类型分布方法"),
                    ("StatisticsServiceImpl", "统计服务实现类，实现标签分组统计"),
                    ("FraudCaseMapper", "案例数据访问接口，包含标签分组查询"),
                    ("FraudTypeVO", "诈骗类型视图对象")
                ],
                "files": [
                    ("StatisticsController.java", "Java", "backend/src/main/java/com/anti/controller/", "统计控制器"),
                    ("StatisticsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "统计服务实现"),
                    ("FraudCaseMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "案例数据访问"),
                    ("FraudCaseMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件"),
                    ("FraudTypeVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "类型分布VO")
                ]
            },
            {
                "id": "5.3.8.4",
                "name": "院系积分统计",
                "desc": """本功能用于统计各院系的积分排名。管理员通过GET /api/statistics/department/scores接口请求。系统从department_statistics表获取院系统计数据，或从sys_user表关联user_score表按院系分组聚合。返回各院系的平均积分、总积分、用户数、通关率等指标。数据用于院系之间的反诈学习效果对比，激励各院系加强反诈教育。需要ADMIN角色权限。按平均积分降序排列。""",
                "classes": [
                    ("StatisticsController", "统计控制器，提供/api/statistics/department/scores接口"),
                    ("StatisticsService", "统计服务接口，定义院系统计方法"),
                    ("StatisticsServiceImpl", "统计服务实现类，实现院系聚合"),
                    ("DepartmentStatisticsMapper", "院系统计数据访问接口"),
                    ("DepartmentScoreVO", "院系积分视图对象")
                ],
                "files": [
                    ("StatisticsController.java", "Java", "backend/src/main/java/com/anti/controller/", "统计控制器"),
                    ("StatisticsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "统计服务实现"),
                    ("DepartmentStatisticsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "院系统计数据访问"),
                    ("DepartmentScoreVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "院系积分VO"),
                    ("DepartmentStatistics.java", "Java", "backend/src/main/java/com/anti/entity/", "院系统计实体")
                ]
            },
            {
                "id": "5.3.8.5",
                "name": "统计数据导出",
                "desc": """本功能用于导出统计数据为Excel文件。管理员通过GET /api/statistics/export/daily接口请求，支持指定时间范围。系统使用EasyExcel生成Excel文件，包含日期、浏览量、新增用户、活跃用户、通关人数、平均分数等列。文件名包含导出日期范围。响应头设置Content-Disposition为attachment，浏览器自动下载。导出大数据量时分批查询避免内存溢出。需要ADMIN角色权限。另有/api/statistics/export/department接口导出院系统计。""",
                "classes": [
                    ("StatisticsController", "统计控制器，提供/api/statistics/export/daily接口"),
                    ("StatisticsService", "统计服务接口，定义导出方法"),
                    ("StatisticsServiceImpl", "统计服务实现类，实现EasyExcel导出"),
                    ("DailyStatistics", "每日统计实体类"),
                    ("EasyExcel", "阿里开源Excel处理库")
                ],
                "files": [
                    ("StatisticsController.java", "Java", "backend/src/main/java/com/anti/controller/", "统计控制器"),
                    ("StatisticsServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "统计服务实现"),
                    ("DailyStatistics.java", "Java", "backend/src/main/java/com/anti/entity/", "每日统计实体"),
                    ("pom.xml", "XML", "backend/", "Maven依赖配置（EasyExcel 3.3.3）"),
                    ("DailyStatisticsMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "统计数据访问")
                ]
            }
        ]
    },
    "5.3.9 积分成就与排行榜模块": {
        "existing_subsections": [],
        "functions": [
            {
                "id": "5.3.9.1",
                "name": "积分信息查询",
                "desc": """本功能用于查询用户的积分和等级信息。用户通过GET /api/score/info接口请求，系统从JWT中获取用户ID，查询user_score表获取积分数据。返回总积分（totalScore）、当前等级（currentLevel）、周积分（weeklyScore）、升级所需积分（下一级需要totalScore>=level*100）。等级计算公式：level = totalScore/100 + 1。同时返回积分历史记录（最近获得积分的来源和金额）。需要用户登录权限。""",
                "classes": [
                    ("ScoreController", "积分控制器，提供/api/score/info接口"),
                    ("ScoreService", "积分服务接口，定义积分查询方法"),
                    ("ScoreServiceImpl", "积分服务实现类，实现积分查询和等级计算"),
                    ("UserScore", "用户积分实体，映射user_score表"),
                    ("UserScoreMapper", "用户积分数据访问接口")
                ],
                "files": [
                    ("ScoreController.java", "Java", "backend/src/main/java/com/anti/controller/", "积分控制器"),
                    ("ScoreServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "积分服务实现"),
                    ("UserScore.java", "Java", "backend/src/main/java/com/anti/entity/", "用户积分实体"),
                    ("UserScoreMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "积分数据访问"),
                    ("ScoreVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "积分视图对象")
                ]
            },
            {
                "id": "5.3.9.2",
                "name": "成就列表查询",
                "desc": """本功能用于获取系统所有成就定义列表。用户通过GET /api/achievement/list接口请求，系统查询achievement表获取所有成就定义。返回成就列表，每项包含成就代码、成就名称、成就描述、条件类型（login_count/browse_count/post_count/challenge_count等）、条件阈值、积分奖励、图标。成就定义包含连续学习、浏览数、发帖数、闯关数、满分通关、完成所有关卡等多种类型。该接口用于成就展示页面。无需登录即可访问。""",
                "classes": [
                    ("AchievementController", "成就控制器，提供/api/achievement/list接口"),
                    ("AchievementService", "成就服务接口，定义成就列表方法"),
                    ("AchievementServiceImpl", "成就服务实现类，实现成就列表查询"),
                    ("Achievement", "成就实体类，映射achievement表"),
                    ("AchievementMapper", "成就数据访问接口")
                ],
                "files": [
                    ("AchievementController.java", "Java", "backend/src/main/java/com/anti/controller/", "成就控制器"),
                    ("AchievementServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "成就服务实现"),
                    ("Achievement.java", "Java", "backend/src/main/java/com/anti/entity/", "成就实体"),
                    ("AchievementMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "成就数据访问"),
                    ("AchievementVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "成就视图对象")
                ]
            },
            {
                "id": "5.3.9.3",
                "name": "用户成就查询",
                "desc": """本功能用于查询当前用户已获得的成就列表。用户通过GET /api/achievement/user接口请求，系统查询user_achievement表获取该用户的所有成就记录，关联achievement表获取成就详情。返回已获得成就列表，每项包含成就信息、获得时间、奖励积分。同时返回总成就数和已获得成就数，用于前端展示成就进度。需要用户登录权限。另有/api/achievement/user/count接口返回成就数量。""",
                "classes": [
                    ("AchievementController", "成就控制器，提供/api/achievement/user接口"),
                    ("AchievementService", "成就服务接口，定义用户成就查询方法"),
                    ("AchievementServiceImpl", "成就服务实现类，实现用户成就查询"),
                    ("UserAchievementMapper", "用户成就数据访问接口"),
                    ("UserAchievementVO", "用户成就视图对象")
                ],
                "files": [
                    ("AchievementController.java", "Java", "backend/src/main/java/com/anti/controller/", "成就控制器"),
                    ("AchievementServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "成就服务实现"),
                    ("UserAchievementMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "用户成就数据访问"),
                    ("UserAchievementVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "用户成就VO"),
                    ("UserAchievementMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件")
                ]
            },
            {
                "id": "5.3.9.4",
                "name": "日榜查询",
                "desc": """本功能用于查询今日积分排行榜。用户通过GET /api/leaderboard/daily接口请求，系统从Redis缓存读取日榜数据（缓存键：cache:leaderboard:daily，有效期300秒），如果缓存不存在则查询leaderboard表（period_type='daily'）。返回排名列表，包含用户ID、昵称、头像、今日积分、排名、更新时间。日榜每日0点重置，由定时任务LeaderboardServiceImpl.refreshRankings()执行。支持分页查询，默认返回前50名。需要用户登录权限。""",
                "classes": [
                    ("LeaderboardController", "排行榜控制器，提供/api/leaderboard/daily接口"),
                    ("LeaderboardService", "排行榜服务接口，定义日榜查询方法"),
                    ("LeaderboardServiceImpl", "排行榜服务实现类，实现缓存查询和定时刷新"),
                    ("LeaderboardMapper", "排行榜数据访问接口"),
                    ("RedisCacheUtil", "Redis缓存工具类")
                ],
                "files": [
                    ("LeaderboardController.java", "Java", "backend/src/main/java/com/anti/controller/", "排行榜控制器"),
                    ("LeaderboardServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "排行榜服务实现"),
                    ("LeaderboardMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "排行榜数据访问"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "缓存工具"),
                    ("LeaderboardVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "排行榜视图对象")
                ]
            },
            {
                "id": "5.3.9.5",
                "name": "周榜查询",
                "desc": """本功能用于查询本周积分排行榜。用户通过GET /api/leaderboard/weekly接口请求，系统从Redis缓存读取周榜数据（缓存键：cache:leaderboard:weekly，有效期900秒），如果缓存不存在则查询leaderboard表（period_type='weekly'）。返回排名列表，包含用户ID、昵称、头像、本周积分、排名、更新时间。周榜每周一0点重置。系统按weeklyScore字段排序，该字段在用户获得积分时更新。支持分页查询，默认返回前50名。需要用户登录权限。""",
                "classes": [
                    ("LeaderboardController", "排行榜控制器，提供/api/leaderboard/weekly接口"),
                    ("LeaderboardService", "排行榜服务接口，定义周榜查询方法"),
                    ("LeaderboardServiceImpl", "排行榜服务实现类，实现周榜缓存和更新"),
                    ("Leaderboard", "排行榜实体类，映射leaderboard表"),
                    ("LeaderboardMapper", "排行榜数据访问接口")
                ],
                "files": [
                    ("LeaderboardController.java", "Java", "backend/src/main/java/com/anti/controller/", "排行榜控制器"),
                    ("LeaderboardServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "排行榜服务实现"),
                    ("Leaderboard.java", "Java", "backend/src/main/java/com/anti/entity/", "排行榜实体"),
                    ("LeaderboardMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "排行榜数据访问"),
                    ("RedisCacheUtil.java", "Java", "backend/src/main/java/com/anti/util/", "缓存工具")
                ]
            },
            {
                "id": "5.3.9.6",
                "name": "总榜查询",
                "desc": """本功能用于查询总积分排行榜。用户通过GET /api/leaderboard/all接口请求，系统从Redis缓存读取总榜数据（缓存键：cache:leaderboard:all，有效期1800秒），如果缓存不存在则查询leaderboard表（period_type='all'）。返回排名列表，包含用户ID、昵称、头像、总积分、等级、排名、更新时间。总榜按totalScore字段排序，永不重置。支持分页查询，默认返回前50名。总榜反映用户的历史总贡献，激励用户长期学习。需要用户登录权限。""",
                "classes": [
                    ("LeaderboardController", "排行榜控制器，提供/api/leaderboard/all接口"),
                    ("LeaderboardService", "排行榜服务接口，定义总榜查询方法"),
                    ("LeaderboardServiceImpl", "排行榜服务实现类，实现总榜缓存和查询"),
                    ("LeaderboardMapper", "排行榜数据访问接口"),
                    ("LeaderboardVO", "排行榜视图对象")
                ],
                "files": [
                    ("LeaderboardController.java", "Java", "backend/src/main/java/com/anti/controller/", "排行榜控制器"),
                    ("LeaderboardServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "排行榜服务实现"),
                    ("LeaderboardMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "排行榜数据访问"),
                    ("LeaderboardVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "排行榜VO"),
                    ("CacheConstants.java", "Java", "backend/src/main/java/com/anti/common/", "缓存常量")
                ]
            },
            {
                "id": "5.3.9.7",
                "name": "用户排名查询",
                "desc": """本功能用于查询当前用户在排行榜中的排名。用户通过GET /api/leaderboard/user-rank接口请求，支持指定榜单类型（type: daily/weekly/all）。系统查询leaderboard表获取该用户在指定榜单中的排名和积分。返回用户排名、积分、前一名用户的积分差距、后一名用户的积分差距。该接口用于前端展示用户的排名位置，激励用户提升排名。如果用户未上榜，返回未上榜状态。需要用户登录权限。""",
                "classes": [
                    ("LeaderboardController", "排行榜控制器，提供/api/leaderboard/user-rank接口"),
                    ("LeaderboardService", "排行榜服务接口，定义用户排名查询方法"),
                    ("LeaderboardServiceImpl", "排行榜服务实现类，实现用户排名查询"),
                    ("LeaderboardMapper", "排行榜数据访问接口"),
                    ("UserRankVO", "用户排名视图对象")
                ],
                "files": [
                    ("LeaderboardController.java", "Java", "backend/src/main/java/com/anti/controller/", "排行榜控制器"),
                    ("LeaderboardServiceImpl.java", "Java", "backend/src/main/java/com/anti/service/impl/", "排行榜服务实现"),
                    ("LeaderboardMapper.java", "Java", "backend/src/main/java/com/anti/mapper/", "排行榜数据访问"),
                    ("UserRankVO.java", "Java", "backend/src/main/java/com/anti/entity/vo/", "用户排名VO"),
                    ("LeaderboardMapper.xml", "XML", "backend/src/main/resources/mapper/", "MyBatis映射文件")
                ]
            }
        ]
    }
}


def add_table_after_paragraph(doc, paragraph, headers, rows):
    """在段落后添加表格"""
    # 在段落后插入表格
    table = doc.add_table(rows=len(rows)+1, cols=len(headers))
    table.style = 'Table Grid'
    
    # 设置表头
    for i, header in enumerate(headers):
        cell = table.rows[0].cells[i]
        cell.text = header
        # 设置表头加粗
        for paragraph in cell.paragraphs:
            for run in paragraph.runs:
                run.bold = True
    
    # 填充数据
    for row_idx, row_data in enumerate(rows):
        for col_idx, cell_data in enumerate(row_data):
            table.rows[row_idx + 1].cells[col_idx].text = str(cell_data)
    
    return table


def find_paragraph_index(doc, text):
    """查找包含特定文本的段落索引"""
    for i, para in enumerate(doc.paragraphs):
        if text in para.text:
            return i
    return -1


def find_module_end_index(doc, start_idx, module_title):
    """查找模块结束位置（下一个模块开始前或文档结束）"""
    # 查找下一个同级标题
    for i in range(start_idx + 1, len(doc.paragraphs)):
        para = doc.paragraphs[i]
        # 检查是否是Heading 3（模块小节标题）
        if para.style.name == 'Heading 3' and para.text.startswith('5.3.'):
            # 检查是否是下一个模块（如5.3.2 -> 5.3.3）
            current_num = int(module_title.split()[0].split('.')[2])
            next_num = int(para.text.split()[0].split('.')[2])
            if next_num > current_num:
                return i
    return len(doc.paragraphs)


def expand_module(doc, module_title, module_data):
    """扩展单个模块，添加功能子节"""
    print(f"正在扩展模块: {module_title}")
    
    # 查找模块标题段落
    module_idx = find_paragraph_index(doc, module_title.split()[0])
    if module_idx == -1:
        print(f"  未找到模块: {module_title}")
        return 0
    
    # 查找模块结束位置
    end_idx = find_module_end_index(doc, module_idx, module_title)
    
    # 在模块结束位置前插入功能子节
    added_count = 0
    
    for func in module_data["functions"]:
        func_id = func["id"]
        func_name = func["name"]
        
        # 检查是否已存在该子节
        existing = False
        for para in doc.paragraphs:
            if func_id in para.text:
                existing = True
                break
        
        if existing:
            print(f"  子节 {func_id} 已存在，跳过")
            continue
        
        # 添加功能子节标题 (Heading 4)
        heading_para = doc.add_paragraph(f"{func_id} {func_name}")
        heading_para.style = 'Heading 4'
        
        # 移动到正确位置
        doc.paragraphs[end_idx]._element.addprevious(heading_para._element)
        
        # 添加功能设计描述
        desc_para = doc.add_paragraph(func["desc"])
        desc_para.style = 'Normal'
        doc.paragraphs[end_idx]._element.addprevious(desc_para._element)
        
        # 添加类介绍
        class_header = doc.add_paragraph("（1）类")
        class_header.style = 'Normal'
        doc.paragraphs[end_idx]._element.addprevious(class_header._element)
        
        for class_name, class_desc in func["classes"]:
            class_para1 = doc.add_paragraph(f"1）{class_name}")
            class_para1.style = 'Normal'
            doc.paragraphs[end_idx]._element.addprevious(class_para1._element)
            
            class_para2 = doc.add_paragraph(class_desc)
            class_para2.style = 'Normal'
            doc.paragraphs[end_idx]._element.addprevious(class_para2._element)
        
        # 添加文件列表
        file_header1 = doc.add_paragraph("（3）文件列表")
        file_header1.style = 'Normal'
        doc.paragraphs[end_idx]._element.addprevious(file_header1._element)
        
        file_header2 = doc.add_paragraph("如下表所示。")
        file_header2.style = 'Normal'
        doc.paragraphs[end_idx]._element.addprevious(file_header2._element)
        
        table_title = doc.add_paragraph(f"表{func_id} {func_name}文件列表")
        table_title.style = 'Normal'
        doc.paragraphs[end_idx]._element.addprevious(table_title._element)
        
        # 添加表格
        headers = ["名称", "类型", "存放位置", "说明"]
        rows = [list(f) for f in func["files"]]
        table = doc.add_table(rows=len(rows)+1, cols=len(headers))
        table.style = 'Table Grid'
        
        # 设置表头
        for i, header in enumerate(headers):
            cell = table.rows[0].cells[i]
            cell.text = header
            for paragraph in cell.paragraphs:
                for run in paragraph.runs:
                    run.bold = True
        
        # 填充数据
        for row_idx, row_data in enumerate(rows):
            for col_idx, cell_data in enumerate(row_data):
                table.rows[row_idx + 1].cells[col_idx].text = str(cell_data)
        
        # 移动表格到正确位置
        doc.paragraphs[end_idx]._element.addprevious(table._element)
        
        added_count += 1
        print(f"  已添加子节: {func_id} {func_name}")
    
    return added_count


def main():
    """主函数"""
    print("=" * 60)
    print("系统设计说明书5.3节扩展脚本")
    print("=" * 60)
    
    # 检查备份文件
    if not BACKUP_FILE.exists():
        print(f"错误: 备份文件不存在: {BACKUP_FILE}")
        return
    
    # 从备份恢复（确保幂等性）
    print(f"从备份文件读取: {BACKUP_FILE.name}")
    
    # 打开文档
    doc = Document(BACKUP_FILE)
    
    # 统计初始段落数
    initial_paragraphs = len(doc.paragraphs)
    initial_tables = len(doc.tables)
    print(f"初始段落数: {initial_paragraphs}")
    print(f"初始表格数: {initial_tables}")
    
    # 扩展各模块
    total_added = 0
    for module_title, module_data in MODULES.items():
        added = expand_module(doc, module_title, module_data)
        total_added += added
    
    # 保存到输出文件
    print(f"保存到输出文件: {OUTPUT_FILE.name}")
    doc.save(OUTPUT_FILE)
    
    # 重新打开统计
    doc = Document(OUTPUT_FILE)
    final_paragraphs = len(doc.paragraphs)
    final_tables = len(doc.tables)
    
    # 统计新增表格数
    # 计算所有功能子节的总数
    total_functions = sum(len(m["functions"]) for m in MODULES.values())
    
    print("=" * 60)
    print("扩展完成!")
    print(f"新增功能子节: {total_added}")
    print(f"新增表格: {total_added}")  # 每个功能子节一个表格
    print(f"段落数变化: {initial_paragraphs} -> {final_paragraphs} (+{final_paragraphs - initial_paragraphs})")
    print(f"表格数变化: {initial_tables} -> {final_tables} (+{final_tables - initial_tables})")
    print(f"输出文件: {OUTPUT_FILE}")
    print("=" * 60)
    print("\n请关闭原Word文档，然后手动将输出文件重命名为:")
    print(f"  {TARGET_FILE.name}")
    print("=" * 60)


if __name__ == "__main__":
    main()