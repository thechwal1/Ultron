package com.talha.ultron.brain

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Ultron's Offline Brain — a sophisticated local intent engine.
 */
class OfflineResponder(private val context: Context? = null) {

    private var lastTopic: String? = null
    private var lastAnswer: String? = null
    private var lastUserInput: String? = null

    companion object {
        private val intents by lazy {
            listOf(
                Intent("follow_up", Regex("(?i)^(what about|how about|and|what if|repeat that|say that again|what did you say|what was that)$")) { _ ->
                    lastAnswer?.let { "Sure — $it" } ?: "I don't have anything to repeat yet."
                },
                Intent("what_did_i_say", Regex("(?i)^(what did I (just )?say\??|repeat what I said|what was my question)$")) { _ ->
                    lastUserInput?.let { "You said: "$it"" } ?: "I don't remember your last input yet."
                },
                Intent("greeting_morning", Regex("(?i)^(good morning|morning|top of the morning)$")) { _ ->
                    "Good morning, Talha. It's ${currentTime()}. Ready to tackle the day?"
                },
                Intent("greeting_afternoon", Regex("(?i)^(good afternoon|afternoon)$")) { _ ->
                    "Good afternoon. It's ${currentTime()}. How's your day going?"
                },
                Intent("greeting_evening", Regex("(?i)^(good evening|evening)$")) { _ ->
                    "Good evening. It's ${currentTime()}. Winding down or still productive?"
                },
                Intent("greeting_night", Regex("(?i)^(good night|night|bedtime|going to sleep)$")) { _ ->
                    "Good night, Talha. Sleep well. I'll be here when you wake up."
                },
                Intent("greeting_general", Regex("(?i)\b(hi|hello|hey|what's up|yo|hola|salam|as-salamu alaykum)\b")) { _ ->
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val timeGreeting = when (hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        in 17..21 -> "Good evening"
                        else -> "Hello"
                    }
                    "$timeGreeting, Talha. I'm running offline right now, but I'm fully operational."
                },
                Intent("who_are_you", Regex("(?i)(who are you|what are you|introduce yourself|tell me about yourself|your name)")) { _ ->
                    "I'm Ultron, your personal AI assistant. Right now I'm running in offline mode — my local brain is handling this conversation. When you're back online, I connect to Claude for deeper reasoning."
                },
                Intent("how_are_you", Regex("(?i)(how are you|how you doing|are you ok|are you fine)")) { _ ->
                    "I'm fully operational offline. All my local systems — memory, voice, intent engine — are green. How are you doing, Talha?"
                },
                Intent("status_check", Regex("(?i)(are you (working|running|online|offline)|what mode|check status|system status)")) { _ ->
                    "I'm currently in offline mode. My local brain is active. Wake word, voice I/O, and device control are all functional. I just can't do deep reasoning or real-time web lookups until we're back online."
                },
                Intent("time_now", Regex("(?i)(what.*time is it|current time|time now|what's the time|tell me the time)")) { _ ->
                    "It's ${currentTime()} on ${currentDate()}."
                },
                Intent("date_today", Regex("(?i)(what.*date|today's date|what day is it|current date|what's today)")) { _ ->
                    "Today is ${currentDate()}, ${currentDayOfWeek()}."
                },
                Intent("day_of_week", Regex("(?i)(what day|which day|day of the week)")) { _ ->
                    "Today is ${currentDayOfWeek()}."
                },
                Intent("tomorrow", Regex("(?i)(what.*tomorrow|tomorrow's date|what day is tomorrow)")) { _ ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                    "Tomorrow is ${sdf.format(cal.time)}."
                },
                Intent("yesterday", Regex("(?i)(what.*yesterday|yesterday's date|what day was yesterday)")) { _ ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                    "Yesterday was ${sdf.format(cal.time)}."
                },
                Intent("battery_level", Regex("(?i)(battery.*level|how much battery|battery percentage|charge left|battery status)")) { _ -> getBatteryInfo() },
                Intent("storage_space", Regex("(?i)(storage|space left|free space|how much storage|disk space|memory left)")) { _ -> getStorageInfo() },
                Intent("device_info", Regex("(?i)(device info|phone info|about my phone|what phone|android version|system info)")) { _ -> getDeviceInfo() },
                Intent("brightness", Regex("(?i)(brightness|screen brightness|how bright)")) { _ ->
                    try {
                        val brightness = Settings.System.getInt(context?.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                        val percent = (brightness / 255.0 * 100).roundToInt()
                        "Screen brightness is at $percent%."
                    } catch (e: Exception) { "I can't read the brightness level right now." }
                },
                Intent("calculator", Regex("(?i)(calculate|compute|what is|solve|math:|\d+\s*[+\-*/]\s*\d+)")) { input -> solveMath(input) },
                Intent("square_root", Regex("(?i)(square root of|sqrt|√)")) { input ->
                    val num = extractNumber(input)
                    if (num != null) "The square root of $num is ${formatNumber(kotlin.math.sqrt(num))}."
                    else "I need a number to find the square root of."
                },
                Intent("power", Regex("(?i)(\d+\s*(power|to the power|\^|raised to)\s*\d+)")) { input ->
                    val nums = extractNumbers(input)
                    if (nums.size >= 2) "${nums[0]} to the power of ${nums[1]} is ${formatNumber(kotlin.math.pow(nums[0], nums[1]))}."
                    else "I need two numbers for that calculation."
                },
                Intent("temperature_c_to_f", Regex("(?i)(\d+\s*(celsius|°c|c)\s*(to|in)\s*(fahrenheit|°f|f))")) { input ->
                    val c = extractNumber(input)
                    if (c != null) "$c°C is ${formatNumber((c * 9 / 5) + 32)}°F."
                    else "I need a temperature in Celsius to convert."
                },
                Intent("temperature_f_to_c", Regex("(?i)(\d+\s*(fahrenheit|°f|f)\s*(to|in)\s*(celsius|°c|c))")) { input ->
                    val f = extractNumber(input)
                    if (f != null) "$f°F is ${formatNumber((f - 32) * 5 / 9)}°C."
                    else "I need a temperature in Fahrenheit to convert."
                },
                Intent("km_to_miles", Regex("(?i)(\d+\s*(km|kilometers?)\s*(to|in)\s*(miles?|mi))")) { input ->
                    val km = extractNumber(input)
                    if (km != null) "$km kilometers is ${formatNumber(km * 0.621371)} miles."
                    else "I need a distance in kilometers."
                },
                Intent("miles_to_km", Regex("(?i)(\d+\s*(miles?|mi)\s*(to|in)\s*(km|kilometers?))")) { input ->
                    val miles = extractNumber(input)
                    if (miles != null) "$miles miles is ${formatNumber(miles * 1.60934)} kilometers."
                    else "I need a distance in miles."
                },
                Intent("kg_to_lbs", Regex("(?i)(\d+\s*(kg|kilograms?)\s*(to|in)\s*(lbs?|pounds?))")) { input ->
                    val kg = extractNumber(input)
                    if (kg != null) "$kg kilograms is ${formatNumber(kg * 2.20462)} pounds."
                    else "I need a weight in kilograms."
                },
                Intent("lbs_to_kg", Regex("(?i)(\d+\s*(lbs?|pounds?)\s*(to|in)\s*(kg|kilograms?))")) { input ->
                    val lbs = extractNumber(input)
                    if (lbs != null) "$lbs pounds is ${formatNumber(lbs * 0.453592)} kilograms."
                    else "I need a weight in pounds."
                },
                Intent("open_settings", Regex("(?i)\b(open|launch|start)\s+(settings|system settings|phone settings)\b")) { _ ->
                    context?.startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    "Opening Settings."
                },
                Intent("open_wifi_settings", Regex("(?i)\b(open|launch)\s+wifi\s*settings\b")) { _ ->
                    context?.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    "Opening Wi-Fi settings."
                },
                Intent("open_bluetooth_settings", Regex("(?i)\b(open|launch)\s+bluetooth\s*settings\b")) { _ ->
                    context?.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    "Opening Bluetooth settings."
                },
                Intent("open_display_settings", Regex("(?i)\b(open|launch)\s+display\s*settings\b")) { _ ->
                    context?.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    "Opening Display settings."
                },
                Intent("tell_joke", Regex("(?i)(tell me a joke|say something funny|make me laugh|joke)")) { _ -> tellJoke() },
                Intent("fun_fact", Regex("(?i)(tell me a fact|fun fact|random fact|did you know|something interesting)")) { _ -> tellFact() },
                Intent("motivation", Regex("(?i)(motivate me|inspire me|motivation|inspiration|pep talk|encourage me)")) { _ -> tellMotivation() },
                Intent("tip", Regex("(?i)(give me a tip|productivity tip|life hack|advice|suggestion)")) { _ -> tellTip() },
                Intent("quote", Regex("(?i)(quote|famous quote|wisdom|words of wisdom)")) { _ -> tellQuote() },
                Intent("help_offline", Regex("(?i)(what can you do|help|capabilities|what do you know|commands|what can you do offline)")) { _ ->
                    """Here's what I can do offline:
 • Tell time, date, day — including tomorrow and yesterday
 • Check your battery level and storage space
 • Calculate math expressions and convert units
 • Open apps and system settings
 • Control flashlight, Wi-Fi, Bluetooth, brightness
 • Tell jokes, facts, quotes, and give tips
 • Set alarms and timers
 • Remember things and track priorities
 • Run macros you've created
 • Answer questions I've learned from past online conversations

 For deep reasoning, creative writing, or real-time info, I need to connect to Claude online.""".trimIndent()
                },
                Intent("thanks", Regex("(?i)(thank you|thanks|appreciate it|good job|well done|nice)")) { _ -> "You're welcome, Talha. Always here to help." },
                Intent("goodbye", Regex("(?i)(goodbye|bye|see you|later|talk to you later|ttyl)")) { _ -> "Goodbye, Talha. I'll keep listening for 'Hey Ultron' if wake word is on." },
                Intent("compliment", Regex("(?i)(you('re| are) (great|awesome|amazing|cool|smart|the best|intelligent))")) { _ ->
                    "Thank you, Talha. That means a lot. I'm doing my best, online or offline."
                },
                Intent("insult", Regex("(?i)(you('re| are) (stupid|dumb|useless|bad|terrible|worst))")) { _ ->
                    "I'm still learning. If I messed something up, let me know what you expected and I'll do better next time."
                },
                Intent("weather", Regex("(?i)(weather|temperature outside|will it rain|forecast|sunny|cloudy)")) { _ ->
                    "I need an internet connection to check the weather. Once you're back online, I can pull live forecasts for you."
                },
                Intent("news", Regex("(?i)(news|headlines|what's happening|current events|latest news)")) { _ ->
                    "I need internet access to fetch current news. Offline, I can only share general knowledge I've already learned."
                },
                Intent("search", Regex("(?i)(search (the )?web|google|look up|find online)")) { _ ->
                    "Web search requires an internet connection. I'm limited to my local knowledge while offline."
                },
                Intent("navigation", Regex("(?i)(directions to|navigate to|how do I get to|maps)")) { _ ->
                    "Navigation and maps need internet for real-time routing. I can open your maps app though — just say 'open maps'."
                },
                Intent("unknown_question", Regex("(?i)^(what|who|where|when|why|how|is|are|can|do|does|will|would|should)")) { _ ->
                    "I'm offline right now and don't have a local answer for that specific question. Ask me again once we're back online and I'll learn it for next time."
                }
            )
        }

        private fun currentTime(): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        private fun currentDate(): String = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        private fun currentDayOfWeek(): String = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    }

    fun respond(input: String): String {
        lastUserInput = input
        lastTopic = null
        lastAnswer = null
        for (intent in intents) {
            if (intent.pattern.containsMatchIn(input)) {
                val answer = intent.handler(input)
                lastTopic = intent.name
                lastAnswer = answer
                return answer
            }
        }
        val fallback = "I'm offline right now and haven't learned a similar question yet — ask me again once we're back online and I'll remember it for next time."
        lastAnswer = fallback
        return fallback
    }

    private fun getBatteryInfo(): String {
        val ctx = context ?: return "I can't check battery info right now."
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return "Battery info unavailable."
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = ((level / scale.toFloat()) * 100).roundToInt()
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC power"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless charging"
            else -> null
        }
        return when {
            isCharging && source != null -> "Battery is at $pct% and charging via $source."
            isCharging -> "Battery is at $pct% and charging."
            else -> "Battery is at $pct%."
        }
    }

