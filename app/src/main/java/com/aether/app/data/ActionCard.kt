package com.aether.app.data

data class ActionCard(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val timestamp: Long
)

object MockData {
    const val userName = "江南少"

    val sampleCard = ActionCard(
        id = "1",
        type = "message",
        title = "飞书消息草稿",
        content = "向团队发送项目进度更新：\n\n各位好，Aether 项目第一阶段开发已完成 80%，预计本周五完成核心交互功能。",
        timestamp = System.currentTimeMillis()
    )

    fun getCardList() = listOf(
        ActionCard(
            id = "1",
            type = "message",
            title = "飞书消息草稿",
            content = "向团队发送项目进度更新：\n\n各位好，Aether 项目第一阶段开发已完成 80%，预计本周五完成核心交互功能。",
            timestamp = System.currentTimeMillis()
        ),
        ActionCard(
            id = "2",
            type = "approval",
            title = "请假审批",
            content = "张三申请年假 3 天\n时间：4月5日-4月7日\n理由：家庭事务",
            timestamp = System.currentTimeMillis()
        ),
        ActionCard(
            id = "3",
            type = "notification",
            title = "会议提醒",
            content = "产品评审会议将在 30 分钟后开始\n地点：3楼会议室\n参会人：产品团队全员",
            timestamp = System.currentTimeMillis()
        ),
        ActionCard(
            id = "4",
            type = "task",
            title = "代码审查",
            content = "李四提交了 PR #234\n需要你的审查和批准\n涉及文件：5个",
            timestamp = System.currentTimeMillis()
        )
    )
}
