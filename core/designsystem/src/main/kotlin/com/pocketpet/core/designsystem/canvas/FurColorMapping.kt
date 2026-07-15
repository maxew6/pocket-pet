package com.pocketpet.core.designsystem.canvas

import androidx.compose.ui.graphics.Color
import com.pocketpet.core.designsystem.theme.FurBlush
import com.pocketpet.core.designsystem.theme.FurCaramel
import com.pocketpet.core.designsystem.theme.FurCharcoal
import com.pocketpet.core.designsystem.theme.FurHoney
import com.pocketpet.core.designsystem.theme.FurIvory
import com.pocketpet.core.designsystem.theme.FurMilk
import com.pocketpet.core.model.ColorTone

fun ColorTone.toFurColor(): Color = when (this) {
    ColorTone.Milk -> FurMilk
    ColorTone.Ivory -> FurIvory
    ColorTone.Honey -> FurHoney
    ColorTone.Caramel -> FurCaramel
    ColorTone.Blush -> FurBlush
    ColorTone.Charcoal -> FurCharcoal
}
