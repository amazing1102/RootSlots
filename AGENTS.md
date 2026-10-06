# AGENTS.md — D:\CET4 工作区

CET-4 构词学习项目:词根词缀资料 + 数据管线 + 「词根老虎机 RootSlots」Android 单机 App。
**动态进度与下一步见 [HANDOFF.md](HANDOFF.md);活文档计划书见 [词根老虎机-计划书.md](词根老虎机-计划书.md)(里程碑表以其为准)。**

## 目录结构

```
D:\CET4\
├── cet_4_words.md                    # 源词表(6286 词,含粘连词/错拼已由管线清洗)
├── cet4_roots_affixes.md             # 前缀64/后缀49/词根272 总表(阶段二扩充见 tools/roots_stage2.py)
├── cet4_prefix_root_suffix_words.md  # 三段式 856 词
├── cet4_word_formation_map.md        # 组合图谱(族键有别名:cid2/bank2 等)
├── assets\                           # ★ 管线输出目录(build_data.py 的 ASSETS)
│   ├── words/morphs/families/combos.json
│   ├── gd.json / exams.json          # ECDICT 多义项释义 + 存量词考试标签(build_gd.py)
│   ├── exam_words.json / exam_lemma_tags.json  # 扩库新词 + 屈折吸收标签(build_dict.py)
│   ├── glosses\c01–c07.json          # 释义批文件(2297 条,新增批次也放这里)
│   └── sentences\s01.json            # 例句批次({word:[英,中]},新增批次顺延)
├── tools\
│   ├── build_data.py                 # 数据管线:md → 4 份 JSON + 合并 glosses/gd/exams/例句/扩库词
│   ├── build_gd.py                   # ECDICT→存量词多义项释义+考试标签(需 tools/stardict.db)
│   ├── build_dict.py                 # 词库扩展合并管线:ECDICT 7 考试新词入库(§4/§5 规则)
│   ├── roots_stage2.py               # 阶段二词根扩充数据(新根 107+词面扩充)
│   ├── validate_batch.py             # 例句批文件校验器(键集/词在句中/查重)
│   ├── verify_assets.py              # 校验器(拼接一致性+key 可解析,须 0 错误)
│   └── shots\                        # 验收截图(不入库)
└── RootSlots\                        # Android App(包名 com.cet4.rootslots)
    └── app\src\main\
        ├── assets\                   # ★ App assets:管线 4 份 JSON 拷贝到这里
        └── java\com\cet4\rootslots\
            ├── data\                 # Repository.kt(含 ensurePrefilled)、GamePrefs(DataStore)、Room、
            │                         # Duels.kt(战书表 v8)、ChallengeCodec.kt(战书码 RS1 编解码)
            ├── tts\TtsHelper.kt
            └── ui\{slots,detail,codex,mine,nav,quiz,review,duel,theme}
```

**双 assets 目录注意**:释义批文件放在 `D:\CET4\assets\glosses\`(管线侧),由 build_data.py
合并进 words.json 的 `"g"` 字段;App 只需要合并后的 4 份 JSON 拷到 `RootSlots\app\src\main\assets\`。

## 技术栈与环境(本机路径)

- AGP 8.5.2 / Gradle 8.7 / Kotlin 2.0.21 / Compose BOM 2024.09 / Room 2.6.1 + KSP;minSdk 26 / target 35
- 完全离线单机:无网络权限、无后端;DataStore 存设置;Room 预填词库(当前 DB v8(v7+duels 战书表),词库超集 14653 词=7 考试并集)
- 双模拟器验收:临时 AVD `DuelB`(同一 system-image android-35/default 克隆,`emulator -port 5556`),好友PK 互传码测试用
- Android SDK:`D:\33603\AppData\Local\Android\Sdk`(local.properties 已配)
- JDK:**必须 17** `D:\Java\jdk-17.0.18`(本机默认 java 是 25,不能用;已写入 gradle.properties `org.gradle.java.home`)
- Gradle:**用 `D:\gradle-8.7\bin\gradle.bat`,不要用 gradlew.bat**(wrapper jar 损坏)
- 模拟器:AVD `RootSlots`(API 35),`ANDROID_AVD_HOME=D:\33603\.android\avd`
- settings.gradle.kts 配了阿里云 Maven 镜像(google() 兜底)

## 常用命令

```bash
# 数据管线(改了 md 源表或 glosses 批文件后;ipa.json 缺失或词表变更时先跑 build_ipa.py;
# build_gd/build_dict 依赖 tools/stardict.db(851MB,已 gitignore),词表没变可跳过)
cd /d/CET4 && python tools/build_ipa.py && python tools/build_gd.py && python tools/build_dict.py && python tools/build_data.py && python tools/verify_assets.py
cp assets/{words,morphs,families,combos}.json RootSlots/app/src/main/assets/

