package dev.goquick.kmposable.sampleapp.contacts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.goquick.kmposable.sampleapp.contacts.EditContactEvent
import dev.goquick.kmposable.sampleapp.contacts.EditContactState

@Composable
fun EditContactScreen(
    state: EditContactState,
    onEvent: (EditContactEvent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = TextFieldValue(state.name),
            onValueChange = { onEvent(EditContactEvent.NameChanged(it.text)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = TextFieldValue(state.phone),
            onValueChange = { onEvent(EditContactEvent.PhoneChanged(it.text)) },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = TextFieldValue(state.email.orEmpty()),
            onValueChange = { onEvent(EditContactEvent.EmailChanged(it.text)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        if (state.error != null) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { onEvent(EditContactEvent.SaveClicked) }, enabled = !state.isSaving) {
            Text("Save")
        }
        Button(onClick = { onEvent(EditContactEvent.CancelClicked) }) {
            Text("Cancel")
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
