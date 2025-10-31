# My_Modules

一个用于试验与验证现代 Android 模块化架构的实验项目，包含一个完整的老虎机玩法样例。项目采用分层与模块化设计，将 UI、领域算法、数据访问与数据库预置生成进行解耦，方便独立演进与替换。

## 模块结构

- `app`: 应用入口（Hilt、Compose 主题、挂载 `feature_slot`）
- `feature_slot`: 功能模块（UI + ViewModel + Flow），展示老虎机画面、发起 `spin()`
- `lib_slot_data`: 数据层（Repository + Mapper），从数据库读取预生成盘面并转为领域模型
- `lib_database`: 数据库存储（Room + Hilt Provider），通过 `assets/slot_results.db` 预置数据
- `lib_slot_core`: 领域/算法核心（符号系统、连线、规则引擎、结果盘生成器）

## 技术栈

- Kotlin、Kotlin Coroutines + Flow
- Jetpack Compose（UI）
- Hilt（依赖注入）
- Room（本地数据库，预置库导入）
- KSP（Hilt/Room 编译期处理）
- 版本管理：`gradle/libs.versions.toml`

---

## 老虎机核心算法

核心算法位于 `lib_slot_core`，职责包括：
- 符号系统与赔率表（领域模型）
- 连线系统（20 条 paylines）
- 赢分计算引擎（规则引擎）
- 结果盘批量生成器（离线预生成，落库到 SQLite）

### 1. 符号系统与赔率

来自需求文档的金币符号及赔率（MG、FG通用），3/4/5 连分别对应赔率（均为 5 的倍数）。实现中使用密封类 `SlotSymbol` 建模：
- `GoldCoin(name, payouts: Map<Int, Int>)`：10 个金币符号，支持 3/4/5 连赔率
- `Wild`：万能符号，可替代任意金币
- `GreenBill`、`BonusGame`、`FreeGame`：不参与连线计算（用于盘面填充与扩展玩法）
- `Empty`：生成过程中的占位符

静态配置集中于 `GameConfiguration`：
- `goldCoinSymbols`: 10 个金币符号与赔率表
- `winningSymbols`: 金币 + Wild（参与连线判定）
- `allSymbols`: 所有可能在盘面出现的符号

### 2. 连线系统（Paylines）

- 固定 20 条连线，每条连线由 5 个有序坐标点组成，坐标系采用列 `x ∈ [0..4]`、行 `y ∈ [0..2]`
- 规则：
  - 从左到右连续排列，中间不可中断
  - 至少 3 连才算中奖
  - 仅按最高连线赔付（同一条线不叠加 3/4/5）

实现：`Payline.allPaylines` 定义 20 条线路，覆盖直线、V 形、倒 V、Z 字等常见样式。

### 3. 赢分计算（规则引擎）

- 输入：5×3 盘面与总押注 `totalBet`
- 流程：
  - 遍历 20 条 paylines，抽取该线的 5 个符号
  - 首符若非金币/非 Wild，则该线不中奖
  - 确定“实际中奖符号”：线上的第一个金币符号；若整条线没有金币（只有 Wild/非参与符号），不中奖
  - 从左到右统计连续匹配数（金币同名或 Wild 皆可），`consecutiveCount >= 3` 即中奖
  - 取该金币在 3/4/5 连下的赔率 `payout`，计算单线奖励：`lineWin = (totalBet × payout) ÷ 20`
  - 汇总所有中奖线得到 `totalWinAmount`
- 输出：`SpinResult(grid, winningLines, totalWinAmount)`

与文档一致性：
- 公式与示例吻合
- 最高连线赔付，Wild 仅作替代

### 4. 结果盘生成器（离线预生成）

为满足“中奖倍率为 5 的倍数”与运行期性能需求，提供离线生成器（`lib_slot_core` 内 JVM main）批量生成不同“目标赔率”的有效盘面写入 SQLite 表 `slot_results`，App 侧通过 `assets/slot_results.db` 预置导入。

- 目标赔率：5..10000（步长 5）
- 关键参数：
  - `TOTAL_BET = 2000L`（生成与运行期一致）
  - 分层策略 `GenerationStrategy`：按目标赔率选择组合规模与每倍率最大盘面数
  - 熔断控制：`MAX_ATTEMPTS_PER_COMBINATION`、`MAX_SOLUTIONS_TO_FIND_PER_PAYOUT`
- 思路：
  1) 基于赔率表枚举“理论赢奖组合”，按赔率降序以利剪枝  
  2) 搜索可凑成目标赔率的组合集合  
  3) 回溯将若干组合摆放到 5×3 网格，允许小概率以 `Wild` 替代金币（禁止全 Wild）  
  4) 未命中格子用非连线符号随机填充（`GreenBill/Bonus/FreeGame`）  
  5) 用规则引擎复算校验最终赔率，匹配则写库  
  6) 将网格序列化入库，并建立 `payout` 索引
- 数据规模（估算）：
  - 约 20 万条记录、约 20MB，可直接作为 App 资产

---

## 运行期数据流

1) `feature_slot` 的 `SlotViewModel.spin()` 随机挑选“目标赔率”，调用 `SlotRepository.getResultByPayout(payout)`  
2) `lib_slot_data` 在 `SlotResultDao` 随机取一条匹配记录  
3) 反序列化并用 `SlotMachine.calculateWinnings()` 复算中奖线，返回 `SpinResult`  
4) UI 将领域模型映射为 `UiSlotItem`，高亮中奖位置并展示统计

---

## 与需求文档的对应

- 已实现
  - 金币 + Wild 连线，3/4/5 连、左到右、不间断、最高赔付
  - 赔率表、赢分公式与示例
  - 目标倍率的组合拆分思想以“预生成器”实现
- 待扩展
  - RTP 驱动的“是否中奖 + 倍率范围”策略
  - 绿钞（额外奖励）、BG、FG 的完整判定与结算通道（现阶段作为填充符号）

---

## 构建与运行

- Android：构建 `app`，Hilt 注入，Room 通过 `createFromAsset("slot_results.db")` 导入预置数据
- 生成器（离线）：运行 `lib_slot_core` 的 JVM main，产出 SQLite 写入 `lib_database/src/main/assets/slot_results.db`

建议：
- 保持 `TOTAL_BET` 一致（当前生成与运行期均为 2000）
- 如需扩充倍率或盘面数量，可考虑资产分包或在线下发

---

## 测试建议

- 引擎单测：`SlotMachine.calculateWinnings`（正、反例；全 Wild；无金币；混合连线）
- 反序列化：非法符号名与维度校验
- DAO：按 `payout` 的随机取样与空结果处理
- 端到端：固定盘面与赔率，校验 UI 高亮与统计数据


