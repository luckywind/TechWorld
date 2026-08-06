# [用量查询](https://github.com/farion1231/cc-switch/blob/main/docs/user-manual/zh/2-providers/2.5-usage-query.md)

模板：

| 类别       | 覆盖供应商                                                | 模板类型               |
| ---------- | --------------------------------------------------------- | ---------------------- |
| Token Plan | Kimi / Zhipu GLM / MiniMax                                | 套餐配额（带使用进度） |
| 第三方余额 | DeepSeek / StepFun / SiliconFlow / OpenRouter / Novita AI | 官方余额查询           |

[CC-Switch的用量查询 小白式保姆级配置教程](https://linux.do/t/topic/1429450)



## deepseek

通用模板：
[参考](https://github.com/farion1231/cc-switch/issues/1658)

```json
({
  request: {
    url: "{{baseUrl}}/user/balance",
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: "Bearer {{apiKey}}",
      "User-Agent": "cc-switch/1.0",
    },
  },
  extractor: function (response) {
    return {
      isValid: response.is_available,
      remaining: parseFloat(response.balance_infos[0].total_balance),
      unit: response.balance_infos[0].currency,
    };
  },
});
```



通用模板：
```json
({
  request: {
    url: "{{baseUrl}}/user/balance",
    method: "GET",
    headers: {
      Authorization: "Bearer {{apiKey}}",
      "User-Agent": "cc-switch/1.0",
    },
  },
  extractor: function (response) {
    // 1. 查找USD余额（DeepSeek默认返回CNY，若你账户是USD则改此条件）
    const balanceInfo = response.balance_infos?.find(info => info.currency === "CNY");

    // 2. 提取余额数值，若找不到则返回0
    const remainingBalance = balanceInfo ? parseFloat(balanceInfo.total_balance) : 0;

    return {
      isValid: response.is_available === true, // 直接使用API返回的可用状态
      remaining: remainingBalance,
      unit: "CNY" // 单位与上述currency字段保持一致
    };
  },
});
```



## 模型映射

| 模型槽                                                       | 映射模型            | 说明                                              |
| ------------------------------------------------------------ | ------------------- | ------------------------------------------------- |
| ***\*主模型 (Default)\****                                   | deepseek-v4-pro[1m] | 1M 超大上下文，主力推理模型                       |
| ***\*Opus\****                                               | deepseek-v4-pro[1m] | 对应 Claude Code 的 Opus 模型请求默认，重量级推理 |
| ***\*Sonnet\****                                             | deepseek-v4-pro[1m] | 对应 Claude Code 的 Sonnet 模型请求，中等复杂度   |
| ***\*Haiku\****                                              | deepseek-v4-flash   | 轻量快速模型，适合作子代理，轻量级，快速响应      |
| ***\*子代理模型 (Sub-[agent](https://link.csdn.net/?target=https%3A%2F%2Fgitcode.com%2Fatomgit_atomcode%2Fatomcode%3Futm_source%3Ddevpress_gitcode_keyword%26login%3Dfrom_csdn))\**** | deepseek-v4-flash   | 子任务执行，节省 token                            |
