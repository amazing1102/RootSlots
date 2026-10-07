# HANDOFF.md — 交接快照(会话恢复入口)

> 最后更新:2026-10-07(M19c 好友PK 增强包完成:二维码互传 + 宿敌指名战书;M18 金币出口 + M19 好友PK 全线收官)。
> 每完成一个节点,更新本文件并 git 提交。需求决策与设计细节见 [词根老虎机-计划书.md](词根老虎机-计划书.md) v2。

## 当前状态(2026-10-07)

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
| M17 | 词库扩展阶段二+三·扩库重跑与全考试收尾(2026-10-06):**阶段二**——roots_stage2.py 词根表 437→545(数据驱动挖矿:未拆词剥前后缀统计高频核心,人工审定 171 候选、拆词器自动验证例词≥3 保留 107);**自动拆词器** auto_split(无图谱新词:P?+R(+连接元音)+S?S?,词根≥4 字母、派生后缀白名单、黑名单+人工 override);**阶段三**——build_dict 全 7 考试拉新词 7948(屈折吸收 787),超集 14653;build_ipa 扩全词表(CMU 主源)IPA 98.1%;gs 释义来源标记;**结构表升级刷新**:morphs/families/combos 清表重灌(fillStructTables,派生数据不含用户数据);EXAM_CHOICES 7 类全放开 | 已实测:升级 newWords=7948 插入+结构表刷新,7 池词数/组合数正确(toefl 3146 词/1521 组合),收藏/SRS 无损,托福池转出 remiss(re+miss)且 mit 族显示 29 词(池扩展生效);抽检 100 词错误率 ≤2%(黑名单沉淀 money/honey/render/colonel/astray 等 13 词);IPA 98.1% 达标;词根表 545/词族 505 |
| 数据 | **释义 14653/14653 全覆盖**(存量自策 + 新词 ECDICT)+ **例句 6710/6710**(存量 6710 词全量;扩库新词按批追加)+ **音标 14372/14653(98.1%)**;组合词 3730(校验 0 错误);词根表 545(含阶段二新根 107)/词族 505;考试池:7 类全通 |
| M19a | **好友 PK·异步战书:数据层 + 下战书**(2026-10-06,两份设计方案评审通过后实施,好友PK 先行):Room **v7→v8 真迁移**新增 duels 表(role/code_id/opponent/bet/quiz_json/成绩/status,纯新增);ChallengeCodec 战书码(JSON→Deflate→Base64URL,`RS1.<payload>.<crc32hex>`);题目自包含入码(释义×4+拼写×3 交替,固化跨版本不变);挑词三来源(生词本薄弱/词根族/随机);下战书 flow(挑词→押注+署名→基准战 7 题 15s/题→生成码);托管/作废退回/7 天超时退回;测验页入口卡。已实测:双模拟器互传码解析全还原、托管/退回金币逐分吻合、防重导入、迁移无损;截图 tools/shots/m19a/ |
| M19b | **好友 PK:应战答题 + 幽灵进度 + 回执结算 + 战绩**(2026-10-06):duels.quiz_json 改存**全量 JSON {q,my{s,ms,seq}}**(seq=下战书方每题对错序列,幽灵数据随码走,兼容旧纯 q 格式);**应战流**:received 卡「开始作答」→ BattlePane(标题"应战 n/7" + 幽灵行「👻 对手 已对 x/7 · 你 y/7」按 seq 与题位实时递增,旧码无 seq 退化为目标线)→ finishDefense 本端即时结算(胜 +2×bet/平退 bet/负不退)→ status=settled + **回执码**(85 字符);**结算流**:challenger 导入回执(parseAny 自动识别战书码/回执码)→ settleWithReceipt(applyReceipt 回填对方成绩+划转);胜负判定 judgeWin=比分→平分比用时(短者胜)→完全相同平;**战绩卡**(「我的」页):总场次/胜率/明细/当前连胜(duelStats,settled 记录按 settledAt 倒序)。已实测(双模拟器全流程):B 应战托管 200→170→答题(幽灵数字随题位递增)→2/7 平分但用时更长→惜败押注没收;A 导回执战局胜利 +60→273;零和核验 A+30/B−30 精确;双方列表胜/负徽章+成绩;战绩卡 1 场 0胜0平1负;截图 tools/shots/m19b/。**修的坑**:应战成功后列表未刷新(onChanged 漏调);settled 成绩不齐(旧数据)显示"平"误导→改"已结算" |
| M18a | **金币消费出口·宿敌对决**(2026-10-07,方案已评审通过):lapses≥3 未毕业的词 = **宿敌**(已降服词再忘 NEMESIS_REVISIT_GAP=3 次 → 再临,筹码×2 多一局听音);生词本顶部**通缉令卡**(☠ 词+悬赏等级+嘲讽台词模板池 3 条 hash 轮换+入场费);**入场筹码** = 20+10×(悬赏−1),再临×2;**三局 Boss 战**(全围绕该词:释义 4 选 1 → 例句挖空(sen_en 词面命中才挖,否则退化看词选释义)→ 拼写补全;再临加听音局);**三局全胜 = 降服**:筹码翻倍返还 + 🏅 降服勋章(DataStore stringSet "word:lapsesAtDefeat",详情页大词旁+生词本行内永久标记)+ SRS stage+3(封顶 1天..30天档不毕业);**任一局败/弃战(系统返回)** = 筹码没收 + 词回 5 分钟档;零 DB 迁移(nemesis 状态 = srs.lapses + DataStore)。已实测(单机玩法,A 端全场景):event 3 次没记住→通缉令(悬赏 3 级);对决三局全胜 → +80 翻倍(270−40+80=**310 精确**)+ 5分钟档→1天档 + 🏅 + 通缉令消失;pretense 中途答错 → 💀 筹码没收(270 精确)+ 词回 5 分钟档;event 再忘 3 次(lapses=6)→ 通缉令「再临·筹码×2」入场 140(=70×2 精确);台词模板轮换;详情页勋章;截图 tools/shots/m18a/。**修的坑**:Overlay.Nemesis 首帧 nemesisOf 未加载 → LaunchedEffect(Unit) 立即关推入页 = "点击无反应"(加载期间须显示空白,确认 missing 才退回);对决页底部按钮 y≈2060(input tap 别用截图估算,Button 底缘 ~2130) |
| M18b | **金币消费出口·定向转轴**(2026-10-07):30 币许愿一个词根族,**下一转必出该族词**(一次性,不可叠加,24h 未用惰性失效退币);老虎机 HUD 金币芯片旁 **🎯 许愿入口 chip**(未许愿灰金 / 已许愿金色高亮带族名,转轴消费后恢复);**机柜微光**(许愿激活时机柜描边 animateColorAsState 转金色);WishScreen 推入页(搜索 + 族列表按 count desc + 「许愿 −30🪙」/「已有许愿·不可叠加」);Repository:GamePrefs WISH_FAMILY/EXPIRE 持久化(App 被杀不丢)、randomCombo 许愿分支(族池按当前考试池过滤,池空兜底普通抽取保许愿)、spin() 落定后 consumeWishIfMatches 消费。已实测:许愿扣 30(273→243 精确)→ SPIN 转出 **extension ∈ tend 族** → chip 恢复灰金(消费);「已有许愿(tend)· 不可叠加」按钮态;截图 tools/shots/m18b/。过期退币为惰性路径(24h),代码走查覆盖 |
| M18c | **金币消费出口·机台改装**(2026-10-07):纸面(默认)/ **鎏金 800** / **霓虹 2000** 三款皮肤,**纯外观零数值**——Theme.kt `applySkin(base, skin, dark)` 只覆盖机台硬件 token(cabinet/reelPaperHi/Lo/reelInk/bulbDim),语义色不动,明暗主题各有适配;「我的-外观」新增**机台改装卡**(三行:使用中金框 / 已拥有·点击换上 / 价格,余额不足购买静默不生效);购买即换上(buySkin:已拥有/余额不足 false,扣款+owned+setSkin);RootSlotsTheme(themeMode, skin),皮肤/已购走 GamePrefs SKIN/SKINS_OWNED 持久化。已实测:买鎏金 −800(2000→**1200 精确**)机柜转黄铜滚筒米金即时生效;1200 点霓虹(2000)不扣款不切换(**余额不足购买不生效**);点纸面(已拥有)切回 + force-stop 重启后保持(**切换即时生效+重启持久化**);COINS_START 临时 2000/5000 验购买后**已改回 200**;霓虹深夜紫机柜+暗滚筒荧光青墨;截图 tools/shots/m18c/ |
| M19c | **好友 PK 增强包**(2026-10-07,M19 收官):**①战书二维码**——zxing core 3.5.3(新增唯一三方依赖,纯 Java 无权限)接入 ChallengeCodec(toQrBitmap ECC-M 白边 / decodeQr 小图 ×3 放大+TRY_HARDER);生成页显示二维码 + 「保存二维码到相册」(MediaStore Pictures/RootSlots,API29+ 免权限,26-28 由 manifest maxSdkVersion 权限覆盖);打战书页加「📷 从相册识别」(GetContent 选图 → IO 解码 → 自动填入+解析,失败提示改用文本码)。**②宿敌指名战书**——挑词来源第 4 项「⚔ 宿敌」:候选 = 当前宿敌词(悬赏排序)+ 生词本薄弱补齐至 7,说明「把折磨你的宿敌词发给正被同一个词折磨的朋友」。**③HMAC 跳过**:离线密钥协商机制未设计(需配对协议),朋友威胁模型下 CRC 校验已够,文档登记。已实测(双模拟器):A 生成战书(基准 3/7·37.5s)→ 二维码显示 + 保存相册(文件落盘 Pictures/RootSlots/)→ adb 传图至 B(模拟微信发图)→ B 相册识别 → zxing 解码自动填入 → 解析出预览(VS 无名氏·3/7·37.5s·7 词)→ 应战成功落库;宿敌(3 轮没记住造出)进入指名候选首位,不足 7 与其它来源一致禁用提示;截图 tools/shots/m19c/。**坑**:挑战/详情等推入页无 Tab 栏,固定 Tab 坐标(540/648,2220)会打空——**推入页内先 BACK 再切 Tab;测验 Tab 真实坐标 (761,2211) 非 648** |

