package com.heartyculture.app.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.heartyculture.app.api.fetchProductTemplates
import com.heartyculture.app.data.ProductTemplate
import com.heartyculture.app.database.AppDatabase
import com.heartyculture.app.database.ImageEntity
import com.heartyculture.app.viewModels.DetectorViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64

@Composable
fun ImageGallery(images: List<ImageEntity>, navController: NavController) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(images.size, itemContent = { imageEntity ->
            val bitmap = BitmapFactory.decodeByteArray(images[imageEntity].imageData, 0, images[imageEntity].imageData.size)
            ImageCard(
                bitmap = bitmap.asImageBitmap(),
                disease = let { images[imageEntity].disease ?: "Unknown" },
                plant = let { images[imageEntity].plant ?: "Unknown" },
                onClick = { onClickImageCard(navController = navController, plant = images[imageEntity].plant, disease = images[imageEntity].disease) }
            )
        })
    }
}

fun onClickImageCard(
    navController: NavController,
    plant: String,
    disease: String
) {
    navController.navigate("image_detail/$plant/$disease")
}

@Composable
fun ImageDetailScreen(
    plant: String,
    disease: String,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Plant: $plant",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProductTemplateList(plant)
    }
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
@Composable
fun Base64Image(base64String: String, contentDescription: String?) {
    val decodedBytes = remember(base64String) {
        Base64.decode(base64String, Base64.DEFAULT)
    }

    val bitmap = remember(decodedBytes) {
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    Image(
        painter = BitmapPainter(bitmap.asImageBitmap()),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .height(250.dp)
            .fillMaxWidth()
    )
}

@Composable
fun PlantImageSection(productTemplate: ProductTemplate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Base64Image(
            base64String = productTemplate.image_1920,
            contentDescription = productTemplate.name
        )
    }
}

@Composable
fun PlantInfoSection(productTemplate: ProductTemplate) {
    Text(
        text = productTemplate.name,
        style = MaterialTheme.typography.headlineMedium,
        color = Color.DarkGray
    )

//    Spacer(modifier = Modifier.height(8.dp))
//
//    Text(
//        text = productTemplate.description,
//        style = MaterialTheme.typography.bodyMedium
//    )

    Spacer(modifier =Modifier.height(16.dp))

    PlantDetailRow(label = "Price", value = productTemplate.list_price.toString())
    PlantDetailRow(label = "Qty Available", value = productTemplate.qty_available.toString())

    Spacer(modifier = Modifier.height(16.dp))

    // Display other important plant details
//    PlantDetailRow("Botanical Name", productTemplate.x_botanical_name)
    PlantDetailRow("Water Frequency", productTemplate.x_Waterfrequency)
    PlantDetailRow("Economic Value", productTemplate.x_EconomicValue)
    PlantDetailRow("Insects", productTemplate.x_Insect)
    PlantDetailRow("Diseases", productTemplate.x_Diseases)
    PlantDetailRow("Canopy Type", productTemplate.x_CanopyType)
    PlantDetailRow(label = "Flower", value =productTemplate.x_Flower)
    PlantDetailRow(label = "Fertilizer", value = productTemplate.x_ferriliser)
    PlantDetailRow(label = "Distribution", value = productTemplate.x_Distribution)
}

@Composable
fun PlantDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun PlantDetailsView(productTemplate: ProductTemplate) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .height(500.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PlantImageSection(productTemplate)
        Spacer(modifier = Modifier.height(16.dp))
        PlantInfoSection(productTemplate)
    }
}


@Composable
fun ProductTemplateList(plant: String) {
    val scope = rememberCoroutineScope()
    var productTemplates by remember { mutableStateOf<List<ProductTemplate>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context: Context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            val result = fetchProductTemplates(context, plant)
            if (result != null) {
                productTemplates = result
                isLoading = false
            } else {
                errorMessage = "Failed to fetch data"
                isLoading = false
            }
        }
    }

    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    } else if (errorMessage != null) {
        Text(text = errorMessage ?: "Unknown Error", color = MaterialTheme.colorScheme.error)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(productTemplates?.size ?: 0) { index ->
                productTemplates?.get(index)?.let { productTemplate ->
                    PlantDetailsView(productTemplate)
                }
            }
        }
    }
}

@Composable
fun ProductTemplateItem(productTemplate: ProductTemplate) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "ID: ${productTemplate.id}")
            Text(text = "Name: ${productTemplate.name}")
            // Display other fields as necessary
        }
    }
}

@Composable
fun ImageCard(bitmap: ImageBitmap, plant: String, disease: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(Color.White)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = plant,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = plant,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = disease,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun GalleryScreen(viewModel: DetectorViewModel, navController: NavController) {
    val scope = rememberCoroutineScope()
    val savedImages = remember { mutableStateOf(emptyList<ImageEntity>()) }
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val images = db.imageDao().getAllImages()
            withContext(Dispatchers.Main) {
                savedImages.value = images
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Saved Images",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            ImageGallery(images = savedImages.value, navController = navController)
        }
    }
}