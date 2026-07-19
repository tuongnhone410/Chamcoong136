package com.example.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Helper extension function to safely await Google Task API in suspend coroutines
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTaskFirestore(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) {
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Unknown Firestore system error"))
            }
        }
    }
}

object FirestoreService {
    private const val TAG = "FirestoreService"

    private fun getDb(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase Firestore is not available in this configuration: ${e.message}")
            null
        }
    }

    private fun isDemoUser(uid: String): Boolean {
        return uid.startsWith("demo") || uid.contains("demo")
    }

    fun getUserConfigFlow(uid: String): Flow<UserConfig?> = callbackFlow {
        if (isDemoUser(uid)) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val firestore = getDb()
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = firestore.collection("users").document(uid).collection("config").document("salary_settings")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to UserConfig changes", error)
                trySend(null)
                close()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val config = snapshot.toUserConfig(uid)
                trySend(config)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveUserConfig(config: UserConfig) {
        if (isDemoUser(config.uid)) return
        val firestore = getDb() ?: return
        val map = mapOf(
            "fullName" to config.fullName,
            "hourlyRate" to config.hourlyRate,
            "currency" to config.currency,
            "dailyTargetHours" to config.dailyTargetHours,
            "he_so_ot_dem" to config.he_so_ot_dem,
            "thoi_gian_ca_dem" to config.thoi_gian_ca_dem,
            "luong_co_ban" to config.luong_co_ban,
            "luong_bao_hiem" to config.luong_bao_hiem,
            "tile_bao_hiem" to config.tile_bao_hiem,
            "doan_phi_40k" to config.doan_phi_40k,
            "ngay_chot_luong" to config.ngay_chot_luong,
            "he_so_ot_normal" to config.he_so_ot_normal,
            "he_so_ot_sunday" to config.he_so_ot_sunday,
            "he_so_ot_holiday" to config.he_so_ot_holiday,
            "tien_chuyen_can" to config.tien_chuyen_can,
            "so_ngay_nghi_phep" to config.so_ngay_nghi_phep,
            "phu_cap_ky_thuat" to config.phu_cap_ky_thuat,
            "phu_cap_trach_nhiem" to config.phu_cap_trach_nhiem,
            "phu_cap_chuc_vu" to config.phu_cap_chuc_vu,
            "phu_cap_hieu_suat" to config.phu_cap_hieu_suat,
            "phu_cap_san_pham" to config.phu_cap_san_pham,
            "phu_cap_com_ca" to config.phu_cap_com_ca,
            "phu_cap_com_ot" to config.phu_cap_com_ot,
            "phu_cap_tham_nien" to config.phu_cap_tham_nien,
            "phu_cap_nha_o" to config.phu_cap_nha_o,
            "phu_cap_doc_hai" to config.phu_cap_doc_hai,
            "phu_cap_dien_thoai" to config.phu_cap_dien_thoai,
            "phu_cap_xang_xe" to config.phu_cap_xang_xe,
            "phu_cap_khac" to config.phu_cap_khac,
            "tong_tien_com" to config.tong_tien_com,
            "phu_cap" to config.phu_cap,
            "thuong" to config.thuong,
            "tien_khau_tru_nghi" to config.tien_khau_tru_nghi,
            "last_accumulated_month" to config.last_accumulated_month
        )
        firestore.collection("users").document(config.uid)
            .collection("config").document("salary_settings")
            .set(map, SetOptions.merge())
            .awaitTaskFirestore()
    }

    // Convert date string "dd/MM/yyyy" -> "yyyy-MM-dd" for secure chronological ordering & document IDs
    fun formatDateForDocId(dateStr: String): String {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val dd = parts[0]
                val mm = parts[1]
                val yyyy = parts[2]
                "$yyyy-$mm-$dd"
            } else {
                dateStr.replace("/", "-")
            }
        } catch (e: Exception) {
            dateStr.replace("/", "-")
        }
    }

    fun getAttendanceLogsFlow(uid: String): Flow<List<AttendanceRecord>> = callbackFlow {
        if (isDemoUser(uid)) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val firestore = getDb()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = firestore.collection("users").document(uid).collection("attendance_logs")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to AttendanceLogs changes", error)
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toAttendanceRecord(uid)
                }.sortedByDescending { it.clockInTime }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecord) {
        if (isDemoUser(record.uid)) return
        val firestore = getDb() ?: return
        val docId = formatDateForDocId(record.dateString)
        val map = mapOf(
            "id" to record.id,
            "uid" to record.uid,
            "dateString" to record.dateString,
            "clockInTime" to record.clockInTime,
            "clockOutTime" to record.clockOutTime,
            "status" to record.status,
            "notes" to record.notes
        )
        firestore.collection("users").document(record.uid)
            .collection("attendance_logs").document(docId)
            .set(map, SetOptions.merge())
            .awaitTaskFirestore()
    }

    suspend fun deleteAttendanceRecord(uid: String, dateString: String) {
        if (isDemoUser(uid)) return
        val firestore = getDb() ?: return
        val docId = formatDateForDocId(dateString)
        firestore.collection("users").document(uid)
            .collection("attendance_logs").document(docId)
            .delete()
            .awaitTaskFirestore()
    }

    suspend fun getAllUserConfigs(): List<com.example.data.model.UserConfig> {
        val firestore = getDb() ?: return emptyList()
        return try {
            // Collection group query to find all 'settings' documents in any 'salary_config' subcollection
            val snapshot = firestore.collectionGroup("salary_config")
                .get()
                .awaitTaskFirestore()
            snapshot?.documents?.mapNotNull { doc ->
                val userId = doc.reference.parent.parent?.id ?: return@mapNotNull null
                doc.toUserSalaryConfig(userId)
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all user configs: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAttendanceLogsForUser(uid: String): List<AttendanceRecord> {
        val firestore = getDb() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(uid).collection("attendance_logs")
                .get()
                .awaitTaskFirestore()
            snapshot?.documents?.mapNotNull { doc ->
                doc.toAttendanceRecord(uid)
            }?.sortedByDescending { it.clockInTime } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance logs for $uid: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllAttendanceLogsInMonth(monthStr: String): List<AttendanceRecord> {
        val firestore = getDb() ?: return emptyList()
        return try {
            // monthStr format: yyyy-MM
            val snapshot = firestore.collectionGroup("attendance_logs")
                .whereGreaterThanOrEqualTo("dateString", "$monthStr-01")
                .whereLessThanOrEqualTo("dateString", "$monthStr-31")
                .get()
                .awaitTaskFirestore()
            snapshot?.documents?.mapNotNull { doc ->
                val uid = doc.reference.parent.parent?.id ?: return@mapNotNull null
                doc.toAttendanceRecord(uid)
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all attendance logs in month $monthStr: ${e.message}")
            emptyList()
        }
    }

    fun parseVersionToCode(versionStr: String): Long {
        val clean = versionStr.trim()
        if (clean.isEmpty()) return 0L
        
        // Remove any non-alphanumeric/dot characters just in case
        val filtered = clean.filter { it.isDigit() || it == '.' }
        val parts = filtered.split(".")
        if (parts.size == 1) {
            return filtered.toLongOrNull() ?: 0L
        }
        
        return try {
            val major = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val minor = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            val patch = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            major * 10000L + minor * 100L + patch
        } catch (e: java.lang.Exception) {
            0L
        }
    }

    fun isVersionNewer(codeA: Long, nameA: String, codeB: Long, nameB: String): Boolean {
        val cleanA = nameA.trim()
        val cleanB = nameB.trim()
        
        if (cleanA.isNotEmpty() && cleanB.isNotEmpty()) {
            val parsedA = parseVersionToCode(cleanA)
            val parsedB = parseVersionToCode(cleanB)
            if (parsedA > 0 && parsedB > 0) {
                return parsedA > parsedB
            }
        }
        
        return codeA > codeB
    }

    suspend fun checkAppVersion(context: android.content.Context): AppVersionControl? {
        val currentCode = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            2L // fallback current version code
        }

        val currentVersionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.4"
        } catch (e: Exception) {
            "1.4"
        }

        // Method 1: Check Firestore (Primary, configured directly in Admin Settings)
        try {
            val firestore = getDb()
            if (firestore != null) {
                val document = kotlinx.coroutines.withTimeoutOrNull(4000) {
                    firestore.collection("app_config").document("version_control")
                        .get().awaitTaskFirestore()
                }
                if (document != null && document.exists()) {
                    val dbCode = document.getLong("current_version_code") ?: 0L
                    val dbName = document.getString("current_version_name") ?: ""
                    val downloadUrl = document.getString("download_url") ?: ""
                    
                    val isNewAvailable = isVersionNewer(dbCode, dbName, currentCode, currentVersionName)
                    if (isNewAvailable && downloadUrl.isNotEmpty()) {
                        Log.d(TAG, "New version detected via Firestore (Primary): $dbName (Code $dbCode) > $currentVersionName")
                        return AppVersionControl(
                            latestVersionCode = dbCode,
                            downloadUrl = downloadUrl,
                            isNewVersionAvailable = true,
                            latestVersionName = dbName.ifEmpty { "v$dbCode" }
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Firestore check failed: ${e.message}")
        }

        // Method 2: Check Realtime Database (Backup)
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = database.getReference("app_version")
            val dataSnapshot = kotlinx.coroutines.withTimeoutOrNull(4000) {
                ref.get().awaitTaskFirestore()
            }
            if (dataSnapshot != null && dataSnapshot.exists()) {
                val dbCode = dataSnapshot.child("current_version_code").getValue(Long::class.java) ?: 0L
                val dbName = dataSnapshot.child("current_version_name").getValue(String::class.java) ?: ""
                val downloadUrl = dataSnapshot.child("download_url").getValue(String::class.java) ?: ""
                
                val isNewAvailable = isVersionNewer(dbCode, dbName, currentCode, currentVersionName)
                if (isNewAvailable && downloadUrl.isNotEmpty()) {
                    Log.d(TAG, "New version detected via Realtime DB: $dbName (Code $dbCode) > $currentVersionName")
                    return AppVersionControl(
                        latestVersionCode = dbCode,
                        downloadUrl = downloadUrl,
                        isNewVersionAvailable = true,
                        latestVersionName = dbName.ifEmpty { "v$dbCode" }
                    )
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Realtime Database check failed or not setup: ${e.message}")
        }

        // Method 3: Check Remote Config (Backup)
        try {
            val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val configSettings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // fetch check interval
                .build()
            
            val remoteFetched = kotlinx.coroutines.withTimeoutOrNull(4000) {
                remoteConfig.setConfigSettingsAsync(configSettings).awaitTaskFirestore()
                remoteConfig.fetchAndActivate().awaitTaskFirestore() ?: false
            } ?: false
            
            if (remoteFetched) {
                val remoteCode = remoteConfig.getLong("current_version_code")
                val remoteName = remoteConfig.getString("current_version_name")
                val downloadUrl = remoteConfig.getString("download_url")
                
                val isNewAvailable = isVersionNewer(remoteCode, remoteName, currentCode, currentVersionName)
                if (isNewAvailable && downloadUrl.isNotEmpty()) {
                    Log.d(TAG, "New version detected via Remote Config: $remoteName (Code $remoteCode) > $currentVersionName")
                    return AppVersionControl(
                        latestVersionCode = remoteCode,
                        downloadUrl = downloadUrl,
                        isNewVersionAvailable = true,
                        latestVersionName = remoteName.ifEmpty { "v$remoteCode" }
                    )
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Remote Config check failed or not setup: ${e.message}")
        }

        return null
    }

    suspend fun publishNewAppVersion(versionCode: Long, downloadUrl: String, versionName: String = ""): Boolean {
        var firestoreSuccess = false
        var rtDbSuccess = false

        // Update Firestore
        try {
            val firestore = getDb()
            if (firestore != null) {
                kotlinx.coroutines.withTimeoutOrNull(4000) {
                    firestore.collection("app_config").document("version_control")
                        .set(mapOf(
                            "current_version_code" to versionCode,
                            "current_version_name" to versionName,
                            "download_url" to downloadUrl
                        ))
                        .awaitTaskFirestore()
                }
                firestoreSuccess = true
                Log.d(TAG, "Successfully updated version in Firestore: $versionCode ($versionName)")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update version in Firestore: ${e.message}")
        }

        // Update Realtime Database
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = database.getReference("app_version")
            val updates = mapOf(
                "current_version_code" to versionCode,
                "current_version_name" to versionName,
                "download_url" to downloadUrl
            )
            val task = ref.updateChildren(updates)
            val dbSuccess = kotlinx.coroutines.withTimeoutOrNull(4000) {
                try {
                    task.awaitTaskFirestore()
                    true
                } catch (e: Exception) {
                    false
                }
            } ?: false
            rtDbSuccess = dbSuccess
            Log.d(TAG, "Successfully updated version in Realtime DB: $versionCode ($versionName)")
        } catch (e: Throwable) {
            Log.d(TAG, "Failed to update version in Realtime DB (maybe not configured): ${e.message}")
        }

        return firestoreSuccess || rtDbSuccess
    }

    suspend fun fetchPublishedVersion(): PublishedVersionResult {
        val errors = mutableListOf<String>()
        // 1. Try Firestore with robust timeout
        try {
            val firestore = getDb()
            if (firestore != null) {
                val doc = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    try {
                        firestore.collection("app_config").document("version_control")
                            .get().awaitTaskFirestore()
                    } catch (e: Exception) {
                        val exMessage = e.message ?: "Unknown Firestore error"
                        errors.add("Firestore: $exMessage")
                        null
                    }
                }
                if (doc != null && doc.exists()) {
                    val dbCode = doc.getLong("current_version_code") ?: 0L
                    val dbName = doc.getString("current_version_name") ?: ""
                    val dbUrl = doc.getString("download_url") ?: ""
                    return PublishedVersionResult(dbCode, dbName, dbUrl, true)
                } else if (doc != null && !doc.exists()) {
                    return PublishedVersionResult(0L, "Chưa có bản", "", true)
                }
            } else {
                errors.add("Firestore instance null")
            }
        } catch (e: Throwable) {
            errors.add("Firestore exception: ${e.message}")
            Log.e(TAG, "fetchPublishedVersion (Firestore) failed: ${e.message}")
        }

        // 2. Try Realtime Database with robust timeout
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = database.getReference("app_version")
            val snap = kotlinx.coroutines.withTimeoutOrNull(3000) {
                try {
                    ref.get().awaitTaskFirestore()
                } catch (e: Exception) {
                    val exMessage = e.message ?: "Unknown RTDB error"
                    errors.add("RTDB: $exMessage")
                    null
                }
            }
            if (snap != null && snap.exists()) {
                val dbCode = snap.child("current_version_code").getValue(Long::class.java) ?: 0L
                val dbName = snap.child("current_version_name").getValue(String::class.java) ?: ""
                val dbUrl = snap.child("download_url").getValue(String::class.java) ?: ""
                return PublishedVersionResult(dbCode, dbName, dbUrl, true)
            } else if (snap != null && !snap.exists()) {
                errors.add("RTDB: node empty")
            }
        } catch (e: Throwable) {
            errors.add("RTDB exception: ${e.message}")
            Log.e(TAG, "fetchPublishedVersion (RTDB) failed: ${e.message}")
        }

        val finalError = if (errors.isEmpty()) "Không thể tải thông tin" else errors.joinToString("; ")
        return PublishedVersionResult(0L, "Lỗi tải bản", "", false, finalError)
    }

    suspend fun saveUserSalaryConfigToFirestore(config: com.example.data.model.UserConfig) {
        if (config.userId.startsWith("demo") || config.userId.contains("demo")) return
        val firestore = getDb() ?: return
        val map = mapOf(
            "userId" to config.userId,
            "luongCoBan" to config.luongCoBan,
            "luongDongBaoHiem" to config.luongDongBaoHiem,
            "tiLeDongBaoHiem" to config.tiLeDongBaoHiem,
            "ngayChotLuong" to config.ngayChotLuong,
            "doanPhiCongDoan" to config.doanPhiCongDoan,
            "heSoOtNgayThuong" to config.heSoOtNgayThuong,
            "heSoOtChuNhat" to config.heSoOtChuNhat,
            "heSoOtNgayLe" to config.heSoOtNgayLe,
            "tienChuyenCanGoc" to config.tienChuyenCanGoc,
            "soNgayPhepNam" to config.soNgayPhepNam,
            "phepNamConLai" to config.phepNamConLai,
            "lastAccumulatedMonth" to config.lastAccumulatedMonth,
            "pcKyThuat" to config.pcKyThuat,
            "pcTrachNhiem" to config.pcTrachNhiem,
            "pcChucVu" to config.pcChucVu,
            "pcHieuSuat" to config.pcHieuSuat,
            "pcSanPham" to config.pcSanPham,
            "pcComCa" to config.pcComCa,
            "pcComOt" to config.pcComOt,
            "pcNhaO" to config.pcNhaO,
            "pcDocHai" to config.pcDocHai,
            "pcDtDoanhThu" to config.pcDtDoanhThu,
            "pcXangXe" to config.pcXangXe,
            "pcThamNien" to config.pcThamNien,
            "pcKhac1" to config.pcKhac1,
            "pcKhac" to config.pcKhac,
            "allowanceCalcTypes" to config.allowanceCalcTypes,
            "soGioNghiGiaiLao" to config.soGioNghiGiaiLao,
            "tinhKhauTruNghi" to config.tinhKhauTruNghi,
            "hoVaTen" to config.hoVaTen,
            "maNhanVien" to config.maNhanVien,
            "emailDangKy" to config.emailDangKy,
            "ngayVaoLam" to config.ngayVaoLam,
            "tienComMoiNgay" to config.tienComMoiNgay,
            "phuCap" to config.phuCap,
            "phuCapXangXe" to config.phuCapXangXe,
            "phuCapDienThoai" to config.phuCapDienThoai,
            "phuCapNhaO" to config.phuCapNhaO,
            "phuCapChuyenCan" to config.phuCapChuyenCan,
            "thuong" to config.thuong,
            "heSoOtDem" to config.heSoOtDem,
            "caDemStart" to config.caDemStart,
            "caDemEnd" to config.caDemEnd,
            "isAdmin" to config.isAdmin
        )
        firestore.collection("users").document(config.userId).collection("salary_config").document("settings")
            .set(map, SetOptions.merge())
            .awaitTaskFirestore()
    }

    suspend fun fetchUserSalaryConfigFromFirestore(userId: String): com.example.data.model.UserConfig? {
        if (userId.startsWith("demo") || userId.contains("demo")) return null
        val firestore = getDb() ?: return null
        return try {
            val document = firestore.collection("users").document(userId).collection("salary_config").document("settings")
                .get()
                .awaitTaskFirestore()
            if (document != null && document.exists()) {
                document.toUserSalaryConfig(userId)
            } else {
                // Fallback to old root collection
                val oldDocument = firestore.collection("users_salary").document(userId)
                    .get()
                    .awaitTaskFirestore()
                if (oldDocument != null && oldDocument.exists()) {
                    oldDocument.toUserSalaryConfig(userId)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user salary config from Firestore: ${e.message}", e)
            null
        }
    }

    suspend fun deleteUserFully(userId: String) {
        if (userId.startsWith("demo") || userId.contains("demo")) return
        val firestore = getDb() ?: return
        try {
            // 1. Delete salary_config subcollection documents
            val salaryConfigs = firestore.collection("users").document(userId).collection("salary_config").get().awaitTaskFirestore()
            salaryConfigs?.documents?.forEach { doc ->
                doc.reference.delete().awaitTaskFirestore()
            }
            
            // 2. Delete attendance_logs subcollection documents
            val logs = firestore.collection("users").document(userId).collection("attendance_logs").get().awaitTaskFirestore()
            logs?.documents?.forEach { doc ->
                doc.reference.delete().awaitTaskFirestore()
            }
            
            // 3. Delete the main user document
            firestore.collection("users").document(userId).delete().awaitTaskFirestore()
            
            // 4. Delete from old root collection if exists
            firestore.collection("users_salary").document(userId).delete().awaitTaskFirestore()
            
            Log.d(TAG, "Successfully deleted user fully: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user $userId: ${e.message}")
        }
    }
}

fun DocumentSnapshot.toUserSalaryConfig(userId: String): com.example.data.model.UserConfig {
    return com.example.data.model.UserConfig(
        userId = userId,
        luongCoBan = getDouble("luongCoBan") ?: 0.0,
        luongDongBaoHiem = getDouble("luongDongBaoHiem") ?: 0.0,
        tiLeDongBaoHiem = getDouble("tiLeDongBaoHiem") ?: 10.5,
        ngayChotLuong = getLong("ngayChotLuong")?.toInt() ?: 1,
        doanPhiCongDoan = getDouble("doanPhiCongDoan") ?: 0.0,
        heSoOtNgayThuong = getDouble("heSoOtNgayThuong") ?: 1.5,
        heSoOtChuNhat = getDouble("heSoOtChuNhat") ?: 2.0,
        heSoOtNgayLe = getDouble("heSoOtNgayLe") ?: 3.0,
        tienChuyenCanGoc = getDouble("tienChuyenCanGoc") ?: 0.0,
        soNgayPhepNam = getLong("soNgayPhepNam")?.toInt() ?: 0,
        phepNamConLai = getLong("phepNamConLai")?.toInt() ?: 0,
        lastAccumulatedMonth = getString("lastAccumulatedMonth") ?: "",
        pcKyThuat = getDouble("pcKyThuat") ?: 0.0,
        pcTrachNhiem = getDouble("pcTrachNhiem") ?: 0.0,
        pcChucVu = getDouble("pcChucVu") ?: 0.0,
        pcHieuSuat = getDouble("pcHieuSuat") ?: 0.0,
        pcSanPham = getDouble("pcSanPham") ?: 0.0,
        pcComCa = getDouble("pcComCa") ?: 0.0,
        pcComOt = getDouble("pcComOt") ?: 0.0,
        pcNhaO = getDouble("pcNhaO") ?: 0.0,
        pcDocHai = getDouble("pcDocHai") ?: 0.0,
        pcDtDoanhThu = getDouble("pcDtDoanhThu") ?: 0.0,
        pcXangXe = getDouble("pcXangXe") ?: 0.0,
        pcThamNien = getDouble("pcThamNien") ?: 0.0,
        pcKhac1 = getDouble("pcKhac1") ?: 0.0,
        pcKhac = getDouble("pcKhac") ?: 0.0,
        allowanceCalcTypes = getString("allowanceCalcTypes") ?: "",
        soGioNghiGiaiLao = getDouble("soGioNghiGiaiLao") ?: 1.5,
        tinhKhauTruNghi = getBoolean("tinhKhauTruNghi") ?: false,
        hoVaTen = getString("hoVaTen") ?: "User Demo",
        maNhanVien = getString("maNhanVien") ?: "demo_${userId.takeLast(6)}",
        emailDangKy = getString("emailDangKy") ?: "",
        ngayVaoLam = getString("ngayVaoLam") ?: "",
        tienComMoiNgay = getDouble("tienComMoiNgay") ?: 50000.0,
        phuCap = getDouble("phuCap") ?: 1000000.0,
        phuCapXangXe = getDouble("phuCapXangXe") ?: 500000.0,
        phuCapDienThoai = getDouble("phuCapDienThoai") ?: 300000.0,
        phuCapNhaO = getDouble("phuCapNhaO") ?: 1000000.0,
        phuCapChuyenCan = getDouble("phuCapChuyenCan") ?: 500000.0,
        thuong = getDouble("thuong") ?: 800000.0,
        heSoOtDem = getDouble("heSoOtDem") ?: 1.75,
        caDemStart = getString("caDemStart") ?: "22:00",
        caDemEnd = getString("caDemEnd") ?: "06:00",
        isAdmin = getBoolean("isAdmin") ?: false
    )
}

data class AppVersionControl(
    val latestVersionCode: Long,
    val downloadUrl: String,
    val isNewVersionAvailable: Boolean,
    val latestVersionName: String = ""
)

data class PublishedVersionResult(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val isSuccess: Boolean,
    val errorMessage: String = ""
)

fun DocumentSnapshot.toUserConfig(uid: String): UserConfig {
    return UserConfig(
        uid = uid,
        fullName = getString("fullName") ?: "Nhân viên mới",
        hourlyRate = getDouble("hourlyRate") ?: 50000.0,
        currency = getString("currency") ?: "đ",
        dailyTargetHours = getDouble("dailyTargetHours") ?: 8.0,
        he_so_ot_dem = getDouble("he_so_ot_dem") ?: 1.75,
        thoi_gian_ca_dem = getString("thoi_gian_ca_dem") ?: "22:00",
        
        luong_co_ban = getDouble("luong_co_ban") ?: 6000000.0,
        luong_bao_hiem = getDouble("luong_bao_hiem") ?: 5000000.0,
        tile_bao_hiem = getDouble("tile_bao_hiem") ?: 10.5,
        doan_phi_40k = getDouble("doan_phi_40k") ?: 40000.0,
        ngay_chot_luong = getLong("ngay_chot_luong")?.toInt() ?: 25,
        
        he_so_ot_normal = getDouble("he_so_ot_normal") ?: 1.5,
        he_so_ot_sunday = getDouble("he_so_ot_sunday") ?: 2.0,
        he_so_ot_holiday = getDouble("he_so_ot_holiday") ?: 3.0,
        
        tien_chuyen_can = getDouble("tien_chuyen_can") ?: 500000.0,
        so_ngay_nghi_phep = getLong("so_ngay_nghi_phep")?.toInt() ?: 12,
        
        phu_cap_ky_thuat = getDouble("phu_cap_ky_thuat") ?: 0.0,
        phu_cap_trach_nhiem = getDouble("phu_cap_trach_nhiem") ?: 0.0,
        phu_cap_chuc_vu = getDouble("phu_cap_chuc_vu") ?: 0.0,
        phu_cap_hieu_suat = getDouble("phu_cap_hieu_suat") ?: 0.0,
        phu_cap_san_pham = getDouble("phu_cap_san_pham") ?: 0.0,
        phu_cap_com_ca = getDouble("phu_cap_com_ca") ?: 30000.0,
        phu_cap_com_ot = getDouble("phu_cap_com_ot") ?: 15000.0,
        phu_cap_tham_nien = getDouble("phu_cap_tham_nien") ?: 0.0,
        phu_cap_nha_o = getDouble("phu_cap_nha_o") ?: 0.0,
        phu_cap_doc_hai = getDouble("phu_cap_doc_hai") ?: 0.0,
        phu_cap_dien_thoai = getDouble("phu_cap_dien_thoai") ?: 0.0,
        phu_cap_xang_xe = getDouble("phu_cap_xang_xe") ?: 0.0,
        phu_cap_khac = getDouble("phu_cap_khac") ?: 0.0,
        
        phu_cap = getDouble("phu_cap") ?: 500000.0,
        thuong = getDouble("thuong") ?: 200000.0,
        tien_khau_tru_nghi = getDouble("tien_khau_tru_nghi") ?: 0.0,
        tong_tien_com = getDouble("tong_tien_com") ?: 600000.0,
        last_accumulated_month = getLong("last_accumulated_month")?.toInt() ?: -1
    )
}

fun DocumentSnapshot.toAttendanceRecord(uid: String): AttendanceRecord {
    val idVal = getLong("id") ?: 0L
    return AttendanceRecord(
        id = idVal,
        uid = uid,
        dateString = getString("dateString") ?: "",
        clockInTime = getLong("clockInTime") ?: 0L,
        clockOutTime = getLong("clockOutTime"),
        status = getString("status") ?: "Active",
        notes = getString("notes") ?: ""
    )
}
