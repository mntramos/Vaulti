package com.vaulti.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : BottomNavItem("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Transactions : BottomNavItem("transactions", "Transactions", Icons.Filled.Receipt, Icons.Outlined.Receipt)
    data object Accounts : BottomNavItem("accounts", "Accounts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    data object Budgets : BottomNavItem("budgets", "Budgets", Icons.Filled.TrackChanges, Icons.Outlined.TrackChanges)
    data object Goals : BottomNavItem("goals", "Goals", Icons.Filled.Savings, Icons.Outlined.Savings)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Transactions,
    BottomNavItem.Accounts,
    BottomNavItem.Budgets,
    BottomNavItem.Goals
)

@Composable
fun VaultiBottomNavBar(
    modifier: Modifier = Modifier,
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit
) {
    NavigationBar(modifier = modifier) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(text = item.title, style = MaterialTheme.typography.labelSmall)
                }
            )
        }
    }
}
