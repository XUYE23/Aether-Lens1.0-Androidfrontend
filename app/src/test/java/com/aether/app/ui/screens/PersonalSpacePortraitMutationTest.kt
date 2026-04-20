package com.aether.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalSpacePortraitMutationTest {

    @Test
    fun `addPortraitCard appends card to matching group`() {
        val groups = listOf(
            PortraitGroup(
                label = "我是谁",
                cards = listOf(PortraitCard("ME", "原有内容"))
            ),
            PortraitGroup(
                label = "我喜欢的",
                cards = listOf(PortraitCard("LIKE", "燕麦拿铁"))
            )
        )

        val result = addPortraitCardToGroups(
            portraitGroups = groups,
            targetLabel = "我是谁",
            content = "跑步王"
        )

        assertEquals(2, result.first { it.label == "我是谁" }.cards.size)
        assertEquals("跑步王", result.first { it.label == "我是谁" }.cards.last().text)
    }

    @Test
    fun `addPortraitCard creates matching metadata for new card`() {
        val result = addPortraitCardToGroups(
            portraitGroups = listOf(
                PortraitGroup(
                    label = "我的目标",
                    cards = emptyList()
                )
            ),
            targetLabel = "我的目标",
            content = "这周完成专访稿"
        )

        val newCard = result.first().cards.single()
        assertEquals("GOAL", newCard.category)
        assertEquals("这周完成专访稿", newCard.text)
        assertEquals("刚刚添加", newCard.tail)
    }

    @Test
    fun `resolvePreferredPortraitLabel keeps tapped group label`() {
        val result = resolvePreferredPortraitLabel(
            preferredLabel = "我的人",
            portraitGroups = listOf(
                PortraitGroup("我是谁", emptyList()),
                PortraitGroup("我的人", emptyList())
            )
        )

        assertEquals("我的人", result)
    }

    @Test
    fun `updatePortraitCard replaces matching card in target group`() {
        val original = PortraitCard("ME", "跑步王", "刚刚添加")
        val result = updatePortraitCardInGroups(
            portraitGroups = listOf(
                PortraitGroup("我是谁", listOf(original)),
                PortraitGroup("我喜欢的", listOf(PortraitCard("LIKE", "燕麦拿铁")))
            ),
            targetLabel = "我是谁",
            originalCard = original,
            originalGroupLabel = "我是谁",
            content = "夜跑的人"
        )

        val updated = result.first { it.label == "我是谁" }.cards.single()
        assertEquals("夜跑的人", updated.text)
        assertEquals("已更新", updated.tail)
    }

    @Test
    fun `deletePortraitCard removes matching card from target group`() {
        val removable = PortraitCard("ME", "跑步王")
        val result = deletePortraitCardFromGroups(
            portraitGroups = listOf(
                PortraitGroup("我是谁", listOf(removable, PortraitCard("ME", "住在巴黎"))),
                PortraitGroup("我喜欢的", listOf(PortraitCard("LIKE", "燕麦拿铁")))
            ),
            targetLabel = "我是谁",
            targetCard = removable
        )

        assertEquals(1, result.first { it.label == "我是谁" }.cards.size)
        assertEquals("住在巴黎", result.first { it.label == "我是谁" }.cards.single().text)
    }
}
