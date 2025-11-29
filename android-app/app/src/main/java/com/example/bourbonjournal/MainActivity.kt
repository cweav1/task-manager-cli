package com.example.bourbonjournal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Delete
import androidx.compose.material3.icons.filled.LocalBar
import androidx.compose.material3.icons.filled.NoteAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

val Context.bourbonDataStore by preferencesDataStore(name = "bourbon_journal")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = bourbonColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BourbonJournalScreen()
                }
            }
        }
    }
}

data class BourbonTasting(
    val name: String,
    val distillery: String,
    val proof: String,
    val price: String,
    val scoreOverall: String,
    val notes: String,
    val flavors: List<String>,
    val timestamp: Long
)

private enum class SortOption(val label: String) {
    DATE("Date added"),
    SCORE("Score"),
    NAME("Name"),
    PRICE("Price")
}

private val flavors = listOf(
    "Vanilla", "Caramel", "Brown sugar", "Oak", "Baking spice", "Cinnamon",
    "Pepper", "Rye spice", "Fruit", "Cherry", "Apple / Pear", "Citrus", "Nutty",
    "Chocolate", "Coffee", "Floral", "Herbal", "Mint", "Smoke", "Leather", "Tobacco"
)

@Composable
fun BourbonJournalScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val tastingKey = stringPreferencesKey("tastings_json")
    val tastings = remember { mutableStateListOf<BourbonTasting>() }

    LaunchedEffect(Unit) {
        val prefs = context.bourbonDataStore.data.first()
        prefs[tastingKey]?.let { json ->
            tastings.clear()
            tastings.addAll(parseJson(json))
        }
    }

    fun persist() {
        scope.launch {
            context.bourbonDataStore.edit { prefs ->
                prefs[tastingKey] = toJson(tastings)
            }
        }
    }

    var name by remember { mutableStateOf("") }
    var distillery by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var scoreOverall by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val selectedFlavors = remember { mutableStateListOf<String>() }

    var search by remember { mutableStateOf("") }
    var minScore by remember { mutableStateOf("") }
    var flavorFilter by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.DATE) }
    var sortAscending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(text = "Bourbon Tasting Journal", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Android-ready tasting log",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            actions = {
                Icon(imageVector = Icons.Default.LocalBar, contentDescription = null)
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.NoteAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "New Tasting",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Bourbon name*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = distillery,
                            onValueChange = { distillery = it },
                            label = { Text("Distillery / Brand") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = proof,
                                onValueChange = { proof = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Proof") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Price ($)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = scoreOverall,
                            onValueChange = { scoreOverall = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Overall score (0-10)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Tasting notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlavorPicker(selected = selectedFlavors)
                        Button(
                            enabled = name.isNotBlank() && scoreOverall.isNotBlank(),
                            onClick = {
                                val entry = BourbonTasting(
                                    name = name.trim(),
                                    distillery = distillery.trim(),
                                    proof = proof.trim(),
                                    price = price.trim(),
                                    scoreOverall = scoreOverall.trim(),
                                    notes = notes.trim(),
                                    flavors = selectedFlavors.toList(),
                                    timestamp = System.currentTimeMillis()
                                )
                                tastings.add(0, entry)
                                persist()
                                name = ""
                                distillery = ""
                                proof = ""
                                price = ""
                                scoreOverall = ""
                                notes = ""
                                selectedFlavors.clear()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Tasting")
                        }
                    }
                }
            }

            if (tastings.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                item {
                    StatsHeader(
                        tastings = tastings,
                        onClearAll = {
                            tastings.clear()
                            persist()
                        }
                    )
                }
                item {
                    FilterPanel(
                        search = search,
                        onSearchChange = { search = it },
                        minScore = minScore,
                        onMinScoreChange = { minScore = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        flavorFilter = flavorFilter,
                        onFlavorFilterChange = { flavorFilter = it },
                        sortOption = sortOption,
                        onSortChange = { sortOption = it },
                        sortAscending = sortAscending,
                        onSortAscendingChange = { sortAscending = it },
                        onReset = {
                            search = ""
                            minScore = ""
                            flavorFilter = ""
                            sortOption = SortOption.DATE
                            sortAscending = false
                        }
                    )
                }
                val filtered = filteredAndSorted(
                    tastings = tastings,
                    search = search,
                    minScore = minScore,
                    flavorFilter = flavorFilter,
                    sortOption = sortOption,
                    ascending = sortAscending
                )

                if (filtered.isEmpty()) {
                    item {
                        EmptyFilteredState()
                    }
                }

                items(filtered, key = { it.timestamp }) { tasting ->
                    TastingCard(
                        tasting = tasting,
                        onDelete = {
                            tastings.remove(tasting)
                            persist()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FlavorPicker(selected: MutableList<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Flavor notes",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = { selected.clear() }) { Text("Clear") }
            TextButton(onClick = {
                selected.apply {
                    clear()
                    addAll(listOf("Vanilla", "Caramel", "Brown sugar", "Oak"))
                }
            }) {
                Text("Fill demo")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            flavors.chunked(3).forEach { rowFlavors ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowFlavors.forEach { label ->
                        val isSelected = selected.contains(label)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selected.remove(label) else selected.add(label)
                            },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsHeader(tastings: List<BourbonTasting>, onClearAll: () -> Unit) {
    val average = tastings.mapNotNull { it.scoreOverall.toDoubleOrNull() }.average().takeIf { !it.isNaN() }
    val favorite = tastings.maxByOrNull { it.scoreOverall.toDoubleOrNull() ?: 0.0 }
    val topFlavor = tastings
        .flatMap { it.flavors }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }?.key
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Quick stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClearAll) {
                    Text("Clear all")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatChip(label = "Entries", value = tastings.size.toString())
                StatChip(label = "Average", value = if (average != null) String.format("%.2f", average) else "–")
                StatChip(label = "Favorite", value = favorite?.name ?: "–")
                StatChip(label = "Top flavor", value = topFlavor ?: "–")
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TastingCard(tasting: BourbonTasting, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = tasting.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = tasting.distillery.ifBlank { "Distillery TBD" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge(text = tasting.scoreOverall.ifBlank { "–" })
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            if (tasting.notes.isNotBlank()) {
                Text(text = tasting.notes, style = MaterialTheme.typography.bodyMedium)
            }
            if (tasting.flavors.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    tasting.flavors.take(4).forEach { flavor ->
                        Badge(text = flavor, tint = MaterialTheme.colorScheme.primary)
                    }
                    if (tasting.flavors.size > 4) {
                        Text(
                            text = "+${tasting.flavors.size - 4} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, tint: Color = MaterialTheme.colorScheme.secondary) {
    Box(
        modifier = Modifier
            .background(color = tint.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocalBar,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No tastings yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Add a pour to start your Android-ready tasting journal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EmptyFilteredState() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No tastings match your filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Try clearing the search, flavor, or minimum score filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    search: String,
    onSearchChange: (String) -> Unit,
    minScore: String,
    onMinScoreChange: (String) -> Unit,
    flavorFilter: String,
    onFlavorFilterChange: (String) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Filter & sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                label = { Text("Search (name or distillery)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = minScore,
                    onValueChange = onMinScoreChange,
                    label = { Text("Min score") },
                    modifier = Modifier.weight(1f)
                )
                FlavorDropdown(
                    value = flavorFilter,
                    onValueChange = onFlavorFilterChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Sort by", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOption.values().forEach { option ->
                        val selected = sortOption == option
                        FilterChip(
                            selected = selected,
                            onClick = { onSortChange(option) },
                            label = { Text(option.label) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Order:", style = MaterialTheme.typography.labelLarge)
                    FilterChip(
                        selected = sortAscending,
                        onClick = { onSortAscendingChange(!sortAscending) },
                        label = { Text(if (sortAscending) "Low → High" else "High → Low") }
                    )
                }
            }
            TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
                Text("Reset filters")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlavorDropdown(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Flavor filter") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Any") }, onClick = {
                onValueChange("")
                expanded = false
            })
            flavors.forEach { flavor ->
                DropdownMenuItem(text = { Text(flavor) }, onClick = {
                    onValueChange(flavor)
                    expanded = false
                })
            }
        }
    }
}

private fun filteredAndSorted(
    tastings: SnapshotStateList<BourbonTasting>,
    search: String,
    minScore: String,
    flavorFilter: String,
    sortOption: SortOption,
    ascending: Boolean
): List<BourbonTasting> {
    val lowerSearch = search.trim().lowercase()
    val minScoreValue = minScore.toDoubleOrNull()
    val filtered = tastings.filter { tasting ->
        val matchesSearch = lowerSearch.isBlank() ||
            tasting.name.lowercase().contains(lowerSearch) ||
            tasting.distillery.lowercase().contains(lowerSearch)
        val matchesFlavor = flavorFilter.isBlank() || tasting.flavors.any { it.equals(flavorFilter, ignoreCase = true) }
        val matchesScore = minScoreValue == null || (tasting.scoreOverall.toDoubleOrNull() ?: 0.0) >= minScoreValue
        matchesSearch && matchesFlavor && matchesScore
    }

    val comparator = when (sortOption) {
        SortOption.DATE -> compareBy<BourbonTasting> { it.timestamp }
        SortOption.SCORE -> compareBy { it.scoreOverall.toDoubleOrNull() ?: 0.0 }
        SortOption.NAME -> compareBy { it.name.lowercase() }
        SortOption.PRICE -> compareBy { it.price.toDoubleOrNull() ?: 0.0 }
    }

    return if (ascending) {
        filtered.sortedWith(comparator)
    } else {
        filtered.sortedWith(comparator.reversed())
    }
}

private fun parseJson(json: String): List<BourbonTasting> {
    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val flavorsArray = obj.optJSONArray("flavors") ?: JSONArray()
                val parsedFlavors = mutableListOf<String>()
                for (j in 0 until flavorsArray.length()) {
                    parsedFlavors.add(flavorsArray.optString(j))
                }
                add(
                    BourbonTasting(
                        name = obj.optString("name"),
                        distillery = obj.optString("distillery"),
                        proof = obj.optString("proof"),
                        price = obj.optString("price"),
                        scoreOverall = obj.optString("scoreOverall"),
                        notes = obj.optString("notes"),
                        flavors = parsedFlavors,
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun toJson(list: List<BourbonTasting>): String {
    val array = JSONArray()
    list.forEach { tasting ->
        val obj = JSONObject()
        obj.put("name", tasting.name)
        obj.put("distillery", tasting.distillery)
        obj.put("proof", tasting.proof)
        obj.put("price", tasting.price)
        obj.put("scoreOverall", tasting.scoreOverall)
        obj.put("notes", tasting.notes)
        obj.put("timestamp", tasting.timestamp)
        obj.put("flavors", JSONArray(tasting.flavors))
        array.put(obj)
    }
    return array.toString()
}

@Composable
fun bourbonColorScheme() = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFF59E0B),
    secondary = Color(0xFFFBBF24),
    background = Color(0xFF0F172A),
    surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF111827),
    outline = Color(0xFF94A3B8)
)
