package com.example.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NoCell
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhonelinkOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class HabitIconEntry(
  val key: String,
  val label: String,
  val category: String,
  val vector: ImageVector? = null,
  val emoji: String? = null
)

object HabitIconsCatalog {
  val icons: List<HabitIconEntry> = listOf(
    // Featured / Defaults
    HabitIconEntry("star", "Star", "Popular", Icons.Default.Star),
    HabitIconEntry("walk", "Walk", "Health", Icons.AutoMirrored.Filled.DirectionsWalk),
    HabitIconEntry("run", "Run", "Health", Icons.AutoMirrored.Filled.DirectionsRun),
    HabitIconEntry("workout", "Workout", "Health", Icons.Default.FitnessCenter),
    HabitIconEntry("water", "Drink Water", "Health", Icons.Default.WaterDrop),
    HabitIconEntry("meditate", "Meditate", "Mind", Icons.Default.SelfImprovement),
    HabitIconEntry("book", "Read Book", "Study", Icons.AutoMirrored.Filled.MenuBook),
    HabitIconEntry("moon", "Lights Out", "Sleep", Icons.Default.Bedtime),
    HabitIconEntry("savings", "Money / Spend", "Money", Icons.Default.Savings),
    HabitIconEntry("sunrise", "Morning", "Faith & Reflection", Icons.Default.WbTwilight),
    HabitIconEntry("sun", "Daylight", "Faith & Reflection", Icons.Default.WbSunny),
    HabitIconEntry("night", "Evening", "Faith & Reflection", Icons.Default.NightlightRound),
    HabitIconEntry("mosque", "Prayer / Faith", "Faith & Reflection", Icons.Default.Mosque),
    HabitIconEntry("charity", "Charity", "Faith & Reflection", Icons.Default.VolunteerActivism),
    HabitIconEntry("vitamins", "Vitamins", "Health", Icons.Default.Medication),
    HabitIconEntry("journal", "Journal", "Mind", Icons.Default.EditNote),
    HabitIconEntry("mind", "Psychology", "Mind", Icons.Default.Psychology),
    HabitIconEntry("noscreen", "No Screens", "Sleep", Icons.Default.PhonelinkOff),
    HabitIconEntry("alarm", "Wake Up", "Sleep", Icons.Default.AccessAlarm),
    HabitIconEntry("study", "Study", "Study", Icons.Default.School),
    HabitIconEntry("code", "Code", "Study", Icons.Default.Code),
    HabitIconEntry("language", "Language", "Study", Icons.Default.Translate),
    HabitIconEntry("clean", "Tidy Up", "Home", Icons.Default.CleaningServices),
    HabitIconEntry("home", "Home", "Home", Icons.Default.Home),
    HabitIconEntry("plan", "Plan Day", "Home", Icons.Default.CalendarMonth),
    HabitIconEntry("flag", "Milestone Goal", "Popular", Icons.Default.Flag),
    HabitIconEntry("trophy", "Trophy", "Popular", Icons.Default.EmojiEvents),
    HabitIconEntry("laptop", "Laptop", "Money", Icons.Default.Computer),
    HabitIconEntry("wallet", "Wallet", "Money", Icons.Default.AccountBalanceWallet),
    HabitIconEntry("bike", "Cycling", "Health", Icons.AutoMirrored.Filled.DirectionsBike),
    HabitIconEntry("food", "Healthy Meal", "Health", Icons.Default.Restaurant),
    HabitIconEntry("coffee", "Coffee Limit", "Health", Icons.Default.Coffee),
    HabitIconEntry("spa", "Stretch / Spa", "Health", Icons.Default.Spa),
    HabitIconEntry("heart", "Heart", "Health", Icons.Default.Favorite),
    HabitIconEntry("bolt", "Energy", "Popular", Icons.Default.Bolt),
    HabitIconEntry("sparkles", "Sparkles", "Popular", Icons.Default.AutoAwesome),
    HabitIconEntry("idea", "New Idea", "Mind", Icons.Default.Lightbulb),
    HabitIconEntry("brush", "Creative Art", "Mind", Icons.Default.Brush),
    HabitIconEntry("music", "Music Practice", "Mind", Icons.Default.MusicNote),
    HabitIconEntry("headphones", "Podcast", "Study", Icons.Default.Headphones),
    HabitIconEntry("timer", "Focus Timer", "Study", Icons.Default.Timer),
    HabitIconEntry("work", "Deep Work", "Study", Icons.Default.Work),
    HabitIconEntry("pets", "Pet Care", "Home", Icons.Default.Pets),
    HabitIconEntry("cart", "Groceries", "Home", Icons.Default.ShoppingCart),
    HabitIconEntry("flight", "Travel", "Popular", Icons.Default.FlightTakeoff),
    // Emoji collection
    HabitIconEntry("emoji:🙏", "Prayer Hands", "Emoji", emoji = "🙏"),
    HabitIconEntry("emoji:📖", "Open Book", "Emoji", emoji = "📖"),
    HabitIconEntry("emoji:🌙", "Crescent Moon", "Emoji", emoji = "🌙"),
    HabitIconEntry("emoji:☀️", "Morning Sun", "Emoji", emoji = "☀️"),
    HabitIconEntry("emoji:💧", "Water Droplet", "Emoji", emoji = "💧"),
    HabitIconEntry("emoji:🏃", "Runner", "Emoji", emoji = "🏃"),
    HabitIconEntry("emoji:🧘", "Lotus Pose", "Emoji", emoji = "🧘"),
    HabitIconEntry("emoji:💪", "Strength", "Emoji", emoji = "💪"),
    HabitIconEntry("emoji:🥗", "Green Salad", "Emoji", emoji = "🥗"),
    HabitIconEntry("emoji:💊", "Supplement", "Emoji", emoji = "💊"),
    HabitIconEntry("emoji:😴", "Sleep", "Emoji", emoji = "😴"),
    HabitIconEntry("emoji:📵", "No Phone", "Emoji", emoji = "📵"),
    HabitIconEntry("emoji:💰", "Savings Bag", "Emoji", emoji = "💰"),
    HabitIconEntry("emoji:✍️", "Writing", "Emoji", emoji = "✍️"),
    HabitIconEntry("emoji:🧠", "Brain", "Emoji", emoji = "🧠"),
    HabitIconEntry("emoji:🎯", "Bullseye", "Emoji", emoji = "🎯"),
    HabitIconEntry("emoji:🧹", "Broom", "Emoji", emoji = "🧹"),
    HabitIconEntry("emoji:🌿", "Plant", "Emoji", emoji = "🌿"),
    HabitIconEntry("emoji:🎸", "Guitar", "Emoji", emoji = "🎸"),
    HabitIconEntry("emoji:💻", "Laptop", "Emoji", emoji = "💻")
  )

  val categories = listOf("All", "Popular", "Health", "Mind", "Sleep", "Study", "Faith & Reflection", "Money", "Home", "Emoji")

  fun findByKey(key: String): HabitIconEntry {
    if (key.startsWith("emoji:")) {
      val emojiChar = key.removePrefix("emoji:")
      return HabitIconEntry(key, emojiChar, "Emoji", emoji = emojiChar)
    }
    return icons.firstOrNull { it.key == key } ?: icons.first()
  }
}

@Composable
fun HabitIconView(
  iconKey: String,
  tint: Color,
  modifier: Modifier = Modifier,
  emojiSize: TextUnit = 24.sp
) {
  val entry = HabitIconsCatalog.findByKey(iconKey)
  if (entry.emoji != null) {
    Text(
      text = entry.emoji,
      fontSize = emojiSize,
      modifier = modifier
    )
  } else {
    Icon(
      imageVector = entry.vector ?: Icons.Default.Star,
      contentDescription = entry.label,
      tint = tint,
      modifier = modifier
    )
  }
}
