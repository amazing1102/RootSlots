# HANDOFF.md — 交接快照(会话恢复入口)

> 最后更新:2026-10-05(会话 `sess_77aab405-9056-48e9-86c7-530ed53e1e43`)。
> 每完成一个节点,更新本文件并 git 提交。需求决策与设计细节见 [词根老虎机-计划书.md](词根老虎机-计划书.md) v2。

## 当前状态(2026-10-05)

**已全部模拟器实测通过:**

| 里程碑 | 内容 |
|---|---|
| M0~M5 (v1) | 环境/数据管线/老虎机核心/详情页/图鉴/生词本+SRS+测验 |
| M6 | 明暗双主题(设计 token `LocalAppColors`)+ 设置页 |
| M7a | 底部 5 Tab(老虎机/图鉴/生词/测验/我的)+ 我的页合并设置 + 推入页 |
| M7b | 图鉴搜索框 + 单行紧凑卡 + 详情页/图鉴 chips FlowRow 换行 + `per ?` 兜底 |
| M8 | 动态段轴(轴数=段数 2~4)+ 拆分治理(族键回退/后缀优先/收紧验收,2278 词 0 错误,19 词退整词)+ 结果卡完整词逐段点亮 |
| M11 | 艾宾浩斯 9 节点(5分/30分/12时/1/2/4/7/15/30天)+ 毕业态 + 详情页保持率曲线 |
| M9 | 美式 IPA:CMUdict 0.7b→ARPAbet→IPA(98.2% 覆盖,缺词 113 导出)→ words.json "i" → DB v5 → 五处展示 |
| M10 | 内置神经语音:sherpa-onnx AAR 1.13.8 + Piper en_US-amy fp32(63MB);SpeechEngine 抽象(系统TTS先顶/Piper就绪自动切换);espeak-ng-data 拷 filesDir;APK 175MB |
| M12 | 机台化视觉与动效(2026-10-05):机柜+跑马灯泡带、纸面滚筒+三段停轴+字迹减淡、Bungee/DSEG7 字体、LED 计数、SPIN 按压+触感、结果卡入场/胜利脉冲、推入页上滑转场、各页微交互;README 六图已更新。P8 后补:详情页段卡一段一行整宽竖排(有几段显示几行,不横滑)+ 顶栏固定不随滚;发音按钮统一 Material VolumeUp 图标(详情/复习/测验重播/我的试听) |
| M13 | 转轴经济(2026-10-05,用户采纳 A+C 组合):**落定开考**——停轴后 4 选 1 猜释义,答对 ×1.5/答错 −3🪙+词自动进生词本/跳过半价,答错清连击;**锈词折损**——生词本逾期词再转出固定 +2 且清连击,卡片挂逾期提示;毕业词基础 15;**SPIN 阶段感知动画**——点击/出题中淡出缩至 0.55 让位整屏出题,答完延迟 1.6s 淡入弹簧弹回,答后题区内浮现释义+结算提示;**答错弹窗**——「已移入生词本」+ 可勾选「下次不用提醒,自动加入」(DataStore wrong_notify 持久化),勾选后静默入生词本;弹窗期间 SPIN 隐藏,关闭后 0.4s 弹回;出题中详情入口禁用防剧透,结算区可滚动 |
| M14 | 学习统计(2026-10-05,用户采纳双方案):**学习日历**——「我的」页月历热力图(格色 = 当日量÷当日目标,今天描边,底部本月共学/达标天数 + 🔥连续达标),历史存 DataStore daily_stats("yyyymmdd:count:goal" stringSet,零 DB 迁移),每次 spin 经 recordDailySpin() 累加;**生词本遗忘进度条**——每行 3dp 细条 = Repository.forgetProgress(stage/dueAt 算档位间隔消耗度,毕业 −1 显示满条金,到期红/过半金/新鲜绿),列表按紧迫度倒序(到期置顶、毕业与未排期垫底) |
| M15 | 释义增强 + 例句(2026-10-05,词库扩展阶段一数据层):**ECDICT 接入**(tools/build_gd.py,sqlite 340 万词条,MIT)→ gd.json 多义项释义 6255/6286(词性归一 a.→adj.,滤 [化][医] 域噪音行,≤4 义项)+ exams.json 考试标签(词库扩展用);**例句** assets/sentences/s01.json 首批 250 组合词(用户追加批次顺延,同释义批模式);**DB v7 非破坏迁移**(首个真迁移,fallbackToDestructive 已移除):words 加 exam_tags/detail_gloss/sen_en/sen_zh 四列;**升级补灌** enrichIfNeeded(ASSETS_VER 闸门,老安装从 assets 补空字段,数据更新递增常量重跑);**UI**:详情页「释义」多义项(词性蓝色前缀)+「例句」卡(当前词高亮+中文+🔊整句朗读),复习页揭示后带例句;结果卡/列表仍用短释义 |
| M16 | 词库扩展阶段一·架构贯通(2026-10-05,按 词库扩展-数据规格.md §9 实施):**合并管线 tools/build_dict.py**——ECDICT gk(高考)词拉取,§4 规则落地(屈折吸收 39 词标签并入原形/去词组/去单字符/大小写去重),§5 清洗(自策释义优先,ECDICT 首义项 ≤24 汉字,IPA phonetic 兜底 339/424);**超集 6710 词**(6286+424 新 gaokao 词,assets/words.json 1.3MB);**考试池过滤**(§8.2 清单):Repository 内存过滤 wordInPool(exam_tags 包裹式解析,无标签旧行按 cet4 兜底),randomCombo 抽词池(带缓存)/glossedWords 题池/图鉴族列表(count 按池内重计,空族隐藏)全过滤;**收藏/SRS/统计/详情页永不过滤**(详情页同族词用 familyWordListAll 全库口径);**目标考试 UI**:「我的-目标考试」多选 chips(阶段一放开 cet4/gaokao,EXAM_CHOICES 清单),空选兜底回 cet4;**升级补插**:enrichIfNeeded 兼职插新词行(INSERT IGNORE,只加行不动行,words.json 里库里缺的行就地补);**模拟器实测**:升级路径 newWords=424 插入、gaokao 单池转 3/3 词全带 gaokao 标签(1349 纯四级组合词被排除)、严格单池测验选项 4/4 在池、族覆盖 3/296↔3/352 双向刷新、收藏/SRS/角标全程不变;**顺手修复预填竞态**:onCreate 回调与 ensurePrefilled 双路径并发各灌一次导致自增主键表(morphs/combos)整体翻倍,已删 onCreate 预填留 ensurePrefilled 唯一路径 |
| 数据 | **释义 6710/6710 全覆盖**(存量 glosses/c01–c19 + 新词 ECDICT 短释义)+ 音标 6512 词;组合词 2278(校验 0 错误);考试标签 gaokao 3643 / cet4 6387 |

