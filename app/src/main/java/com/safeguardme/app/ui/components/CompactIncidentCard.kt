// ui/components/CompactIncidentCard.kt
package com.safeguardme.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safeguardme.app.data.models.Incident
import com.safeguardme.app.data.models.IncidentType
import com.safeguardme.app.data.models.SeverityLevel
import com.safeguardme.app.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CompactIncidentCard(
    incident: Incident,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity indicator circle
            SeverityIndicator(severity = incident.severityLevel)

            // Main content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Date and type row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = remember(incident.date) {
                            DateUtils.getRelativeTimeString(incident.date.toDate())
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CompactIncidentTypeChip(type = incident.incidentType)
                }

                // Description
                Text(
                    text = remember(incident.description) {
                        incident.description.take(80).let { truncated ->
                            if (incident.description.length > 80) "$truncated..." else truncated
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status indicators row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location indicator
                    if (incident.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = incident.location.take(20),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Evidence indicators
                    if (incident.imageUrls.isNotEmpty()) {
                        CompactEvidenceChip(
                            icon = Icons.Default.Image,
                            count = incident.imageUrls.size
                        )
                    }

                    if (incident.audioUrl.isNotEmpty()) {
                        CompactEvidenceChip(
                            icon = Icons.Default.AudioFile,
                            count = 1
                        )
                    }

                    // Submission status
                    if (incident.submittedToSAPS || incident.submittedToNGO) {
                        CompactStatusChip(isSubmitted = true)
                    }
                }
            }

            // Chevron indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SeverityIndicator(severity: SeverityLevel) {
    val (color, _) = remember(severity) {
        when (severity) {
            SeverityLevel.LOW -> Color(0xFF4CAF50) to "Low"
            SeverityLevel.MEDIUM -> Color(0xFFFF9800) to "Medium"
            SeverityLevel.HIGH -> Color(0xFFF44336) to "High"
            SeverityLevel.CRITICAL -> Color(0xFF9C27B0) to "Critical"
        }
    }

    Surface(
        modifier = Modifier.size(8.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = color
    ) {}
}

@Composable
private fun CompactIncidentTypeChip(type: IncidentType) {
    val text = remember(type) {
        when (type) {
            IncidentType.PHYSICAL_VIOLENCE -> "Physical"
            IncidentType.EMOTIONAL_ABUSE -> "Emotional"
            IncidentType.SEXUAL_VIOLENCE -> "Sexual"
            IncidentType.ECONOMIC_ABUSE -> "Economic"
            IncidentType.STALKING -> "Stalking"
            IncidentType.HARASSMENT -> "Harassment"
            IncidentType.THREATS -> "Threats"
            IncidentType.OTHER -> "Other"
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CompactEvidenceChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            if (count > 1) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun CompactStatusChip(isSubmitted: Boolean) {
    val color = if (isSubmitted) Color(0xFF38A169) else Color(0xFFD69E2E)

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isSubmitted) Icons.Default.Check else Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = color
            )
            Text(
                text = if (isSubmitted) "✓" else "○",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.7f
                ),
                color = color
            )
        }
    }
}

// Alternative even more compact version for dense layouts
@Composable
fun MiniIncidentCard(
    incident: Incident,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity dot
            SeverityIndicator(severity = incident.severityLevel)

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = remember(incident.date) {
                            SimpleDateFormat("MMM dd", Locale.getDefault())
                                .format(incident.date.toDate())
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    CompactIncidentTypeChip(type = incident.incidentType)
                }

                Text(
                    text = remember(incident.description) {
                        incident.description.take(50).let { truncated ->
                            if (incident.description.length > 50) "$truncated..." else truncated
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Usage example extension function for the TabHomeScreen
@Composable
fun CompactIncidentCard(
    incident: Incident,
    onClick: () -> Unit = {}
) {
    CompactIncidentCard(
        incident = incident,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}