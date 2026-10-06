package com.scl.mgr

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.scl.mgr.data.GoogleDriveSyncManager
import com.scl.mgr.data.AutoSyncWorker
import com.scl.mgr.data.SchoolRepository
import com.scl.mgr.data.Student
import com.scl.mgr.data.MonthlyExam
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class DrawerItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val demo: Boolean = false
)

private const val PREFS = "student_form_defaults"

@Composable
fun SchoolManagerApp(repository: SchoolRepository, syncManager: GoogleDriveSyncManager) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val items = remember {
        listOf(
            DrawerItem("dashboard", "Dashboard", Icons.Default.Home),
            DrawerItem("students", "Students", Icons.Default.School),
            DrawerItem("teachers", "Teachers", Icons.Default.Person, true),
            DrawerItem("classes", "Classes", Icons.Default.Class, true),
            DrawerItem("subjects", "Subjects", Icons.Default.MenuBook, true),
            DrawerItem("attendance", "Attendance", Icons.Default.CheckCircle),
            DrawerItem("monthly_exam", "Monthly Exam", Icons.Default.Assignment),
            DrawerItem("exams", "Exams & Results", Icons.Default.Assignment, true),
            DrawerItem("fees", "Fees & Payments", Icons.Default.AccountBalanceWallet, true),
            DrawerItem("notices", "Notices", Icons.Default.Notifications, true),
            DrawerItem("events", "Events", Icons.Default.Event, true),
            DrawerItem("reports", "Reports", Icons.Default.BarChart, true),
            DrawerItem("settings", "Google Drive", Icons.Default.Cloud, false)
        )
    }

    fun navigateFromDrawer(route: String, demo: Boolean, label: String) {
        scope.launch { drawerState.close() }
        val target = if (demo) "demo/$label" else route
        navController.navigate(target) {
            // Drawer destinations are root-level pages. Replace the current root page
            // instead of stacking every drawer click in the back stack.
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(310.dp)) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "School Manager",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("School Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("School administration", style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                Column(Modifier.fillMaxHeight().padding(bottom = 16.dp)) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(items, key = { it.route }) { item ->
                            NavigationDrawerItem(
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.label)
                                        if (item.demo) {
                                            Spacer(Modifier.width(8.dp))
                                            AssistChip(onClick = {}, label = { Text("Demo") }, enabled = false)
                                        }
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = null) },
                                selected = false,
                                onClick = { navigateFromDrawer(item.route, item.demo, item.label) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    NavigationDrawerItem(
                        label = { Text("About") },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("about") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("dashboard") {
                AppScaffold("Dashboard", drawerState, scope) {
                    DashboardScreen(repository, onStudents = { navController.navigate("students") })
                }
            }
            composable("students") {
                AppScaffold("Students", drawerState, scope) {
                    StudentsScreen(
                        repository = repository,
                        onAdd = { navController.navigate("student/add") },
                        onOpen = { id -> navController.navigate("student/$id") }
                    )
                }
            }
            composable("attendance") {
                AppScaffold("Attendance", drawerState, scope) {
                    AttendanceScreen(repository)
                }
            }
            composable("monthly_exam") {
                AppScaffold("Monthly Exam", drawerState, scope) {
                    MonthlyExamScreen(repository)
                }
            }
            composable("student/add") {
                AppScaffold("Add Student", drawerState, scope) {
                    StudentFormScreen(repository, null) { navController.popBackStack() }
                }
            }
            composable(
                "student/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: return@composable
                AppScaffold("Student Details", drawerState, scope) {
                    StudentDetailsScreen(
                        repository = repository,
                        studentId = id,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("student/$id/edit") },
                        onDeleted = { navController.popBackStack() }
                    )
                }
            }
            composable(
                "student/{id}/edit",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: return@composable
                AppScaffold("Edit Student", drawerState, scope) {
                    StudentFormScreen(repository, id) { navController.popBackStack() }
                }
            }
            composable("demo/{name}") { backStack ->
                val name = backStack.arguments?.getString("name") ?: "Feature"
                AppScaffold(name, drawerState, scope) { DemoScreen(name) }
            }
            composable("settings") {
                AppScaffold("Google Drive", drawerState, scope) {
                    GoogleDriveScreen(syncManager)
                }
            }
            composable("about") {
                AppScaffold("About", drawerState, scope) { AboutScreen() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    title: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

private data class AttendanceGroupSummary(
    val className: String,
    val section: String,
    val total: Int,
    val present: Int,
    val absent: Int
)

@Composable
private fun DashboardScreen(repository: SchoolRepository, onStudents: () -> Unit) {
    val students by repository.students("").collectAsState(initial = emptyList())
    val totalStudents = students.size

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Welcome to School Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(onClick = onStudents, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Total Students", style = MaterialTheme.typography.titleMedium)
                Text("$totalStudents", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Manage students and attendance")
            }
        }
    }
}

@Composable
private fun AttendanceCountCard(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("$count", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudentsScreen(repository: SchoolRepository, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    val students by repository.students(query).collectAsState(initial = emptyList())

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                label = { Text("Search students") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            if (students.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (query.isBlank()) "No students yet." else "No students found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students, key = { it.id }) { student -> StudentCard(student) { onOpen(student.id) } }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Add student") }
    }
}

@Composable
private fun StudentCard(student: Student, onClick: () -> Unit) {
    val context = LocalContext.current
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && student.mobileNumber.isNotBlank()) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${student.mobileNumber}"))
            runCatching { context.startActivity(intent) }
        }
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(student.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("ID/Roll: ${student.studentId}")
                Text("Class: ${student.className} • Section: ${student.section}")
                if (student.gender.isNotBlank()) Text("Gender: ${student.gender}")
                if (student.religion.isNotBlank()) Text("Religion: ${student.religion}")
                if (student.fatherName.isNotBlank()) Text("Father: ${student.fatherName}")
                if (student.mobileNumber.isNotBlank()) Text("Mobile: ${student.mobileNumber}")
            }
            if (student.mobileNumber.isNotBlank()) {
                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${student.mobileNumber}"))
                        runCatching { context.startActivity(intent) }
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }) {
                    Icon(Icons.Default.Call, contentDescription = "Call ${student.studentName}", modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentFormScreen(repository: SchoolRepository, studentId: Long?, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, 0) }
    var existing by remember { mutableStateOf<Student?>(null) }

    var name by remember { mutableStateOf("") }
    var idText by remember { mutableStateOf("") }
    var className by remember { mutableStateOf(prefs.getString("class", "") ?: "") }
    var section by remember { mutableStateOf(prefs.getString("section", "") ?: "") }
    var gender by remember { mutableStateOf(prefs.getString("gender", "") ?: "") }
    var religion by remember { mutableStateOf(prefs.getString("religion", "") ?: "") }
    var father by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(studentId) {
        if (studentId != null) {
            existing = repository.getStudent(studentId)
            existing?.let {
                name = it.studentName
                idText = it.studentId
                className = it.className
                section = it.section
                gender = it.gender
                religion = it.religion
                father = it.fatherName
                address = it.address
                mobile = it.mobileNumber
            }
        }
    }

    fun save() {
        if (name.isBlank() || idText.isBlank() || className.isBlank() || section.isBlank() || gender.isBlank() || religion.isBlank()) {
            error = "Name, ID/Roll, class, section, gender and religion are required."
            return
        }
        val student = Student(
            id = existing?.id ?: 0,
            studentName = name.trim(),
            studentId = idText.trim(),
            className = className.trim(),
            section = section.trim(),
            gender = gender,
            religion = religion,
            fatherName = father.trim(),
            address = address.trim(),
            mobileNumber = mobile.trim()
        )
        scope.launch {
            try {
                if (repository.duplicateExists(student)) {
                    error = "This class, section and roll already exists. Student was not added."
                    return@launch
                }
                if (existing == null) repository.addStudent(student) else repository.updateStudent(student)
                prefs.edit()
                    .putString("class", className)
                    .putString("section", section)
                    .putString("gender", gender)
                    .putString("religion", religion)
                    .apply()
                onSaved()
            } catch (_: IllegalArgumentException) {
                error = "This class, section and roll already exists. Student was not added."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormField("Student Name *", name) { name = it }
            FormField("Student ID / Roll *", idText) { idText = it }
            DropdownField("Class *", className, (6..10).map(Int::toString)) { className = it }
            DropdownField("Section *", section, listOf("A", "B")) { section = it }
            DropdownField("Gender *", gender, listOf("Boy", "Girl")) { gender = it }
            DropdownField("Religion *", religion, listOf("Muslim", "Hindu")) { religion = it }
            FormField("Father's Name", father) { father = it }
            FormField("Address", address, minLines = 3) { address = it }
            FormField("Mobile Number", mobile) { mobile = it }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (existing == null) "Add Student" else "Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FormField(label: String, value: String, minLines: Int = 1, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceScreen(repository: SchoolRepository) {
    val allStudents by repository.students("").collectAsState(initial = emptyList())
    var selectedClass by remember { mutableStateOf("All") }
    var selectedSection by remember { mutableStateOf("All") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf("attendance") }
    var customRange by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var toDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var tab by remember { mutableIntStateOf(0) }
    var statuses by remember { mutableStateOf<Map<Long, Boolean?>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    val classOptions = remember(allStudents) { listOf("All") + allStudents.map { it.className }.distinct().sortedWith(compareBy { it.toIntOrNull() ?: 999 }) }
    val sectionOptions = remember(allStudents, selectedClass) {
        listOf("All") + allStudents.filter { selectedClass == "All" || it.className == selectedClass }.map { it.section }.distinct().sorted()
    }
    val filteredStudents = remember(allStudents, selectedClass, selectedSection) {
        allStudents.filter {
            (selectedClass == "All" || it.className == selectedClass) &&
            (selectedSection == "All" || it.section == selectedSection)
        }
    }

    LaunchedEffect(date, filteredStudents) {
        val loaded = mutableMapOf<Long, Boolean?>()
        filteredStudents.forEach { student -> loaded[student.id] = repository.attendanceForDate(student.id, date)?.present }
        statuses = loaded
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Mark Attendance") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Monthly View") })
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField("Class", selectedClass, classOptions, Modifier.weight(1f)) {
                selectedClass = it
                selectedSection = "All"
            }
            DropdownField("Section", selectedSection, sectionOptions, Modifier.weight(1f)) { selectedSection = it }
        }

        if (tab == 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.weight(1f), singleLine = true)
                IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, contentDescription = "Choose date") }
            }
            if (filteredStudents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No students in this class/section.") }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val status = statuses[student.id]
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(student.studentName, fontWeight = FontWeight.Bold)
                                Text("Roll ${student.studentId} • Class ${student.className}-${student.section}")
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { scope.launch { repository.markAttendance(student.id, date, true); statuses = statuses + (student.id to true) } }, modifier = Modifier.weight(1f), colors = if (status == true) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Present") }
                                    OutlinedButton(onClick = { scope.launch { repository.markAttendance(student.id, date, false); statuses = statuses + (student.id to false) } }, modifier = Modifier.weight(1f)) { Text("Absent") }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val start = if (customRange) fromDate else month.atDay(1).toString()
            val end = if (customRange) toDate else month.atEndOfMonth().toString()
            val validRange = runCatching { LocalDate.parse(start) <= LocalDate.parse(end) }.getOrDefault(false)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !customRange, onClick = { customRange = false }, label = { Text("Monthly") })
                        FilterChip(selected = customRange, onClick = { customRange = true }, label = { Text("Custom Range") })
                    }
                    Spacer(Modifier.height(8.dp))
                    if (customRange) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = fromDate, onValueChange = { fromDate = it }, label = { Text("From date") }, modifier = Modifier.weight(1f), singleLine = true,
                                trailingIcon = { IconButton(onClick = { datePickerTarget = "from"; showDatePicker = true }) { Icon(Icons.Default.DateRange, contentDescription = "Choose from date") } })
                            OutlinedTextField(value = toDate, onValueChange = { toDate = it }, label = { Text("To date") }, modifier = Modifier.weight(1f), singleLine = true,
                                trailingIcon = { IconButton(onClick = { datePickerTarget = "to"; showDatePicker = true }) { Icon(Icons.Default.DateRange, contentDescription = "Choose to date") } })
                        }
                        if (!validRange) Text("From date must be on or before the To date.", color = MaterialTheme.colorScheme.error)
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
                            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
                        }
                    }
                }
                if (validRange) {
                    items(filteredStudents, key = { "month-${it.id}" }) { student ->
                        var p by remember(student.id, start, end) { mutableIntStateOf(0) }
                        var a by remember(student.id, start, end) { mutableIntStateOf(0) }
                        LaunchedEffect(student.id, start, end) {
                            p = repository.studentPresentCountBetween(student.id, start, end)
                            a = repository.studentAbsentCountBetween(student.id, start, end)
                        }
                        Card(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(student.studentName, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("Roll ${student.studentId} • Class ${student.className}-${student.section}") },
                                trailingContent = { Text("P $p  •  A $a", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialDate = when (datePickerTarget) {
            "from" -> fromDate
            "to" -> toDate
            else -> date
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = runCatching {
            LocalDate.parse(initialDate).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selected = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                        when (datePickerTarget) {
                            "from" -> fromDate = selected
                            "to" -> toDate = selected
                            else -> date = selected
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun StudentDetailsScreen(
    repository: SchoolRepository,
    studentId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var student by remember { mutableStateOf<Student?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    val attendance by repository.attendance(studentId).collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }

    LaunchedEffect(studentId) { student = repository.getStudent(studentId) }

    student?.let { s ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(s.studentName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("ID/Roll: ${s.studentId}")
                Text("Class: ${s.className} • Section: ${s.section}")
                Text("Gender: ${s.gender} • Religion: ${s.religion}")
                if (s.fatherName.isNotBlank()) Text("Father: ${s.fatherName}")
                if (s.mobileNumber.isNotBlank()) {
                    val context = LocalContext.current
                    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        if (granted) runCatching { context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${s.mobileNumber}"))) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mobile: ${s.mobileNumber}", modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${s.mobileNumber}"))) }
                            } else {
                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            }
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call ${s.studentName}")
                        }
                    }
                }
                if (s.address.isNotBlank()) Text("Address: ${s.address}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Edit")
                    }
                    OutlinedButton(
                        onClick = { showDelete = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Delete")
                    }
                }
            }
            item {
                HorizontalDivider()
                Text("Attendance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = selectedDate, onValueChange = { selectedDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { repository.markAttendance(studentId, selectedDate, true) } }, modifier = Modifier.weight(1f)) { Text("Present") }
                    OutlinedButton(onClick = { scope.launch { repository.markAttendance(studentId, selectedDate, false) } }, modifier = Modifier.weight(1f)) { Text("Absent") }
                }
            }
            item { Text("Attendance History", style = MaterialTheme.typography.titleMedium) }
            items(attendance, key = { it.id }) { record ->
                ListItem(
                    headlineContent = { Text(record.date) },
                    supportingContent = { Text(if (record.present) "Present" else "Absent") },
                    leadingContent = {
                        Icon(
                            if (record.present) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (record.present) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Student not found.") }

    if (showDelete && student != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete student?") },
            text = { Text("This will permanently remove the student and their attendance records from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    val s = student ?: return@TextButton
                    scope.launch { repository.deleteStudent(s); showDelete = false; onDeleted() }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleDriveScreen(syncManager: GoogleDriveSyncManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(AutoSyncWorker.PREFS, android.content.Context.MODE_PRIVATE) }
    var account by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var autoSync by remember { mutableStateOf(prefs.getBoolean(AutoSyncWorker.KEY_AUTO_SYNC, false)) }
    var launchRecovery: ((android.content.Intent) -> Unit)? = null

    fun performSync(selected: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        busy = true
        status = "Syncing Google Drive…"
        scope.launch {
            try {
                val result = syncManager.sync(selected)
                prefs.edit().putLong(AutoSyncWorker.KEY_LAST_SYNC, System.currentTimeMillis()).apply()
                status = if (result.restored) {
                    "Drive database merged. Added ${result.studentsAdded} student(s) and ${result.attendanceAdded} attendance record(s)."
                } else {
                    "No database found. The current database was uploaded to the shared folder."
                }
            } catch (e: GoogleDriveSyncManager.DriveAuthorizationRequiredException) {
                status = "Google Drive permission is required. Opening Google authorization…"
                launchRecovery?.invoke(e.recoveryIntent)
            } catch (e: Exception) {
                status = "Sync failed: ${e.message ?: "Unknown error"}"
            } finally {
                busy = false
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val selected = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            account = selected
            performSync(selected)
        } catch (e: ApiException) {
            status = "Google sign-in failed: statusCode=${e.statusCode}, message=${e.message ?: "none"}"
        } catch (e: Exception) {
            status = "Google sign-in failed: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    launchRecovery = { intent -> signInLauncher.launch(intent) }

    val signInClient = remember(context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(GoogleDriveSyncManager.DRIVE_FILE_SCOPE))
                .build()
        )
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Google Drive Sync", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Uses the shared School Manager folder and one database file.")
                Text("Folder ID: ${GoogleDriveSyncManager.SHARED_FOLDER_ID}", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (account == null) {
            Button(
                onClick = { signInLauncher.launch(signInClient.signInIntent) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Connect Google account")
            }
        } else {
            ListItem(
                leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                headlineContent = { Text(account?.email ?: account?.account?.name.orEmpty()) },
                supportingContent = { Text("Connected to shared School Manager folder") }
            )
            Button(
                onClick = { account?.let(::performSync) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Syncing…" else "Sync now")
            }
            ListItem(
                headlineContent = { Text("Auto Sync") },
                supportingContent = { Text("Sync automatically when internet is available (about every 15 minutes).") },
                trailingContent = {
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { enabled ->
                            autoSync = enabled
                            prefs.edit().putBoolean(AutoSyncWorker.KEY_AUTO_SYNC, enabled).apply()
                            if (enabled) {
                                status = "Auto Sync enabled."
                            } else {
                                status = "Auto Sync disabled."
                            }
                        }
                    )
                }
            )
            OutlinedButton(
                onClick = {
                    signInClient.signOut()
                    account = null
                    status = "Google account disconnected. The Drive database was not deleted."
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Disconnect") }
        }

        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        HorizontalDivider()
        Text("Shared database", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("• One plain school_manager.db is stored in the configured shared Google Drive folder.\n• Authorized Google accounts can access the same database.\n• The app merges remote students and attendance into the local database before uploading.\n• No backup password or app-level encryption is used.")
    }
}

@Composable
private fun DemoScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("$name is Demo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("This module will be added in a future version.")
        }
    }
}

@Composable
private fun AboutScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.School, contentDescription = "School Manager", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(56.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("School Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Version 1.2")
        Spacer(Modifier.height(8.dp))
        Text("Offline student and attendance manager.")
        Spacer(Modifier.height(20.dp))
        Text("Package: com.scl.mgr", style = MaterialTheme.typography.bodySmall)
    }
}


private val LOWER_EXAM_SUBJECTS = listOf(
    "Bangla" to "bangla",
    "English" to "english",
    "Math" to "math",
    "S.Science" to "socialScience",
    "Science" to "science",
    "Religion" to "religion"
)

private val UPPER_EXAM_SUBJECTS = listOf(
    "Bangla" to "bangla",
    "English" to "english",
    "Math" to "math",
    "S.Science" to "socialScience",
    "Religion" to "religion",
    "Physics" to "physics",
    "Chemistry" to "chemistry",
    "Biology" to "biology",
    "H.M./Agri." to "hMOrAgri"
)

private fun examSubjects(className: String): List<Pair<String, String>> =
    if (className == "9" || className == "10") UPPER_EXAM_SUBJECTS else LOWER_EXAM_SUBJECTS

private fun markFor(exam: MonthlyExam?, key: String): Int? = when (key) {
    "bangla" -> exam?.bangla
    "english" -> exam?.english
    "math" -> exam?.math
    "socialScience" -> exam?.socialScience
    "science" -> exam?.science
    "religion" -> exam?.religion
    "physics" -> exam?.physics
    "chemistry" -> exam?.chemistry
    "biology" -> exam?.biology
    "hMOrAgri" -> exam?.hMOrAgri
    else -> null
}

private fun withMark(exam: MonthlyExam, key: String, value: Int?): MonthlyExam = when (key) {
    "bangla" -> exam.copy(bangla = value)
    "english" -> exam.copy(english = value)
    "math" -> exam.copy(math = value)
    "socialScience" -> exam.copy(socialScience = value)
    "science" -> exam.copy(science = value)
    "religion" -> exam.copy(religion = value)
    "physics" -> exam.copy(physics = value)
    "chemistry" -> exam.copy(chemistry = value)
    "biology" -> exam.copy(biology = value)
    "hMOrAgri" -> exam.copy(hMOrAgri = value)
    else -> exam
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyExamScreen(repository: SchoolRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val students by repository.students("").collectAsState(initial = emptyList())
    var className by rememberSaveable { mutableStateOf("6") }
    var section by rememberSaveable { mutableStateOf("A") }
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var subjectIndex by rememberSaveable { mutableIntStateOf(0) }
    var marks by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var exams by remember { mutableStateOf<Map<Long, MonthlyExam>>(emptyMap()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showClass by remember { mutableStateOf(false) }
    var showSection by remember { mutableStateOf(false) }
    var showMonth by remember { mutableStateOf(false) }
    var showSubject by remember { mutableStateOf(false) }
    var activeStudentId by remember { mutableStateOf<Long?>(null) }
    var pendingPdfAfterPermission by rememberSaveable { mutableStateOf(false) }

    val subjects = examSubjects(className)
    val selectedSubject = subjects.getOrNull(subjectIndex) ?: subjects.first()
    val classStudents = remember(students, className, section) {
        students.filter { it.className == className && it.section == section }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(month) {
        exams = repository.monthlyExamsOnce(month).associateBy { it.studentId }
    }
    LaunchedEffect(className, section, month, subjectIndex, classStudents, exams) {
        marks = classStudents.associate { student ->
            student.id to (markFor(exams[student.id], selectedSubject.second)?.toString() ?: "")
        }
        activeStudentId = null
    }

    fun setMark(studentId: Long, value: String) {
        val clean = value.filter(Char::isDigit).take(2)
        if (clean.isEmpty() || (clean.toIntOrNull() ?: 0) <= 20) {
            marks = marks + (studentId to clean)
        }
    }

    fun moveToNextStudent() {
        val current = activeStudentId ?: return
        val index = classStudents.indexOfFirst { it.id == current }
        if (index >= 0 && index < classStudents.lastIndex) {
            val next = classStudents[index + 1]
            activeStudentId = next.id
            scope.launch {
                listState.animateScrollToItem(index + 1)
            }
        } else {
            message = "Last student reached. Press Save & Next to continue."
        }
    }

    fun saveCurrentSubject(next: Boolean) {
        scope.launch {
            val saved = classStudents.map { student ->
                val existing = exams[student.id] ?: MonthlyExam(student.id, month)
                val raw = marks[student.id]?.trim().orEmpty()
                val value = raw.toIntOrNull()?.coerceIn(0, 20)
                withMark(existing, selectedSubject.second, if (raw.isBlank()) null else value)
            }
            repository.saveMonthlyExams(saved)
            exams = (exams + saved.associateBy { it.studentId })
            message = "${selectedSubject.first} saved"
            if (next && subjectIndex < subjects.lastIndex) {
                subjectIndex++
                activeStudentId = null
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingPdfAfterPermission = true
        } else {
            message = "Storage permission is required to save the PDF in Downloads/School Manager."
        }
    }

    fun generatePdf() {
        scope.launch {
            try {
                message = "Creating PDF…"
                val all = repository.monthlyExamsOnce(month).associateBy { it.studentId }
                val ordered = classStudents.sortedWith(compareBy({ it.studentId.toIntOrNull() ?: Int.MAX_VALUE }, { it.studentName.lowercase() }))
                val result = generateMonthlyExamPdf(context, className, section, month, subjects, ordered, all)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, result.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Monthly Exam PDF").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {
                    context.startActivity(chooser)
                    message = "PDF saved to Downloads/School Manager"
                } catch (_: android.content.ActivityNotFoundException) {
                    message = "PDF saved to Downloads/School Manager/${result.fileName}"
                }
            } catch (e: Exception) {
                message = "PDF failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }
    }

    LaunchedEffect(pendingPdfAfterPermission) {
        if (pendingPdfAfterPermission) {
            pendingPdfAfterPermission = false
            generatePdf()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Monthly Exam", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Classes 6–8: 6 subjects / 120 marks • Classes 9–10: 9 subjects / 180 marks", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { showClass = true }, modifier = Modifier.fillMaxWidth()) { Text("Class $className") }
                DropdownMenu(showClass, { showClass = false }) {
                    listOf("6", "7", "8", "9", "10").forEach { value ->
                        DropdownMenuItem(text = { Text("Class $value") }, onClick = { className = value; subjectIndex = 0; showClass = false })
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { showSection = true }, modifier = Modifier.fillMaxWidth()) { Text("Section $section") }
                DropdownMenu(showSection, { showSection = false }) {
                    listOf("A", "B").forEach { value ->
                        DropdownMenuItem(text = { Text("Section $value") }, onClick = { section = value; showSection = false })
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { showMonth = true }, modifier = Modifier.fillMaxWidth()) { Text(month) }
                DropdownMenu(showMonth, { showMonth = false }) {
                    val months = (0..11).map { YearMonth.of(YearMonth.now().year, it + 1) }
                    months.forEach { ym ->
                        DropdownMenuItem(text = { Text(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"))) }, onClick = { month = ym.toString(); showMonth = false })
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Subject selector: teacher can jump directly to any subject.
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showSubject = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Subject: ${selectedSubject.first}")
            }
            DropdownMenu(showSubject, { showSubject = false }) {
                subjects.forEachIndexed { index, subject ->
                    DropdownMenuItem(
                        text = { Text("${index + 1}. ${subject.first}") },
                        onClick = {
                            subjectIndex = index
                            showSubject = false
                            activeStudentId = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Enter marks 0–20 • Tap a box to open the numeric pad", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        if (classStudents.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text("No students found for Class $className, Section $section. Add students first.", Modifier.padding(16.dp)) }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Roll", Modifier.width(58.dp), fontWeight = FontWeight.Bold)
                        Text("Student Name", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("Mark / 20", fontWeight = FontWeight.Bold)
                    }
                }
                items(classStudents, key = { it.id }) { student ->
                    val isActive = activeStudentId == student.id
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(student.studentId, Modifier.width(58.dp))
                        Text(student.studentName, Modifier.weight(1f), maxLines = 1)
                        // This is intentionally NOT a TextField. A read-only TextField can
                        // consume the tap/focus event before the app keypad is opened.
                        // A clickable surface makes every tap reliably open our built-in keypad
                        // and never invokes the Android/system keyboard.
                        Surface(
                            modifier = Modifier
                                .width(100.dp)
                                .height(56.dp)
                                .clickable { activeStudentId = student.id },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            ),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(
                                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = marks[student.id].takeUnless { it.isNullOrBlank() } ?: "0–20",
                                    color = if (marks[student.id].isNullOrBlank())
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            // The built-in keypad is rendered below the student list rather than as an
            // overlay. This keeps the active marks box visible and prevents the keypad
            // from covering the input field.
        }

        if (activeStudentId != null) {
            val activeStudent = classStudents.firstOrNull { it.id == activeStudentId }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Marks • ${activeStudent?.studentName ?: "Student"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                "${selectedSubject.first} • 0–20",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            marks[activeStudentId] ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(5.dp))

                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
                    keys.chunked(3).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            row.forEach { key ->
                                Button(
                                    onClick = {
                                        val id = activeStudentId ?: return@Button
                                        when (key) {
                                            "C" -> setMark(id, "")
                                            "⌫" -> setMark(id, (marks[id] ?: "").dropLast(1))
                                            else -> setMark(id, (marks[id] ?: "") + key)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(key, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeStudentId = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Close")
                        }
                        Button(
                            onClick = { moveToNextStudent() },
                            modifier = Modifier
                                .weight(2f)
                                .height(42.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Enter → Next", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Keep the main action row out of the way while the built-in keypad is open.
        // This gives the keypad and the student list their own layout space instead of
        // allowing anything to be covered by an overlay.
        if (activeStudentId == null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { if (subjectIndex > 0) subjectIndex-- }, enabled = subjectIndex > 0, modifier = Modifier.weight(1f)) { Text("Previous") }
                Button(onClick = { saveCurrentSubject(subjectIndex < subjects.lastIndex) }, enabled = classStudents.isNotEmpty(), modifier = Modifier.weight(1.3f)) {
                    Text(if (subjectIndex < subjects.lastIndex) "Save & Next" else "Save All")
                }
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        ) {
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            generatePdf()
                        }
                    },
                    enabled = classStudents.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Generate PDF") }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) }
    }
}

private data class PdfOutput(val uri: Uri, val fileName: String)

private fun generateMonthlyExamPdf(
    context: android.content.Context,
    className: String,
    section: String,
    yearMonth: String,
    subjects: List<Pair<String, String>>,
    students: List<Student>,
    exams: Map<Long, MonthlyExam>
): PdfOutput {
    val doc = PdfDocument()
    val pageWidth = 842
    val pageHeight = 595
    val margin = 24f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 20f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 9f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 8f }
    val totalMax = subjects.size * 20
    val monthTitle = try { YearMonth.parse(yearMonth).format(DateTimeFormatter.ofPattern("MMMM yyyy")) } catch (_: Exception) { yearMonth }
    val columns = listOf("Roll", "Student") + subjects.map { it.first } + "Total"
    val widths = mutableListOf(42f, 145f).apply { repeat(subjects.size) { add(60f) }; add(55f) }
    var pageNumber = 0
    var index = 0
    while (index < students.size || students.isEmpty() && pageNumber == 0) {
        pageNumber++
        val page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        val c = page.canvas
        c.drawText("School Manager", margin, 30f, titlePaint)
        c.drawText("Monthly Examination — Class $className, Section $section — $monthTitle", margin, 50f, textPaint)
        c.drawText("Maximum: $totalMax", pageWidth - 105f, 50f, headerPaint)
        var x = margin
        var y = 70f
        paint.style = Paint.Style.STROKE
        c.drawRect(margin, y, pageWidth - margin, y + 24f, paint)
        columns.forEachIndexed { i, name ->
            c.drawText(name, x + 3f, y + 16f, headerPaint)
            x += widths[i]
            if (i < columns.lastIndex) c.drawLine(x, y, x, y + 24f, paint)
        }
        paint.style = Paint.Style.FILL
        y += 24f
        val rowHeight = 22f
        while (index < students.size && y + rowHeight < pageHeight - 40f) {
            val s = students[index]
            val e = exams[s.id]
            x = margin
            paint.style = Paint.Style.STROKE
            c.drawRect(margin, y, pageWidth - margin, y + rowHeight, paint)
            paint.style = Paint.Style.FILL
            val values = mutableListOf(s.studentId, s.studentName)
            val marks = subjects.map { markFor(e, it.second) }
            val total = marks.filterNotNull().sum()
            values.addAll(marks.map { it?.toString() ?: "" })
            values.add(if (marks.any { it != null }) total.toString() else "")
            values.forEachIndexed { i, value ->
                c.drawText(value.take(if (i == 1) 24 else 11), x + 3f, y + 15f, textPaint)
                x += widths[i]
                if (i < values.lastIndex) c.drawLine(x, y, x, y + rowHeight, paint)
            }
            y += rowHeight
            index++
        }
        c.drawText("Page $pageNumber", pageWidth - 70f, pageHeight - 18f, textPaint)
        doc.finishPage(page)
        if (students.isEmpty()) break
    }

    val fileName = "Monthly_Exam_Class_${className}_${section}_${yearMonth}.pdf"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: MediaStore can create a real public Downloads file without
            // broad storage permission, using scoped storage correctly.
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/School Manager")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw java.io.IOException("Unable to create Downloads/School Manager file")
            try {
                resolver.openOutputStream(uri)?.use { output -> doc.writeTo(output) }
                    ?: throw java.io.IOException("Unable to open PDF output stream")
                val done = android.content.ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
                PdfOutput(uri, fileName)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            // Android 8/9: public Downloads requires WRITE_EXTERNAL_STORAGE permission.
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads, "School Manager")
            if (!dir.exists() && !dir.mkdirs()) {
                throw java.io.IOException("Unable to create Downloads/School Manager")
            }
            val file = File(dir, fileName)
            file.outputStream().use { output -> doc.writeTo(output) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            PdfOutput(uri, fileName)
        }
    } finally {
        doc.close()
    }
}