## 经济数值(M13 后,改玩法先看)

- 基础奖励:新词/未逾期 10,已毕业 15;×(1 + min(combo,20)×0.05) + 同族连击 5×(n−1);答对 ×1.5,跳过 ×0.5
- 答错:−3🪙(钱包钳 0)+ combo/familyStreak 清零 + 词自动收藏(SRS 新学档)
- 答错弹窗:仅当「词是新收入生词本」且 wrong_notify=true 时弹(GamePrefs WRONG_NOTIFY,默认开);
  勾选「下次不用提醒」→ setWrongNotify(false),已在生词本的词答错不弹不重复收藏
- 锈词 = srs 非空 && dueAt ≤ now && stage < GRADUATED:无论答对与否收益固定 +2、连击清零
- 金币仍无消费出口(候选:护盾/能量道具,未立项)
- 每日目标:Mine 卡片预设 20/50/100/200(GamePrefs DAILY_GOAL,默认 50),SPINS_TODAY 按
  DAY_STAMP(LocalDate epochDay)自然日归零,卡片显示今日进度条(达成变绿);仅统计转动,复习/测验不计数

**v1 时的四大用户反馈(P1~P4)已全部闭环:** 布局(M7)、发音/音标(M9+M10)、构词展示(M8)、配色+明暗主题(M6)。
滚动条胶囊:新构建未复现,判定为模拟器瞬时浮层(见 AGENTS.md 已知坑 #8 同类),关闭。

## 剩余工作(仅剩可选打磨)

1. **~~真机验收~~ ✅ 2026-10-04 用户确认:真机发音自然度通过(Piper amy),项目验收完成。**
2. **词库扩展(全考试受众)**:规格见 [词库扩展-数据规格.md](词库扩展-数据规格.md)。
   **阶段一(架构贯通)已完成 ✅ 2026-10-05**(M16);剩:
   - **阶段二·扩库重跑**:词根表扩 500+(每根含义+≥3 库内例词,两道人工审)→ 全库重跑拆词 →
     分考试覆盖率报告(目标全库组合词率 ≥40%,现 CET-4 36%);
   - **阶段三·全考试收尾**:cet6/kaoyan/ielts/toefl/gre 池放开(GamePrefs.EXAM_CHOICES 加短码即可,
     exams.json 已含全部标签,但新词只拉过 gk——需把 build_dict.py NEW_WORD_TAGS 放开重跑)、
     IPA 兜底源完善(新词现 339/424)、释义来源标记统计(gs 字段未落)。
3. **例句后续批次 s02+**:250/6710 已入库,余 ~6460 词按批产出(assets/sentences/,每批 250,
   产完拷 App assets 并递增 Repository.ASSETS_VER)。
4. **可选打磨(有想法再做)**:3989 条新释义抽检错别字;
   缺音标的 113 词人工补录(tools/ipa_missing.txt);若发新版可打 v1.0.1 tag + release。

**M16 实施要点(动考试池/词库相关时看):** 考试集合唯一真源是 GamePrefs EXAMS(stringSet,默认/兜底
{"cet4"}),Repository 单例 init 里 collect 成 StateFlow `_exams` 并暴露 `exams`;过滤统一走
`wordInPool(WordEntity)`(examTags 拆包裹式 ",a,b,",null/空按 cet4)。新考试接入三步:
① build_dict.py 的 NEW_WORD_TAGS/TAG_MAP 放开对应短码重跑管线;② GamePrefs.EXAM_CHOICES 加
展示项;③ 递增 ASSETS_VER(若 assets 有变)。预填/补插路径:空库走 ensurePrefilled(唯一预填入口,
**onCreate 预填已删——双路径并发会翻倍自增主键表**);老库加行走 enrichIfNeeded 的 insertIgnore
(words.json 有而库里无的行),闸门 = ASSETS_VER。图鉴族 count 是过滤后动态值(f.copy),
别再信 families 表里的静态 count 做池内统计。

**M13 实施要点(改经济/玩法相关时看):** 结算已从 spin() 挪到 settleQuiz()(SlotViewModel):spin 只抽词+判锈词(pendingRust)+建轴;停稳后 buildQuizForCurrentWord() 出题(干扰项 = glossedWords 排自身去重抽 3,gloss 缺失自动按跳过结算);answerQuiz/skipQuiz → settleQuiz(correct: Boolean?)。quiz/quizPicked/quizResult/lastRust 均为 VM 状态;quiz 保留到下次 spin 才清(供 UI 变色),SPIN 与详情入口以 `quiz != null && quizPicked == null` 判禁用。锈词在 settle 各分支优先于 combo 结算。SPIN 显隐 = SlotScreen 的 spinShown(LaunchedEffect:spinning/出题未答→隐藏;答完 delay 1600ms→浮现;AnimatedVisibility fade+scale 0.55,enter 用弹簧)。

**M12 实施要点(改视觉/动效相关时看):** 机台硬件 token 在 Theme.kt(cabinet/reelPaperHi/Lo/reelInk/bulbDim);
字体 res/font(bungee_regular.ttf、dseg7_classic_bold.ttf,OFL,License 附表已登记)→ `FontBrand`/`FontLed`;
跑马灯 BulbStrip 在 SlotScreen.kt(InfiniteTransition 驱动 Canvas,仅老虎机页组合,无常驻全局开销);
停轴三段动画在 Reel.kt(回拉 70ms → 主 tween → spring 过冲回弹,onStopped 在回弹稳后);
推入页/Tab 转场在 MainActivity.kt(AnimatedContent;推入时旧页 exit=ExitTransition.None 实现「盖住」);
按压缩放助手 `Modifier.pressScale(interactionSource)` 在 ui/theme/Press.kt。
SPIN 按钮坐标随布局变化:1080×2400 模拟器实测 ≈ `input tap 540 1784`(AGENTS.md 旧值 2118 已失效)。

**M10 实施要点(改发音相关时看):** AAR 在 app/libs/(files() 依赖);模型 assets/tts/
(onnx 63MB fp32——**fp16 与内置 CPU onnxruntime 类型不兼容会原生 abort,勿用**);
espeak-ng-data 首启拷 filesDir(PiperEngine.copyEspeakData,幂等);generation 计数打断旧播报;
语速换算 piper speed=rate/0.85;ABI 只留 arm64-v8a+x86_64;GitHub 大文件下载必须 curl 重试+校验尺寸。

**M9 实施要点(改音标相关时看):** tools/build_ipa.py 读 tools/cmudict.dict.cache(0.7b,**Latin-1** 编码;
urllib 下载 GitHub 会失败,需 curl 手动下载,镜像清单在 SOURCES);音标按词原样大小写存(April 为 key),
查找时 lower();重音符按音节 onset 回退插入;AH0→ə、ER0→ər;连字符词分段查后直接拼;
缺词界面隐藏音标行。DB v5 = words 表加 ipa 列。

## 仓库

- 远程:**https://github.com/amazing1102/RootSlots.git**(main 已推送,2026-10-05);
- 推送走本机代理(git 全局已配 127.0.0.1:7897,Clash 需开启);onnx 模型 60MB 超推荐值被警告,
  后续若频繁改动模型可考虑 Git LFS。

## 发布(v1.0.0)

- release 包:`RootSlots/app/build/outputs/apk/release/app-release.apk`(137MB,R8+资源收缩,版本 1.0.0/build 2);
  构建命令:`cd /d/CET4/RootSlots && JAVA_HOME=D:/Java/jdk-17.0.18 /d/gradle-8.7/bin/gradle.bat assembleRelease`
- 签名:`RootSlots/release.keystore` + `RootSlots/keystore.properties`(均不入库,**备份这两个文件**,
  密码在其中;丢了就无法出同签名更新);模拟器已验证 release 下 Piper/合成/播放全链路正常。
- v1.0.0 新增:测验第四模式「看音标选词」+ 答后揭示单词释义;「我的」页复习统计卡
  (近 7 天复习量柱状图 + 记忆曲线达成率环,数据来自 DB v6 review_logs 表,自启用起累积)。
- R8 规则:app/proguard-rules.pro keep `com.k2fsa.sherpa.onnx.**`(JNI 按名反射,勿删)。

## 新会话恢复步骤

1. 读本文件 + [AGENTS.md](AGENTS.md) + [词根老虎机-计划书.md](词根老虎机-计划书.md) v2(问题登记表 P1~P7、设计决策 §四)。
2. 启动模拟器 RootSlots(API 35),构建安装(命令见 AGENTS.md),确认 App 可跑。
3. 从"剩余工作"第 1 项继续;**不要重新询问需求决策**(玩法/词库/功能/技术选型均已拍板,见计划书)。
4. 完成节点后:更新 计划书 状态列 → 更新本文件 → git commit。

## 一期明确不做(勿自行扩展)

账号/云同步/排行榜;iOS;假词押注彩蛋(二期);词根词缀编辑器;老虎机转非组合词(整词走详情卡,见计划书 §4.3)。