## 经济数值(M13 后,改玩法先看)

- 基础奖励:新词/未逾期 10,已毕业 15;×(1 + min(combo,20)×0.05) + 同族连击 5×(n−1);答对 ×1.5,跳过 ×0.5
- 答错:−3🪙(钱包钳 0)+ combo/familyStreak 清零 + 词自动收藏(SRS 新学档)
- 答错弹窗:仅当「词是新收入生词本」且 wrong_notify=true 时弹(GamePrefs WRONG_NOTIFY,默认开);
  勾选「下次不用提醒」→ setWrongNotify(false),已在生词本的词答错不弹不重复收藏
- 锈词 = srs 非空 && dueAt ≤ now && stage < GRADUATED:无论答对与否收益固定 +2、连击清零
- 金币消费出口:**M18 全部上线(2026-10-07)**——宿敌对决(入场 20+10×(悬赏−1),胜翻倍/败没收)
  + 定向转轴(30 币许愿族,一次性)+ 机台改装(鎏金 800/霓虹 2000 纯外观);战书押注托管为零和出口(M19)
- 每日目标:Mine 卡片预设 20/50/100/200(GamePrefs DAILY_GOAL,默认 50),SPINS_TODAY 按
  DAY_STAMP(LocalDate epochDay)自然日归零,卡片显示今日进度条(达成变绿);仅统计转动,复习/测验不计数

