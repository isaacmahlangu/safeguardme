// data/repositories/ContactRepository.kt
package com.safeguardme.app.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.safeguardme.app.data.models.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun getContactsCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("contacts")
    } ?: throw SecurityException("User not authenticated")

    // Add emergency contact with validation
    suspend fun addContact(contact: Contact): Result<String> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val sanitizedContact = contact.sanitized()
        require(sanitizedContact.isValid()) { "Invalid contact data" }

        // Security: Limit number of contacts
        val existingContacts = getContactsCollection().get().await()
        require(existingContacts.size() < 10) { "Maximum 10 contacts allowed" }

        val docRef = getContactsCollection().add(sanitizedContact).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to add contact: ${e.message}"))
    }

    // Get all contacts
    suspend fun getAllContacts(): Result<List<Contact>> = try {
        val querySnapshot = getContactsCollection()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val contacts = querySnapshot.toObjects(Contact::class.java)
        Result.success(contacts)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to get contacts: ${e.message}"))
    }

    // Observe contacts in real-time
    fun observeContacts(): Flow<List<Contact>> {
        return try {
            getContactsCollection()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(Contact::class.java) }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    // Update contact
    suspend fun updateContact(contactId: String, contact: Contact): Result<Unit> = try {
        val sanitizedContact = contact.sanitized()
        require(sanitizedContact.isValid()) { "Invalid contact data" }

        getContactsCollection().document(contactId)
            .set(sanitizedContact)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to update contact: ${e.message}"))
    }

    // Delete contact
    suspend fun deleteContact(contactId: String): Result<Unit> = try {
        getContactsCollection().document(contactId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to delete contact: ${e.message}"))
    }

    // Get primary emergency contacts
    suspend fun getPrimaryContacts(): Result<List<Contact>> = try {
        val querySnapshot = getContactsCollection()
            .whereEqualTo("isPrimary", true)
            .whereEqualTo("canReceiveEmergencyAlerts", true)
            .get()
            .await()

        val contacts = querySnapshot.toObjects(Contact::class.java)
        Result.success(contacts)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to get primary contacts: ${e.message}"))
    }
}