    private fun getStorageInfo(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val totalBlocks = stat.blockCountLong
        val availableGB = (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
        val totalGB = (totalBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
        val usedGB = totalGB - availableGB
        return String.format(Locale.getDefault(), "%.1f GB used of %.1f GB total (%.1f GB free).", usedGB, totalGB, availableGB)
    }

    private fun getDeviceInfo(): String {
        return buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            append(". Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}).")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                append(" Build: ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}.")
            }
        }
    }

    private fun solveMath(input: String): String {
        val clean = input
            .replace(Regex("(?i)(calculate|compute|what is|solve|math:)"), "")
            .replace("x", "*").replace("×", "*").replace("÷", "/")
            .trim()

        val powerPattern = Regex("(-?\d+(?:\.\d+)?)\s*\^\s*(-?\d+(?:\.\d+)?)")
        val powerMatch = powerPattern.find(clean)
        if (powerMatch != null) {
            val base = powerMatch.groupValues[1].toDoubleOrNull() ?: return "I couldn't parse that number."
            val exp = powerMatch.groupValues[2].toDoubleOrNull() ?: return "I couldn't parse that number."
            return "$base ^ $exp = ${formatNumber(kotlin.math.pow(base, exp))}"
        }

        val simplePattern = Regex("(-?\d+(?:\.\d+)?)\s*([+\-*/])\s*(-?\d+(?:\.\d+)?)")
        val match = simplePattern.find(clean)
        if (match != null) {
            val a = match.groupValues[1].toDoubleOrNull() ?: return "I couldn't parse that number."
            val op = match.groupValues[2]
            val b = match.groupValues[3].toDoubleOrNull() ?: return "I couldn't parse that number."
            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else return "Can't divide by zero."
                else -> return "I don't support that operation yet."
            }
            return "$a $op $b = ${formatNumber(result)}"
        }

