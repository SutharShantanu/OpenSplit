package com.opensplit.data.ai

import com.opensplit.domain.model.SplitType

/**
 * Instant, offline fallbacks used when AI is off, has no key, or fails. Every AI feature in the
 * app must still work through these, so they are the floor, not an afterthought.
 */
object AiHeuristics {

    /** Category names must match CategoryChipRow's list. */
    private val categoryKeywords: List<Pair<String, List<String>>> = listOf(
        "Groceries" to listOf("grocer", "supermarket", "blinkit", "zepto", "bigbasket", "walmart", "costco", "dmart"),
        "Coffee" to listOf("coffee", "cafe", "starbucks", "latte", "chai", "tea"),
        "Drinks" to listOf("beer", "wine", "bar", "pub", "drinks", "cocktail", "whisky"),
        "Food" to listOf("dinner", "lunch", "breakfast", "pizza", "burger", "restaurant", "swiggy", "zomato", "food", "biryani", "snack", "brunch"),
        "Transport" to listOf("uber", "ola", "cab", "taxi", "fuel", "petrol", "diesel", "metro", "bus", "train", "parking", "toll"),
        "Travel" to listOf("flight", "hotel", "airbnb", "trip", "ticket", "visa", "hostel", "resort"),
        "Rent" to listOf("rent", "lease", "deposit", "maintenance"),
        "Bills" to listOf("electricity", "wifi", "internet", "water bill", "gas bill", "utility", "bill", "recharge", "phone"),
        "Subscriptions" to listOf("netflix", "spotify", "prime", "subscription", "youtube", "icloud"),
        "Shopping" to listOf("amazon", "flipkart", "mall", "clothes", "shoes", "shopping", "ikea"),
        "Health" to listOf("doctor", "pharmacy", "medicine", "hospital", "clinic", "dentist"),
        "Fitness" to listOf("gym", "yoga", "fitness", "trainer", "crossfit"),
        "Entertainment" to listOf("movie", "cinema", "concert", "game", "netflix party", "show", "theatre", "theater"),
        "Education" to listOf("course", "book", "tuition", "class", "udemy", "school", "college"),
        "Gifts" to listOf("gift", "birthday", "present", "wedding"),
        "Pets" to listOf("vet", "pet", "dog", "cat", "kibble")
    )

    fun categoryFor(description: String): String {
        val lower = description.lowercase()
        if (lower.isBlank()) return "Other"
        return categoryKeywords.firstOrNull { (_, words) -> words.any { lower.contains(it) } }?.first ?: "Other"
    }

    fun splitTypeFor(category: String): SplitType =
        if (category == "Groceries" || category == "Shopping") SplitType.ITEMIZED else SplitType.EQUAL

    private val avatarKeywords = mapOf(
        "travel" to listOf("trip", "tour", "flight", "fly", "vacation", "holiday", "travel", "goa", "paris", "tokyo", "bali", "hawaii", "plane", "resort", "roadtrip", "trek", "hike"),
        "home" to listOf("home", "flat", "apartment", "house", "room", "rent", "roommate", "household", "stay", "villa", "pg", "suite"),
        "food" to listOf("food", "pizza", "lunch", "dinner", "restaurant", "eat", "burger", "cafe", "snack", "biryani", "swiggy", "zomato", "dining", "breakfast", "brunch", "sushi", "taco"),
        "party" to listOf("party", "club", "celebration", "bar", "drinks", "pub", "weekend", "fest", "nightout", "bachelor", "bachelorette", "cheers"),
        "work" to listOf("office", "work", "project", "team", "colleague", "corp", "startup", "company", "meeting", "hackathon", "sync"),
        "school" to listOf("school", "college", "university", "study", "class", "tuition", "batch", "exam", "grad", "campus", "hostel"),
        "beach" to listOf("beach", "sea", "ocean", "surf", "island", "coast", "goa", "miami", "maldives"),
        "gaming" to listOf("game", "gaming", "ps5", "xbox", "steam", "esports", "lan", "arcade", "valorant", "dota"),
        "car" to listOf("car", "cab", "uber", "ola", "drive", "road", "taxi", "ride", "fuel", "petrol", "gas", "commute"),
        "movie" to listOf("movie", "cinema", "film", "show", "netflix", "theater", "popcorn", "imax", "binge"),
        "sports" to listOf("sports", "football", "soccer", "cricket", "match", "gym", "turf", "badminton", "tennis", "nba", "ipl"),
        "pets" to listOf("pet", "dog", "cat", "puppy", "kitten", "vet", "doggo"),
        "music" to listOf("music", "concert", "gig", "band", "song", "spotify", "festival", "show"),
        "books" to listOf("book", "lib", "read", "library", "novel", "reading"),
        "coffee" to listOf("coffee", "chai", "tea", "starbucks", "boba", "espresso", "latte"),
        "shopping" to listOf("shopping", "mall", "clothes", "groceries", "market", "store", "target", "amazon", "mart"),
        "money" to listOf("money", "fund", "investment", "finance", "bank", "savings", "budget", "cash", "crypto"),
        "birthday" to listOf("birthday", "bday", "cake", "anniversary", "gift"),
        "fitness" to listOf("fitness", "workout", "gym", "crossfit", "training", "run", "marathon", "yoga"),
        "world" to listOf("world", "global", "international", "europe", "asia", "usa", "overseas")
    )

    fun avatarKeyFor(groupName: String): String? {
        val lower = groupName.lowercase().trim()
        if (lower.isBlank()) return null
        return avatarKeywords.entries.firstOrNull { (_, words) -> words.any { lower.contains(it) } }?.key
    }

    /**
     * Best-effort offline parse of "Rahul paid 1500 for dinner yesterday with Priya and me".
     * Amount = first number; description = words minus money/people/filler.
     */
    fun parseExpenseText(text: String, memberNames: List<String>): ParsedExpense? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val amount = Regex("""(?<![\w.])(\d{1,3}(?:,\d{3})+|\d+)(?:\.\d{1,2})?""").find(trimmed)
            ?.value?.replace(",", "")?.toDoubleOrNull()
        val lower = trimmed.lowercase()
        val payer = memberNames.firstOrNull { name ->
            val first = name.substringBefore(' ').lowercase()
            first.length > 1 && Regex("""\b${Regex.escape(first)}\b\s+(paid|payed|covered)""").containsMatchIn(lower)
        }
        val mentioned = memberNames.filter { name ->
            val first = name.substringBefore(' ').lowercase()
            first.length > 1 && Regex("""\b${Regex.escape(first)}\b""").containsMatchIn(lower)
        }
        val stop = setOf("paid", "payed", "for", "with", "and", "me", "i", "we", "split", "equally", "between", "among",
            "rs", "rs.", "inr", "usd", "eur", "yesterday", "today", "tomorrow", "on", "the", "a", "an", "of")
        val names = memberNames.flatMap { it.lowercase().split(' ') }.toSet()
        val words = trimmed.split(Regex("\\s+")).filter { w ->
            val l = w.lowercase().trim(',', '.')
            l.isNotBlank() && l !in stop && l !in names && !Regex("""^[₹$€£]?\d[\d,]*(\.\d+)?$""").matches(l)
        }
        val description = words.take(5).joinToString(" ").replaceFirstChar { it.uppercase() }
        return ParsedExpense(
            description = description.ifBlank { "Expense" },
            amount = amount,
            category = categoryFor(trimmed),
            paidByName = payer,
            participantNames = mentioned,
            splitType = SplitType.EQUAL,
            source = ParsedExpense.Source.LOCAL
        )
    }
}