# 构建 + 安装
cd /d/CET4/RootSlots
JAVA_HOME=D:/Java/jdk-17.0.18 /d/gradle-8.7/bin/gradle.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 改 assets 后二选一:
#  a) 常规内容更新(释义/例句/新词加行):递增 Repository.ASSETS_VER → install -r,数据无损
#  b) 结构性变更(拆词/族表变了):清库重灌(ensurePrefilled 只对空库)
adb shell pm clear com.cet4.rootslots && adb install -r ...
```

验收方式:每个里程碑在模拟器实测 + 截图。SPIN 按钮约 `adb shell input tap 540 1784`(1080×2400,M12 后布局;底部 5 Tab y≈2220)。

## 代码约定

- **主题**:所有颜色走语义 token(`LocalAppColors`,ui/theme/Theme.kt),界面代码禁止硬编码色值;
  每页根布局显式 `.background(c.bg)`(浅色主题下否则露黑底)+ `statusBarsPadding()`+`navigationBarsPadding()`(targetSdk 35 强制 edge-to-edge)。
- **导航**:无导航库。MainActivity 持有 Tab 状态(RootTab,**必须 enum**,sealed class 有 JVM 类初始化循环崩溃坑)+
  Overlay 全屏推入页(详情/复习,推入时隐藏 Tab 栏,保留 BackHandler);Tab 页不写返回键处理。
- **Room**:升 version 必须写真迁移 Migration 对象(**禁止破坏性迁移**,v7 起范式,会清用户收藏/SRS);
  预填唯一入口 `Repository.ensurePrefilled()`(带 Mutex、空库判定),**onCreate 回调里别再挂预填**
  (双路径并发会把 morphs/combos 自增主键表灌翻倍,真实踩过)。
- **考试池过滤**:出题面(老虎机/测验/图鉴)一律走 `Repository.wordInPool`;收藏/SRS/统计/详情页永不过滤。
- **JSON filler**:别用 `JSONArray.join`(会加引号),用 `AppDatabase.jsonJoin`。
- 每个里程碑完成后更新 计划书-计划书.md 状态列和 HANDOFF.md,再提交 git。

## 已知坑速查(高频)

1. 改 assets → 常规内容更新走 ASSETS_VER 闸门(install -r 即可);拆词/族表等结构性变更才 `pm clear` 重灌。
   预填异步,首屏轮询 `awaitReady`(空库 60s 超时会 Log.e)。
2. MCP 截图宽 900、真机坐标 1080:**`input tap` 坐标 ×1.2**;ui_resolve 对 emoji(📚 等)解析不到,对普通中英文可用。
3. 模拟器 API 35 无任何 TTS 引擎,TtsHelper 静默跳过属预期,真机才有声。
4. 模拟器偶发自弹"屏幕截图"编辑器浮层,input tap 全打空 → HOME 键退出重进。
5. Compose 固定高度 Box 内 Column:子项多于 3 个会被量成 h=0 → 滚筒内容必须
   `Modifier.wrapContentHeight(align = Top, unbounded = true)`。
6. Compose Canvas:`drawPath` 第 3 参是 alpha 不是宽度,描边用 `style = Stroke(width)`。
7. 详情页收藏后 SRS 状态不会自动刷新(LaunchedEffect(word) 只跑一次)→ toggleFavorite 后手动刷新。
8. 源表已知错误(管线已处理,新增数据时留意):粘连词 accordingto/owingto 等、错拼 fiarly/jewelery/instalation、
   图谱引用 august 不在词表、族键别名 cid2/bank2 与总表不同名。

## 求职背景(影响取舍)

项目用于全栈/AI 应用方向求职展示,重视:真实可用、模拟器实测验收、代码可讲清设计决策。
一期明确不做:账号/云同步/排行榜/iOS/词根词缀编辑器。
