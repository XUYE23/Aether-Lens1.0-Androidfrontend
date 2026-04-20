package com.aether.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalSpacePortraitSummaryTest {

    @Test
    fun `portraitPreviewItems returns first cards in reading order`() {
        val groups = listOf(
            PortraitGroup(
                label = "我是谁",
                cards = listOf(
                    PortraitCard("Me", "Léa · 28 岁"),
                    PortraitCard("Me", "住在巴黎")
                )
            ),
            PortraitGroup(
                label = "我在意的",
                cards = listOf(
                    PortraitCard("Value", "把话说清楚"),
                    PortraitCard("Value", "答应的事要做到")
                )
            )
        )

        assertEquals(
            listOf("Léa · 28 岁", "住在巴黎", "把话说清楚"),
            portraitPreviewItems(groups, limit = 3)
        )
    }

    @Test
    fun `portraitPreviewItems respects limit`() {
        val groups = listOf(
            PortraitGroup(
                label = "我是谁",
                cards = listOf(
                    PortraitCard("Me", "A"),
                    PortraitCard("Me", "B"),
                    PortraitCard("Me", "C")
                )
            )
        )

        assertEquals(listOf("A", "B"), portraitPreviewItems(groups, limit = 2))
    }
}
