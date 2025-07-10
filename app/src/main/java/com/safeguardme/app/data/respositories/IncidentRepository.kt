// data/repositories/IncidentRepository.kt
package com.safeguardme.app.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.safeguardme.app.data.models.Incident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun getIncidentsCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("incidents")
    } ?: throw SecurityException("User not authenticated")

    // Report incident with evidence integrity
    suspend fun reportIncident(incident: Incident): Result<String> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val sanitizedIncident = incident.sanitized().copy(
            evidenceIntegrity = incident.generateEvidenceHash()
        )
        require(sanitizedIncident.isValid()) { "Invalid incident data" }

        val docRef = getIncidentsCollection().add(sanitizedIncident).await()

        // Security: Log incident creation for audit
        logIncidentAction(docRef.id, "CREATED")

        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to report incident: ${e.message}"))
    }

    // Get all incidents
    suspend fun getAllIncidents(): Result<List<Incident>> = try {
        val querySnapshot = getIncidentsCollection()
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()

        val incidents = querySnapshot.toObjects(Incident::class.java)
        Result.success(incidents)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to get incidents: ${e.message}"))
    }

    // Observe incidents in real-time
    fun observeIncidents(): Flow<List<Incident>> {
        return try {
            getIncidentsCollection()
                .orderBy("date", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(Incident::class.java) }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    // Update incident (with audit trail)
    suspend fun updateIncident(incidentId: String, incident: Incident): Result<Unit> = try {
        val sanitizedIncident = incident.sanitized().copy(
            evidenceIntegrity = incident.generateEvidenceHash()
        )
        require(sanitizedIncident.isValid()) { "Invalid incident data" }

        getIncidentsCollection().document(incidentId)
            .set(sanitizedIncident)
            .await()

        // Security: Log incident modification for audit
        logIncidentAction(incidentId, "UPDATED")

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to update incident: ${e.message}"))
    }

    // Get incident by ID
    suspend fun getIncident(incidentId: String): Result<Incident?> = try {
        val document = getIncidentsCollection().document(incidentId).get().await()
        val incident = document.toObject(Incident::class.java)
        Result.success(incident)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to get incident: ${e.message}"))
    }

    // Delete incident (emergency only)
    suspend fun deleteIncident(incidentId: String): Result<Unit> = try {
        getIncidentsCollection().document(incidentId).delete().await()

        // Security: Log incident deletion for audit
        logIncidentAction(incidentId, "DELETED")

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to delete incident: ${e.message}"))
    }

    // Get incidents by severity level
    suspend fun getIncidentsBySeverity(severity: com.safeguardme.app.data.models.SeverityLevel): Result<List<Incident>> = try {
        val querySnapshot = getIncidentsCollection()
            .whereEqualTo("severityLevel", severity)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()

        val incidents = querySnapshot.toObjects(Incident::class.java)
        Result.success(incidents)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to get incidents by severity: ${e.message}"))
    }

    // Security: Log incident actions for audit trail
    private suspend fun logIncidentAction(incidentId: String, action: String) {
        try {
            val auditLog = mapOf(
                "incidentId" to incidentId,
                "action" to action,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "userId" to (auth.currentUser?.uid ?: "unknown")
            )

            firestore.collection("audit_logs").add(auditLog).await()
        } catch (e: Exception) {
            // Audit logging failure should not fail the main operation
            // but should be logged for security review
        }
    }
}