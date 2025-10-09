package com.pragament.kotlin_images_explorer.presentation.screens

import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pragament.kotlin_images_explorer.presentation.viewmodel.ChatViewModel
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeEvent
import com.pragament.kotlin_images_explorer.presentation.viewmodel.UserRequest
import com.pragament.kotlin_images_explorer.ui.KotlinImagesExplorerIcons
import com.pragament.kotlin_images_explorer.ui.theme.primaryContainerDark
import com.pragament.kotlin_images_explorer.ui.theme.primaryContainerLight
import com.pragament.kotlin_images_explorer.ui.theme.primaryDark
import com.pragament.kotlin_images_explorer.ui.theme.primaryLight
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.annotations.Async
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = koinViewModel()) {
    val userChat = viewModel.userChat
    val modelChat = viewModel.modelChat

    var isUri by rememberSaveable {
        mutableStateOf(false)
    }
    var imageUri by remember {
        mutableStateOf("")
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
            isUri = true
             viewModel.processImage(listOf(uri.toString()))
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()  , topBar = {
            TopAppBar(
                title = { Text(text = "Chat") }
            )
        }
    ){ ip ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(top = ip.calculateTopPadding())) {
            LazyColumn(modifier = Modifier
                .fillMaxSize().align(Alignment.TopStart)
                .padding(6.dp).padding(bottom = 120.dp)) {
                itemsIndexed(userChat){ i , item ->
                    UserChatCard(item)
                    Spacer(modifier = Modifier.height(4.dp))
                    if ( i in viewModel.modelChat.indices){
                        ModelChatCard(modelChat[i])
                    }
                    else{
                        ModelChatCard(text = "Generating...")
                    }
                }
            }

                Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomEnd) , verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(value = viewModel.questionText, onValueChange = {
                        viewModel.questionText = it
                    }, shape = CircleShape, modifier = Modifier.fillMaxWidth().padding(6.dp), colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.White , focusedContainerColor = androidx.compose.ui.graphics.Color.White
                    ) , leadingIcon = {
                        Icon(
                            imageVector = if (isUri) KotlinImagesExplorerIcons.Image else KotlinImagesExplorerIcons.Add,
                            contentDescription = "Image" ,
                            modifier = Modifier.clickable{
                                photoPickerLauncher.launch("image/*")
                            }
                        )
                    } , trailingIcon = {
                        Icon(
                            imageVector = KotlinImagesExplorerIcons.Send,
                            contentDescription = "Send" , modifier = Modifier.clickable( enabled = viewModel.questionText.isNotEmpty() && isUri){
                                viewModel.updateUserChat(UserRequest(imageUri = imageUri , text = viewModel.questionText))
                                viewModel.setQuestion(viewModel.questionText)
                                viewModel.getAnswer(viewModel.questionText)
                                imageUri = ""
                                isUri = false
                                viewModel.questionText = ""
                            }
                        )
                    })
                }

        }
    }

}

@Composable
fun UserChatCard(userRequest: UserRequest) {
    Row(modifier = Modifier.fillMaxWidth() , horizontalArrangement = Arrangement.End) {
        Box(modifier = Modifier.fillMaxWidth(0.7f)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryContainerDark,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = userRequest.imageUri,
                        contentDescription = "image",
                        modifier = Modifier.size(120.dp) ,
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userRequest.text,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModelChatCard(text : String) {
    Row(modifier = Modifier.fillMaxWidth() , horizontalArrangement = Arrangement.Start) {
        Box(modifier = Modifier.fillMaxWidth(0.7f)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) androidx.compose.ui.graphics.Color.DarkGray else androidx.compose.ui.graphics.Color.White
                )
            ) {
                Text(text , modifier = Modifier.padding(4.dp))
            }
        }
    }
}