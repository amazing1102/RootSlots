# 贡献指南

感谢关注 RootSlots!这是一个**完全离线、无账号、无后端**的 Android 单机单词应用,
数据管线(Python)+ Android App(Kotlin/Compose)双仓同源。

## 先读什么

| 文档 | 内容 |
|---|---|
| [README.md](README.md) | 功能总览与技术栈 |
| [AGENTS.md](AGENTS.md) | 环境路径、构建命令、**踩坑速查(高频)** |
| [HANDOFF.md](HANDOFF.md) | 当前进度、待办、实施要点 |
| [词根老虎机-计划书.md](词根老虎机-计划书.md) | 里程碑与设计决策(改玩法先看) |

## 项目红线(提案前必读)

- 保持**完全离线**:不加网络权限、不加后端、不加账号
- 不做:云同步 / 排行榜 / iOS / 词根词缀在线编辑器
- 金币经济红线:不卖答案、不卖盲盒、不卖"能量"(见 金币消费出口-设计方案.md)
- 英语数据(词表/释义/例句)仅供学习交流,不引入受限版权的商业词库

## 本地构建

```bash
# 1. 数据管线(改了 md 源表 / 释义 / 例句批文件后需重跑)
python tools/build_ipa.py && python tools/build_gd.py && python tools/build_dict.py \
  && python tools/build_data.py && python tools/verify_assets.py   # 校验器须 0 错误
cp assets/{words,morphs,families,combos}.json RootSlots/app/src/main/assets/

# 2. Android App(需 JDK 17;wrapper 已修复,直接 ./gradlew;Windows 本机也可用 gradle.bat)
cd RootSlots && ./gradlew assembleDebug
```

CI 会在每次 push/PR 时自动构建 debug APK(.github/workflows/android-build.yml)。

## 提交约定

- 提交信息用中文,格式:`<模块/里程碑> <做了什么>(<关键证据/数字>)`
  例:`M19c 好友PK增强包:战书二维码(zxing生成/保存相册/相册识别)+ 宿敌指名(双模拟器实测)`
- **每个改动必须实测**:UI 改动附模拟器截图;数据改动跑 `verify_assets.py`;
  DB 升版必须写真迁移(禁止破坏性迁移,会清用户收藏/SRS)
- 里程碑完成后同步更新 [HANDOFF.md](HANDOFF.md) 与计划书状态列
- 新增第三方资源(字体/模型/数据)时,在 [LICENSE](LICENSE) 附表登记许可

## 数据贡献(词库/例句)

- 释义批文件:放 `assets/glosses/*.json`;例句批文件:放 `assets/sentences/sNN.json`
  (`{word: [英文, 中文]}`),写完用 `tools/validate_batch.py` 校验
  (键集/词在句中/查重/中文必填)
- 词根词缀数据改 `cet4_roots_affixes.md` 或 `tools/roots_stage2.py`,重跑管线即可全量重建

## 提交 PR

- 用仓库自带 PR 模板逐项勾选(实测证据 / 校验结果 / DB 迁移确认)
- 大的玩法或架构改动,建议先在 Issue 里对齐方案(可参考 `*-设计方案.md` 的历史)