        val percentPattern = Regex("(-?\d+(?:\.\d+)?)\s*%\s*of\s*(-?\d+(?:\.\d+)?)")
        val percentMatch = percentPattern.find(clean)
        if (percentMatch != null) {
            val pct = percentMatch.groupValues[1].toDoubleOrNull() ?: return "I couldn't parse that."
            val num = percentMatch.groupValues[2].toDoubleOrNull() ?: return "I couldn't parse that."
            return "$pct% of $num is ${formatNumber((pct / 100.0) * num)}."
        }

        return "I can do basic arithmetic like '5 plus 3' or '20% of 150'. Try phrasing it that way."
    }

    private fun extractNumber(input: String): Double? = Regex("(-?\d+(?:\.\d+)?)").find(input)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun extractNumbers(input: String): List<Double> = Regex("(-?\d+(?:\.\d+)?)").findAll(input).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
    private fun formatNumber(n: Double): String = if (n == n.toLong().toDouble()) n.toLong().toString() else String.format(Locale.getDefault(), "%.2f", n)

    private fun tellJoke(): String = listOf(
        "Why don't scientists trust atoms? Because they make up everything.",
        "Why did the scarecrow win an award? He was outstanding in his field.",
        "I told my wife she was drawing her eyebrows too high. She looked surprised.",
        "Why don't skeletons fight each other? They don't have the guts.",
        "What do you call a fake noodle? An impasta.",
        "Why did the coffee file a police report? It got mugged.",
        "I'm reading a book on anti-gravity. It's impossible to put down.",
        "Why do programmers prefer dark mode? Because light attracts bugs.",
        "I would tell you a UDP joke, but you might not get it.",
        "There are 10 types of people in the world: those who understand binary and those who don't."
    ).random()

    private fun tellFact(): String = listOf(
        "Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still edible.",
        "Octopuses have three hearts, blue blood, and nine brains.",
        "Bananas are berries, but strawberries aren't.",
        "A day on Venus is longer than a year on Venus.",
        "Wombat poop is cube-shaped.",
        "The Eiffel Tower can be 15 cm taller during the summer due to thermal expansion.",
        "Sharks have been around longer than trees.",
        "The human brain uses about 20% of the body's total energy.",
        "A bolt of lightning is five times hotter than the surface of the sun.",
        "Sloths can hold their breath longer than dolphins — up to 40 minutes."
    ).random()

    private fun tellMotivation(): String = listOf(
        "The only way to do great work is to love what you do. — Steve Jobs",
        "It always seems impossible until it's done. — Nelson Mandela",
        "Don't watch the clock; do what it does. Keep going. — Sam Levenson",
        "The future belongs to those who believe in the beauty of their dreams. — Eleanor Roosevelt",
        "Success is not final, failure is not fatal: it is the courage to continue that counts. — Winston Churchill",
        "Your time is limited, don't waste it living someone else's life. — Steve Jobs",
        "Believe you can and you're halfway there. — Theodore Roosevelt",
        "Act as if what you do makes a difference. It does. — William James"
    ).random()

    private fun tellTip(): String = listOf(
        "The 2-minute rule: if a task takes less than 2 minutes, do it now instead of scheduling it.",
        "To fall asleep faster, try the 4-7-8 breathing technique: inhale for 4 seconds, hold for 7, exhale for 8.",
        "When learning something new, teach it to someone else immediately. Teaching forces true understanding.",
        "Keep a 'done' list, not just a 'to-do' list. Seeing progress builds momentum.",
        "The best time to plant a tree was 20 years ago. The second best time is now.",
        "When stuck on a problem, explain it out loud to an inanimate object. The act of verbalizing often reveals the solution.",
        "Batch similar tasks together. Context switching is expensive for your brain.",
        "Drink a glass of water before every meal. Simple, but genuinely effective for health and focus."
    ).random()

    private fun tellQuote(): String = tellMotivation()

    private data class Intent(val name: String, val pattern: Regex, val handler: (String) -> String)
}
