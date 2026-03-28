# 测试报告

## 验证命令
- `./mvnw -q -DskipTests package`
- `cd docker && ./start.sh`
- `cd yu-image-search-mcp-server && java -jar target/fit-image-search-mcp-server-0.0.1-SNAPSHOT.jar`
- `./mvnw spring-boot:run`
- `curl -sS -X POST 'http://localhost:8123/api/api/fitness/workflow/execute' --data-urlencode 'userInput=你好' --data-urlencode 'userId=1'`
- `curl -sS -N -G 'http://localhost:8123/api/api/fitness/workflow/execute/stream' --data-urlencode 'userInput=你好' --data-urlencode 'userId=1' -H 'Accept: text/event-stream'`
- 清理：
  - `kill 70965 70677`
  - `cd docker && docker compose stop`

## 结果
- Maven 编译通过。
- Docker 依赖成功启动，`fitness-mysql` 进入 `healthy`。
- 图片搜索 MCP 服务成功监听 `8127`。
- 主应用成功监听 `8123`，并完成 DB MCP 与图片 MCP 初始化。
- 额外复测确认：
  - `fitness_db.user_profile` 中存在 `id=1`
  - `training_record` 中存在 2 条 `user_id=1`
  - `user_injury` 中存在 1 条 `user_id=1`
- 额外日志复核确认：
  - `getUserProfileById({"userId":1})` 实际返回用户画像
  - `getUserTrainingRecords({"userId":1})` 实际返回训练记录
  - `UserProfileAgent` 在本轮复测中实际生成 `DATA_STATUS: GROUNDED`
- 同步接口实际返回 JSON：
  - `status=completed`
  - `intent=chat`
  - `node=chat`
  - `result=你好呀！...`
- 同步接口原始返回：
```json
{"status":"completed","userInput":"你好","userId":1,"intent":"chat","node":"chat","profileStatus":null,"result":"你好呀！\uD83D\uDC4B 很高兴见到你～  \n今天有想开始的健身小目标吗？\uD83D\uDCAA  \n","message":"工作流执行完成"}
```
- 流式接口实际返回的 SSE 顺序为：
  - `metadata(workflow started)`
  - `metadata(chat routed)`
  - `metadata(chat streaming)`
  - 多个 `token` 事件
  - `done`
- 实测 token 事件逐段返回，例如 `你好`、`呀！👋`、` 很高兴`、`见到你～`，说明当前不是一次性把完整结果塞进单个结果事件。
- 流式接口原始返回：
```text
event:metadata
data:{"event":"metadata","node":"workflow","status":"started","sequence":1,"done":false,"message":"工作流开始执行"}

event:metadata
data:{"event":"metadata","node":"chat","status":"routed","sequence":2,"done":false,"message":"识别意图：chat","intent":"chat"}

event:metadata
data:{"event":"metadata","node":"chat","status":"streaming","sequence":3,"done":false,"message":"开始流式生成回复","intent":"chat"}

event:token
data:{"event":"token","node":"chat","status":"streaming","sequence":4,"done":false,"intent":"chat","content":"你好"}

event:token
data:{"event":"token","node":"chat","status":"streaming","sequence":5,"done":false,"intent":"chat","content":"呀！\uD83D\uDC4B"}

event:token
data:{"event":"token","node":"chat","status":"streaming","sequence":6,"done":false,"intent":"chat","content":" 很高兴"}

event:token
data:{"event":"token","node":"chat","status":"streaming","sequence":7,"done":false,"intent":"chat","content":"见到你～"}

event:token
data:{"event":"token","node":"chat","status":"streaming","sequence":8,"done":false,"intent":"chat","content":"  \n今天有想开始的"}

event:token
data:{"event":"token","node":"chat","status":"streaming","sequence":9,"done":false,"intent":"chat","content":"健身小目标吗？\uD83D\uDCAA"}

event:done
data:{"event":"done","node":"chat","status":"completed","sequence":10,"done":true,"message":"流式输出完成","intent":"chat","content":"你好呀！\uD83D\uDC4B 很高兴见到你～  \n今天有想开始的健身小目标吗？\uD83D\uDCAA"}
```
- 计划生成链路复测请求：
```bash
curl -X POST "http://localhost:8123/api/api/fitness/workflow/execute" \
  --data-urlencode "userInput=我想减脂并提升心肺，给我一个一周计划" \
  --data-urlencode "userId=1"
```
- 计划生成链路复测结果：
  - `status=completed`
  - `intent=plan_generation`
  - `node=plan_generation`
  - `profileStatus=GROUNDED`
  - 返回内容为基于 `userId=1` 真实画像生成的个性化周计划
