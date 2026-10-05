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
| 数据 | **释义 6286/6286 全覆盖**(glosses/c01–c19 共 19 批)+ 音标 6173 词;组合词 2278(校验 0 错误) |

**v1 时的四大用户反馈(P1~P4)已全部闭环:** 布局(M7)、发音/音标(M9+M10)、构词展示(M8)、配色+明暗主题(M6)。
滚动条胶囊:新构建未复现,判定为模拟器瞬时浮层(见 AGENTS.md 已知坑 #8 同类),关闭。

## 剩余工作(仅剩可选打磨)

1. **~~真机验收~~ ✅ 2026-10-04 用户确认:真机发音自然度通过(Piper amy),项目验收完成。**
2. **可选打磨(有想法再做)**:3989 条新释义抽检错别字;
   缺音标的 113 词人工补录(tools/ipa_missing.txt);若发新版可打 v1.0.1 tag + release。

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
