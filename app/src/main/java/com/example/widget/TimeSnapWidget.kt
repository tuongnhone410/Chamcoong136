package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.auth.AuthController
import com.example.data.db.AppDatabase
import com.example.data.model.TimeEntry
import com.example.data.repository.TimeRepository
import com.example.data.repository.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import com.example.R

class TimeSnapWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val repository = TimeRepository(database.timeEntryDao(), database.userConfigDao())
        val authController = AuthController(context, repository)
        
        val session = authController.currentUserFlow.first()
        val activeEntry = session?.let { repository.getActiveEntry(it.uid) }
        
        provideContent {
            WidgetContent(context, session?.displayName ?: "Cá nhân", activeEntry)
        }
    }
    @Composable
    private fun WidgetContent(context: Context, name: String, activeEntry: TimeEntry?) {
        val isWorking = activeEntry != null
        val accentColor = if (isWorking) Color(0xFFFF5252) else Color(0xFF00E676)
        val neonBlue = Color(0xFF00B0FF)

        Box(modifier = GlanceModifier.fillMaxSize()) {
            Image(
                provider = ImageProvider(R.drawable.widget_bg_dark),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TimeSnap Pro",
                    style = TextStyle(
                        color = ColorProvider(neonBlue),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Text(
                    text = name,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledButton(
                        text = if (isWorking) "RA CA" else "VÀO CA",
                        onClick = actionRunCallback<ToggleActionCallback>(),
                        modifier = GlanceModifier.width(120.dp).height(48.dp)
                    )
                }
                
                if (isWorking && activeEntry?.checkInTime != null) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeStr = sdf.format(Date(activeEntry.checkInTime))
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Vào lúc: $timeStr",
                        style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 10.sp)
                    )
                } else {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dateStr = sdf.format(Date())
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = dateStr,
                        style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

class ToggleActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val database = AppDatabase.getInstance(context)
        val repository = TimeRepository(database.timeEntryDao(), database.userConfigDao())
        val authController = AuthController(context, repository)
        val cloudSyncManager = CloudSyncManager(context)
        
        val session = authController.currentUserFlow.first() ?: return
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val active = repository.getActiveEntry(session.uid)
        val config = repository.getConfigDirect(session.uid)

        if (active == null) {
            val existing = repository.getEntryByDate(session.uid, todayStr)
            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isSunday = (dayOfWeek == Calendar.SUNDAY)
            
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            
            val md = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date())
            val isHoliday = (md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02")

            val dayType = when {
                hour >= 18 -> "NIGHT"
                isHoliday -> "HOLIDAY"
                isSunday -> "SUNDAY"
                else -> "NORMAL"
            }

            val sId = if (hour >= 18) "ca_dem" else "ca1"
            val sType = if (sId == "ca_dem") "NIGHT" else "DAY"

            val newEntry = TimeEntry(
                id = existing?.id ?: 0,
                userId = session.uid,
                date = todayStr,
                checkInTime = System.currentTimeMillis(),
                checkOutTime = null,
                isWorking = true,
                dayType = dayType,
                shiftId = sId,
                shiftType = sType
            )
            val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(newEntry, config)
            repository.insertOrUpdate(calculated)
        } else {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            
            val sId = if (active.shiftId == "ca1" && hour >= 20) "ca2" else active.shiftId ?: "ca1"
            val sType = if (sId == "ca2") "DAY_REST" else active.shiftType ?: "DAY"

            val updated = active.copy(
                checkOutTime = System.currentTimeMillis(),
                isWorking = false,
                shiftId = sId,
                shiftType = sType
            )
            val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(updated, config)
            repository.insertOrUpdate(calculated)
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = repository.getEntries(session.uid).first()
                val conf = repository.getConfigDirect(session.uid) ?: com.example.data.model.UserConfig(userId = session.uid)
                cloudSyncManager.uploadToServer(session.uid, list, conf)
            } catch (e: Exception) {}
        }

        TimeSnapWidget().update(context, glanceId)
    }
}
