import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safeguardme.app.data.models.ContactType
import com.safeguardme.app.data.models.EmergencyContact
import com.safeguardme.app.data.models.NotificationMethod
import com.safeguardme.app.ui.components.AddEditContactDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // Import launch

// =============================================
// RelationshipSuggestions.kt - Relationship Suggestions
// =============================================

@Composable
fun RelationshipSuggestions(
    onSuggestionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Relationship Suggestions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                // Suggestions grid
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val suggestions = getRelationshipSuggestions()

                    items(suggestions.chunked(2)) { rowSuggestions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSuggestions.forEach { suggestion ->
                                SuggestionChip(
                                    text = suggestion.name,
                                    icon = suggestion.icon,
                                    onClick = {
                                        onSuggestionSelected(suggestion.name)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if odd number
                            if (rowSuggestions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1
            )
        }
    }
}

// =============================================
// Utility Functions and Data
// =============================================

fun getNotificationMethodDescription(method: NotificationMethod): String = when (method) {
    NotificationMethod.SMS_ONLY -> "Send text messages only"
    NotificationMethod.CALL_ONLY -> "Make phone calls only"
    NotificationMethod.SMS_AND_CALL -> "Both text messages and calls"
    NotificationMethod.EMAIL_ONLY -> "Send emails only"
    NotificationMethod.EMAIL_AND_SMS -> "Both emails and text messages"
    NotificationMethod.ALL_METHODS -> "All available methods"
    NotificationMethod.NONE -> "No automatic notifications"
}

fun getNotificationMethodIcon(method: NotificationMethod): ImageVector = when (method) {
    NotificationMethod.SMS_ONLY -> Icons.Default.Sms
    NotificationMethod.CALL_ONLY -> Icons.Default.Call
    NotificationMethod.SMS_AND_CALL -> Icons.Default.ContactPhone
    NotificationMethod.EMAIL_ONLY -> Icons.Default.Email
    NotificationMethod.EMAIL_AND_SMS -> Icons.Default.MailOutline
    NotificationMethod.ALL_METHODS -> Icons.Default.Notifications
    NotificationMethod.NONE -> Icons.Default.NotificationsOff
}

private data class RelationshipSuggestion(
    val name: String,
    val icon: ImageVector
)

private fun getRelationshipSuggestions(): List<RelationshipSuggestion> = listOf(
    RelationshipSuggestion("Sister", Icons.Default.Person),
    RelationshipSuggestion("Brother", Icons.Default.Person),
    RelationshipSuggestion("Mother", Icons.Default.Person),
    RelationshipSuggestion("Father", Icons.Default.Person),
    RelationshipSuggestion("Best Friend", Icons.Default.Favorite),
    RelationshipSuggestion("Partner", Icons.Default.Favorite),
    RelationshipSuggestion("Spouse", Icons.Default.Favorite),
    RelationshipSuggestion("Neighbor", Icons.Default.Home),
    RelationshipSuggestion("Colleague", Icons.Default.Work),
    RelationshipSuggestion("Counselor", Icons.Default.Psychology),
    RelationshipSuggestion("Therapist", Icons.Default.Psychology),
    RelationshipSuggestion("Legal Advocate", Icons.Default.Gavel),
    RelationshipSuggestion("Doctor", Icons.Default.LocalHospital),
    RelationshipSuggestion("Nurse", Icons.Default.LocalHospital),
    RelationshipSuggestion("Social Worker", Icons.Default.Work),
    RelationshipSuggestion("Crisis Counselor", Icons.Default.Support),
    RelationshipSuggestion("Religious Leader", Icons.Default.Group),
    RelationshipSuggestion("Teacher", Icons.Default.School),
    RelationshipSuggestion("Coach", Icons.Default.SportsBasketball),
    RelationshipSuggestion("Mentor", Icons.Default.Person),
    RelationshipSuggestion("Guardian", Icons.Default.Shield),
    RelationshipSuggestion("Grandparent", Icons.Default.Person),
    RelationshipSuggestion("Aunt", Icons.Default.Person),
    RelationshipSuggestion("Uncle", Icons.Default.Person),
    RelationshipSuggestion("Cousin", Icons.Default.Person),
    RelationshipSuggestion("Roommate", Icons.Default.Home),
    RelationshipSuggestion("Landlord", Icons.Default.Home),
    RelationshipSuggestion("Emergency Contact", Icons.Default.Emergency),
    RelationshipSuggestion("Other", Icons.Default.MoreHoriz)
)

// Helper function for contact type styling
private fun getContactTypeIcon(type: ContactType): ImageVector = when (type) {
    ContactType.TRUSTED_PERSON -> Icons.Default.Person
    ContactType.PROFESSIONAL -> Icons.Default.Work
    ContactType.EMERGENCY_SERVICE -> Icons.Default.Emergency
    ContactType.CRISIS_HOTLINE -> Icons.Default.Phone
    ContactType.LEGAL_ADVOCATE -> Icons.Default.Gavel
    ContactType.MEDICAL_PROVIDER -> Icons.Default.LocalHospital
    ContactType.COMMUNITY_RESOURCE -> Icons.Default.Group
    ContactType.OTHER -> Icons.Default.ContactPhone
}

private fun getContactTypeColor(type: ContactType): Color = when (type) {
    ContactType.TRUSTED_PERSON -> Color(0xFF4CAF50)
    ContactType.PROFESSIONAL -> Color(0xFF2196F3)
    ContactType.EMERGENCY_SERVICE -> Color(0xFFF44336)
    ContactType.CRISIS_HOTLINE -> Color(0xFF9C27B0)
    ContactType.LEGAL_ADVOCATE -> Color(0xFF607D8B)
    ContactType.MEDICAL_PROVIDER -> Color(0xFF00BCD4)
    ContactType.COMMUNITY_RESOURCE -> Color(0xFFFF9800)
    ContactType.OTHER -> Color(0xFF795548)
}

