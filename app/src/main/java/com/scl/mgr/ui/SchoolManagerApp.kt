package com.scl.mgr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.scl.mgr.data.SchoolRepository
import com.scl.mgr.data.Student
import java.time.LocalDate
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
fun SchoolManagerApp(repository: SchoolRepository) {
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
            DrawerItem("exams", "Exams & Results", Icons.Default.Assignment, true),
            DrawerItem("fees", "Fees & Payments", Icons.Default.AccountBalanceWallet, true),
            DrawerItem("notices", "Notices", Icons.Default.Notifications, true),
            DrawerItem("events", "Events", Icons.Default.Event, true),
            DrawerItem("reports", "Reports", Icons.Default.BarChart, true),
            DrawerItem("settings", "Settings", Icons.Default.Settings, true)
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

@Composable
private fun DashboardScreen(repository: SchoolRepository, onStudents: () -> Unit) {
    val count by repository.studentCount().collectAsState(initial = 0)
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Welcome to School Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(onClick = onStudents, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Students", style = MaterialTheme.typography.titleMedium)
                Text("$count", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Manage students and attendance")
            }
        }
        Text("Offline mode", style = MaterialTheme.typography.bodyMedium)
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
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(student.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("ID/Roll: ${student.studentId}")
            Text("Class: ${student.className} • Section: ${student.section}")
            if (student.gender.isNotBlank()) Text("Gender: ${student.gender}")
            if (student.religion.isNotBlank()) Text("Religion: ${student.religion}")
            if (student.fatherName.isNotBlank()) Text("Father: ${student.fatherName}")
            if (student.mobileNumber.isNotBlank()) Text("Mobile: ${student.mobileNumber}")
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
private fun DropdownField(label: String, value: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
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
    val students by repository.students("").collectAsState(initial = emptyList())
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var statuses by remember { mutableStateOf<Map<Long, Boolean?>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(date, students) {
        val loaded = mutableMapOf<Long, Boolean?>()
        students.forEach { student -> loaded[student.id] = repository.attendanceForDate(student.id, date)?.present }
        statuses = loaded
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Choose date")
            }
        }

        if (students.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Add students first.") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students, key = { it.id }) { student ->
                    val status = statuses[student.id]
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(student.studentName, fontWeight = FontWeight.Bold)
                            Text("Roll ${student.studentId} • Class ${student.className}-${student.section}")
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.markAttendance(student.id, date, true)
                                            statuses = statuses + (student.id to true)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = if (status == true) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) { Text("Present") }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.markAttendance(student.id, date, false)
                                            statuses = statuses + (student.id to false)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Absent") }
                            }
                            if (status != null) {
                                Text(
                                    if (status) "Marked Present" else "Marked Absent",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
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
                if (s.mobileNumber.isNotBlank()) Text("Mobile: ${s.mobileNumber}")
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
        Text("Version 1.1")
        Spacer(Modifier.height(8.dp))
        Text("Offline student and attendance manager.")
        Spacer(Modifier.height(20.dp))
        Text("Package: com.scl.mgr", style = MaterialTheme.typography.bodySmall)
    }
}