- 计划生成链路原始返回摘录：
```json
{
  "status": "completed",
  "userInput": "我想减脂并提升心肺，给我一个一周计划",
  "userId": 1,
  "intent": "plan_generation",
  "node": "plan_generation",
  "profileStatus": "GROUNDED",
  "result": "根据用户画像报告，DATA_STATUS: GROUNDED，且用户ID=1的画像完整可靠（含年龄、性别、身高、体重、BMI、健身目标、健身水平、近期训练记录等），可生成个性化一周健身计划。... 该计划严格基于用户真实数据（GROUNDED状态），未编造任何画像字段，所有训练负荷、营养参数、周期设计均与其中级水平、当前力量表现、BMI及目标转换逻辑一致。",
  "message": "工作流执行完成"
}
```
- 动作指导链路复测请求：
```bash
curl -N -G "http://localhost:8123/api/api/fitness/workflow/execute/stream" \
  --data-urlencode "userInput=深蹲动作怎么做才标准" \
  --data-urlencode "userId=1" \
  -H "Accept: text/event-stream"
```
- 动作指导链路复测结果：
  - 先返回 `metadata(workflow started)`
  - 再返回 `metadata(action_guidance routed)`
  - 再返回 `metadata(action_guidance streaming)`
  - 随后持续返回多条 `token`
  - 最后返回 `done`
- 动作指导链路原始返回摘录：
```text
event:metadata
data:{"event":"metadata","node":"workflow","status":"started","sequence":1,"done":false,"message":"工作流开始执行"}

event:metadata
data:{"event":"metadata","node":"action_guidance","status":"routed","sequence":2,"done":false,"message":"识别意图：action_guidance","intent":"action_guidance"}

event:metadata
data:{"event":"metadata","node":"action_guidance","status":"streaming","sequence":3,"done":false,"message":"开始流式生成动作指导","intent":"action_guidance"}

event:token
data:{"event":"token","node":"action_guidance","status":"streaming","sequence":4,"done":false,"intent":"action_guidance","content":"#"}

event:token
data:{"event":"token","node":"action_guidance","status":"streaming","sequence":5,"done":false,"intent":"action_guidance","content":" 深"}

event:token
data:{"event":"token","node":"action_guidance","status":"streaming","sequence":6,"done":false,"intent":"action_guidance","content":"蹲动作"}

...

event:done
data:{"event":"done","node":"action_guidance","status":"completed","sequence":124,"done":true,"message":"流式输出完成","intent":"action_guidance","content":"# 深蹲动作标准指导\n\n## 动作名称\n深蹲（Squat）\n\n## 动作描述\n..."}
```
- 所有测试进程和 Docker 依赖在验证后已停止；`8123` 和 `8127` 均不再监听。

## 失败与修复
- 在收尾阶段，直接通过沙箱执行 `docker compose stop`、`kill` 会因为权限限制失败。
- 处理方式：
  - 对停止 Java 测试进程和 Docker 服务的命令申请提权执行。
  - 停止后再次确认接口端口已释放。
- 一轮较早的计划生成接口测试曾返回 `profileStatus=DEGRADED`，正文中声称数据库连接失败。
- 后续复测中：
  - 直接数据库查询确认 `userId=1` 数据存在
  - 主应用日志确认 DB MCP 工具调用成功
  - 同一请求再次执行后返回 `profileStatus=GROUNDED`
- 当前判断该异常结果未稳定复现，暂未定位为固定代码路径问题。

## 剩余风险
- 本次已补充覆盖 `plan_generation` 与 `action_guidance`，但 `plan_generation` 曾出现一次未稳定复现的 `DEGRADED` 返回，仍建议后续继续观察稳定性。
- `done` 事件当前会附带完整聚合文本，前端如完全依赖 token 拼接，可忽略该字段。
- 最终流式答案是“工具准备完成后再次生成”的文本，因此与同步接口在措辞上可能存在轻微差异，但事件语义和输出模式已符合本次目标。
- DB MCP 工具结果在主应用日志中存在中文乱码现象（如 `增肌` 显示异常），本次未阻塞 `GROUNDED` 计划生成，但仍是后续应处理的编码风险。