**v1 时的四大用户反馈(P1~P4)已全部闭环:** 布局(M7)、发音/音标(M9+M10)、构词展示(M8)、配色+明暗主题(M6)。
滚动条胶囊:新构建未复现,判定为模拟器瞬时浮层(见 AGENTS.md 已知坑 #8 同类),关闭。

## 剩余工作(仅剩可选打磨)

0. **~~好友 PK(异步战书)~~ ✅ 2026-10-07 全部完成**:
   - **M19a 数据层+下战书 ✅ 2026-10-06**(duels 表 v8 真迁移 / ChallengeCodec / 挑词三来源 /
     下战书 flow / 基准战 / 托管 / 作废退回 / 双模拟器互传实测);
   - **M19b 应战+幽灵+回执+结算 ✅ 2026-10-06**(quiz_json 全量 {q,my{seq}} / 幽灵进度条 /
     finishDefense 即时结算 / 回执码 85B / parseAny 自动识别 / settleWithReceipt 划转 /
     我的页战绩卡;双端全流程:胜负判定含"平分比用时"分支、零和划转 A+30/B−30 精确);
   - **M19c 增强包 ✅ 2026-10-07**(二维码互传:生成页保存相册 + 打战书页相册识别 zxing 解码;
     宿敌指名战书第 4 挑词来源;HMAC 跳过——离线密钥协商未设计,朋友威胁模型 CRC 已够);
   方案全文:[好友PK-异步战书-设计方案.md](好友PK-异步战书-设计方案.md)(已标全部完成)。
   实施提示: 二维码走 zxing core 3.5.3(项目唯一新增三方依赖);平局/过期退币为纯函数/惰性路径,
   代码走查覆盖(M19b 已实测同域分支)。
1. **~~金币消费出口(M18)~~ ✅ 2026-10-07 全部完成**:M18a 宿敌对决 + M18b 定向转轴 + M18c 机台改装
   (方案见 [金币消费出口-设计方案.md](金币消费出口-设计方案.md),红线:不卖答案/盲盒/能量;
   零 DB 迁移,宿敌勋章/许愿/皮肤全走 DataStore)。二期探索:词根当票(方案 §6)未立项。

1. **~~真机验收~~ ✅ 2026-10-04 用户确认:真机发音自然度通过(Piper amy),项目验收完成。**
2. **词库扩展(全考试受众)**:规格见 [词库扩展-数据规格.md](词库扩展-数据规格.md)。
   **阶段一(架构贯通)已完成 ✅ 2026-10-05**(M16);剩:
   - **~~阶段二·扩库重跑~~ ✅ 2026-10-06**:词根表 437→545(roots_stage2.py 新根 107 个保留,
     每根含义+≥3 库内例词由拆词器自动验证);全库重跑+分考试覆盖率(见下 M17 行);
   - **~~阶段三·全考试收尾~~ ✅ 2026-10-06**:7 考试池全放开(NEW_WORD_TAGS 全量重拉,
     扩库新词 7948,超集 14653);IPA 14372/14653=98.1%(CMU 全词表+EC 兜底)≥98% 达标;
     gs 释义来源标记已落 words.json;词池数与 ECDICT 标签数一致(同源);
3. **~~例句批次~~ ✅ 2026-10-06 全量完成**:6710/6710 词全覆盖(s01–s09 组合词 2278,s10–s27 非组合词 4432,
   每句程序化校验:键集/词在句中/查重/中文)。ASSETS_VER=20261006a。后续新增词条时按批追加即可。
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

## 发布(v1.0.1 现行 / v1.0.0 历史)

- **v1.0.1 已发布 ✅(2026-10-07)**:tag `v1.0.1`,release 含 `RootSlots-v1.0.1.apk`(138.8MB,
  版本 1.0.1/build 3);release 页 https://github.com/amazing1102/RootSlots/releases/tag/v1.0.1
- release 包:`RootSlots/app/build/outputs/apk/release/app-release.apk`(R8+资源收缩,含 zxing);
  构建命令:`cd /d/CET4/RootSlots && JAVA_HOME=D:/Java/jdk-17.0.18 /d/gradle-8.7/bin/gradle.bat assembleRelease`
- 发版流程(无 gh CLI 时):git tag → push tag → GitHub API 建 release(POST /releases,payload 存
  tools/shots/relpayload.json 样式)→ 上传 APK(POST uploads.github.com/.../assets?name=...,
  139MB 走 Clash 代理约数分钟,curl -m 900 --retry 2);token 从
  `printf "protocol=https\nhost=github.com\n\n" | git credential fill` 取(gho_ 开头,repo scope)
- 签名:`RootSlots/release.keystore` + `RootSlots/keystore.properties`(均不入库,**备份这两个文件**,
  密码在其中;丢了就无法出同签名更新);模拟器已验证 release 下 Piper/合成/播放全链路正常,
  v1.0.1 追加冒烟:release 版启动 + 战书生成页**二维码正常渲染**(zxing 过 R8)。
- v1.0.1 新增:好友PK异步战书(战书码/二维码/幽灵进度/零和结算)+ 金币消费出口(宿敌对决/定向转轴/
  机台改装)+ 词库 14653 词(7 考试池)+ 例句 6710 全覆盖 + 学习日历/遗忘进度条/目标考试多选。
- v1.0.0(v1.0.0/build 2,137MB):测验第四模式「看音标选词」+ 复习统计卡(DB v6 review_logs)。
- R8 规则:app/proguard-rules.pro keep `com.k2fsa.sherpa.onnx.**`(JNI 按名反射,勿删);
  zxing 无反射路径,实测过 R8 无需额外 keep。
- 仓库元数据:描述/homepage 指向最新 release,topics 20 个(含 cet4/cet6/gre/toefl/ielts/word-roots/
  morphology/offline-first 等;GitHub 上限 20)。

## 新会话恢复步骤

1. 读本文件 + [AGENTS.md](AGENTS.md) + [词根老虎机-计划书.md](词根老虎机-计划书.md) v2(问题登记表 P1~P7、设计决策 §四)。
2. 启动模拟器 RootSlots(API 35),构建安装(命令见 AGENTS.md),确认 App 可跑。
3. 从"剩余工作"第 1 项继续;**不要重新询问需求决策**(玩法/词库/功能/技术选型均已拍板,见计划书)。
4. 完成节点后:更新 计划书 状态列 → 更新本文件 → git commit。

## 一期明确不做(勿自行扩展)

账号/云同步/排行榜;iOS;假词押注彩蛋(二期);词根词缀编辑器;老虎机转非组合词(整词走详情卡,见计划书 §4.3)。
