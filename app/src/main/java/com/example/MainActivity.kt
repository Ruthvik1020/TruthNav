package com.example
 
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.NavViewModel
import com.example.ui.screens.BenchmarkScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.EdgeEngineScreen
import com.example.ui.screens.NavigationScreen
import com.example.ui.theme.*
 
enum class AppNavTab(val title: String, val icon: ImageVector, val tag: String) {
    NAVIGATION("Navigation", Icons.Default.Navigation, "tab_cockpit"),
    REPLAY("Replay", Icons.Default.PlayCircleOutline, "tab_benchmark"),
    CALIBRATE("Calibrate", Icons.Default.Tune, "tab_calibration"),
    SYSTEM("System", Icons.Default.Memory, "tab_edge_engine")
}
 
class MainActivity : ComponentActivity() {
 
     private val viewModel: NavViewModel by viewModels()
 
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         enableEdgeToEdge()
         setContent {
             MyApplicationTheme {
                 MainAppScreen(viewModel = viewModel)
             }
         }
     }
 }
 
 @Composable
 fun MainAppScreen(viewModel: NavViewModel) {
     var selectedTab by remember { mutableStateOf(AppNavTab.NAVIGATION) }
 
     // Request Location permission launcher
     val permissionLauncher = rememberLauncherForActivityResult(
         contract = ActivityResultContracts.RequestMultiplePermissions()
     ) { permissions ->
         val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
         if (fineGranted) {
             viewModel.liveSensorManager.startListening()
         }
     }
 
     LaunchedEffect(Unit) {
         permissionLauncher.launch(
             arrayOf(
                 Manifest.permission.ACCESS_FINE_LOCATION,
                 Manifest.permission.ACCESS_COARSE_LOCATION
             )
         )
     }
 
     Scaffold(
         modifier = Modifier.fillMaxSize(),
         bottomBar = {
             Surface(
                 color = SpaceDarkSurface,
                 tonalElevation = 8.dp,
                 border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder.copy(alpha = 0.5f)),
                 modifier = Modifier.fillMaxWidth()
             ) {
                 NavigationBar(
                     containerColor = SpaceDarkSurface,
                     contentColor = TextPrimary,
                     tonalElevation = 0.dp,
                     windowInsets = WindowInsets.navigationBars,
                     modifier = Modifier.height(68.dp)
                 ) {
                     AppNavTab.entries.forEach { tab ->
                         val isSelected = selectedTab == tab
                         NavigationBarItem(
                             selected = isSelected,
                             onClick = { selectedTab = tab },
                             icon = {
                                 Icon(
                                     imageVector = tab.icon,
                                     contentDescription = tab.title,
                                     tint = if (isSelected) AmberAccent else TextSecondary,
                                     modifier = Modifier.size(22.dp)
                                 )
                             },
                             label = {
                                 Text(
                                     text = tab.title,
                                     fontSize = 11.sp,
                                     fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                     color = if (isSelected) AmberAccent else TextSecondary
                                 )
                             },
                             colors = NavigationBarItemDefaults.colors(
                                 indicatorColor = AmberAccent.copy(alpha = 0.15f),
                                 selectedIconColor = AmberAccent,
                                 unselectedIconColor = TextSecondary,
                                 selectedTextColor = AmberAccent,
                                 unselectedTextColor = TextSecondary
                             ),
                             modifier = Modifier.testTag(tab.tag)
                         )
                     }
                 }
             }
         }
     ) { innerPadding ->
         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(SpaceDark)
                 .padding(innerPadding)
         ) {
             when (selectedTab) {
                 AppNavTab.NAVIGATION -> NavigationScreen(viewModel = viewModel)
                 AppNavTab.CALIBRATE -> CalibrationScreen(viewModel = viewModel)
                 AppNavTab.REPLAY -> BenchmarkScreen(viewModel = viewModel)
                 AppNavTab.SYSTEM -> EdgeEngineScreen(viewModel = viewModel)
             }
         }
     }
 }

